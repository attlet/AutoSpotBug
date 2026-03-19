package com.autospotbug;

import com.autospotbug.build.BuildDetector;
import com.autospotbug.build.BuildTool;
import com.autospotbug.build.GradleRunner;
import com.autospotbug.build.MavenRunner;
import com.autospotbug.config.AppConfig;
import com.autospotbug.config.ConfigLoader;
import com.autospotbug.config.ProjectConfig;
import com.autospotbug.git.RepoManager;
import com.autospotbug.report.AnalysisResult;
import com.autospotbug.report.ReportCollector;
import com.autospotbug.report.ReportFile;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        AppConfig config = parseArgs(args);
        if (config == null) return; // --help 출력 후 정상 종료

        log.info("GitLab URL  : {}", config.getGitlabUrl());
        log.info("설정 파일   : {}", config.getConfigPath());
        log.info("출력 디렉토리: {}", config.getOutputDir());

        // 프로젝트 목록 로드
        List<ProjectConfig> projects = new ConfigLoader().load(config.getConfigPath());
        if (projects.isEmpty()) {
            log.error("분석할 프로젝트가 없습니다. config/projects.yaml을 확인하세요.");
            System.exit(1);
        }
        log.info("분석 대상: {}개 프로젝트", projects.size());

        // 출력 및 워크스페이스 디렉토리 생성
        Files.createDirectories(Path.of(config.getOutputDir()));
        Files.createDirectories(Path.of(config.getWorkspaceDir()));

        // 분석 실행
        RepoManager    repoManager = new RepoManager(config.getGitlabUrl(), config.getToken(), config.getWorkspaceDir());
        BuildDetector  detector    = new BuildDetector();
        GradleRunner   gradle      = new GradleRunner();
        MavenRunner    maven       = new MavenRunner();
        ReportCollector collector  = new ReportCollector(config.getOutputDir());

        List<AnalysisResult> results = new ArrayList<>();

        for (ProjectConfig project : projects) {
            log.info("=".repeat(50));
            log.info("[{}] 분석 시작", project.getName());

            try {
                // 1. clone / pull
                Path repoPath = repoManager.cloneOrPull(project);

                // 2. 빌드 도구 감지
                BuildTool tool = detector.detect(repoPath);
                log.info("[{}] 빌드 도구: {}", project.getName(), tool);

                // 3. SpotBugs 실행 및 XML 수집
                List<ReportFile> reportFiles;
                if (tool == BuildTool.GRADLE) {
                    gradle.run(project.getName(), repoPath);
                    reportFiles = gradle.collectReports(repoPath);
                } else {
                    maven.run(project.getName(), repoPath);
                    reportFiles = maven.collectReports(repoPath);
                }

                if (reportFiles.isEmpty()) {
                    throw new RuntimeException(
                            "SpotBugs XML 보고서를 찾을 수 없습니다. 컴파일 오류를 확인하세요."
                    );
                }

                // 4. output 디렉토리에 복사
                List<String> saved = collector.save(project.getName(), reportFiles);
                results.add(AnalysisResult.success(project.getName(), saved));

            } catch (Exception e) {
                log.error("[{}] 분석 실패: {}", project.getName(), e.getMessage());
                results.add(AnalysisResult.failure(project.getName(), e.getMessage()));
            }
        }

        collector.printSummary(results);

        long failCount = results.stream().filter(r -> !r.isSuccess()).count();
        System.exit(failCount > 0 ? 1 : 0);
    }

    private static AppConfig parseArgs(String[] args) throws ParseException {
        Options options = new Options();

        options.addOption(Option.builder()
                .longOpt("gitlab-url").hasArg().required()
                .desc("GitLab 서버 URL (예: https://gitlab.company.com)")
                .build());

        options.addOption(Option.builder()
                .longOpt("token").hasArg()
                .desc("GitLab Personal Access Token (미입력 시 환경변수 GITLAB_TOKEN 사용)")
                .build());

        options.addOption(Option.builder()
                .longOpt("config").hasArg()
                .desc("프로젝트 목록 YAML 파일 경로 (기본: config/projects.yaml)")
                .build());

        options.addOption(Option.builder()
                .longOpt("output").hasArg()
                .desc("XML 보고서 저장 디렉토리 (기본: output/)")
                .build());

        options.addOption(Option.builder()
                .longOpt("workspace").hasArg()
                .desc("Git clone 임시 디렉토리 (기본: workspace/)")
                .build());

        options.addOption("h", "help", false, "도움말 출력");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("java -jar autospotbug.jar", options, true);
                return null;
            }

            String token = cmd.getOptionValue("token", System.getenv("GITLAB_TOKEN"));
            if (token == null || token.isBlank()) {
                log.error("GitLab 토큰이 필요합니다. --token 또는 환경변수 GITLAB_TOKEN을 설정하세요.");
                System.exit(1);
            }

            return new AppConfig(
                    cmd.getOptionValue("gitlab-url"),
                    token,
                    cmd.getOptionValue("config", "config/projects.yaml"),
                    cmd.getOptionValue("output", "output"),
                    cmd.getOptionValue("workspace", "workspace")
            );

        } catch (MissingOptionException e) {
            log.error("필수 옵션 누락: {}", e.getMessage());
            formatter.printHelp("java -jar autospotbug.jar", options, true);
            System.exit(1);
            return null;
        }
    }
}

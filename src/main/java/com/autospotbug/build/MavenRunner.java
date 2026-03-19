package com.autospotbug.build;

import com.autospotbug.process.ProcessResult;
import com.autospotbug.process.ProcessRunner;
import com.autospotbug.report.ReportFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MavenRunner {

    private static final Logger log = LoggerFactory.getLogger(MavenRunner.class);

    // pom.xml 수정 없이 플러그인 좌표를 직접 지정하여 실행
    private static final String SPOTBUGS_GOAL =
            "com.github.spotbugs:spotbugs-maven-plugin:spotbugs";

    /**
     * Maven 프로젝트에 SpotBugs를 실행한다.
     * pom.xml을 수정하지 않고 플러그인 좌표를 CLI로 직접 지정한다.
     */
    public void run(String projectName, Path repoPath) throws IOException, InterruptedException {
        List<String> cmd = buildCommand(repoPath,
                SPOTBUGS_GOAL,
                "-DxmlOutput=true",
                "-Dspotbugs.effort=Max",
                "-Dspotbugs.threshold=Low",
                "-Dspotbugs.failOnError=false",
                "--batch-mode",
                "-DskipTests=true"
        );

        log.info("[{}] Maven SpotBugs 실행 중...", projectName);
        ProcessResult result = ProcessRunner.run(cmd, repoPath);

        if (!result.isSuccess()) {
            log.warn("[{}] Maven 종료 코드: {}", projectName, result.exitCode());
            log.debug("[{}] STDERR:\n{}", projectName, result.tailStderr(2000));
        }
    }

    /**
     * Maven 빌드 결과 XML 목록을 반환한다.
     * target/spotbugsXml.xml 패턴으로 수집 (멀티모듈 포함)
     */
    public List<ReportFile> collectReports(Path repoPath) throws IOException {
        List<ReportFile> found = new ArrayList<>();
        try (var walk = Files.walk(repoPath)) {
            walk.filter(p -> p.getFileName().toString().equals("spotbugsXml.xml")
                            && p.toString().replace("\\", "/").contains("/target/"))
                    .forEach(p -> found.add(new ReportFile(p, extractModuleName(repoPath, p))));
        }
        return found;
    }

    private String extractModuleName(Path repoPath, Path xmlPath) {
        Path rel = repoPath.relativize(xmlPath);
        // 경로 구조: [module/]target/spotbugsXml.xml
        if (rel.getNameCount() > 2 && "target".equals(rel.getName(1).toString())) {
            return rel.getName(0).toString();
        }
        return "";
    }

    private List<String> buildCommand(Path repoPath, String... goals) {
        List<String> cmd = new ArrayList<>();
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        if (isWindows) {
            Path wrapper = repoPath.resolve("mvnw.cmd");
            cmd.add(Files.exists(wrapper) ? wrapper.toAbsolutePath().toString() : "mvn");
        } else {
            Path wrapper = repoPath.resolve("mvnw");
            if (Files.exists(wrapper)) {
                wrapper.toFile().setExecutable(true);
                cmd.add(wrapper.toAbsolutePath().toString());
            } else {
                cmd.add("mvn");
            }
        }

        cmd.addAll(List.of(goals));
        return cmd;
    }
}

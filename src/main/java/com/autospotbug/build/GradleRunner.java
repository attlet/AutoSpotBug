package com.autospotbug.build;

import com.autospotbug.process.ProcessResult;
import com.autospotbug.process.ProcessRunner;
import com.autospotbug.report.ReportFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class GradleRunner {

    private static final Logger log = LoggerFactory.getLogger(GradleRunner.class);

    /**
     * Gradle 프로젝트에 SpotBugs를 실행한다.
     * --init-script 로 플러그인을 주입하므로 프로젝트 파일을 수정하지 않는다.
     */
    public void run(String projectName, Path repoPath) throws IOException, InterruptedException {
        Path initScript = resolveInitScript();
        List<String> cmd = buildCommand(repoPath,
                "spotbugsMain",
                "-x", "test",
                "--init-script", initScript.toAbsolutePath().toString(),
                "--continue",
                "--no-daemon"
        );

        log.info("[{}] Gradle SpotBugs 실행 중...", projectName);
        ProcessResult result = ProcessRunner.run(cmd, repoPath);

        if (!result.isSuccess()) {
            log.warn("[{}] Gradle 종료 코드: {} (ignoreFailures=true 이므로 계속)", projectName, result.exitCode());
            log.debug("[{}] STDERR:\n{}", projectName, result.tailStderr(2000));
        }
    }

    /**
     * Gradle 빌드 결과 XML 목록을 반환한다.
     * build/reports/spotbugs/*.xml 패턴으로 수집 (멀티모듈 포함)
     */
    public List<ReportFile> collectReports(Path repoPath) throws IOException {
        List<ReportFile> found = new ArrayList<>();
        try (var walk = Files.walk(repoPath)) {
            walk.filter(p -> p.toString().replace("\\", "/").contains("build/reports/spotbugs/")
                            && p.toString().endsWith(".xml"))
                    .forEach(p -> found.add(new ReportFile(p, extractModuleName(repoPath, p))));
        }
        return found;
    }

    private String extractModuleName(Path repoPath, Path xmlPath) {
        Path rel = repoPath.relativize(xmlPath);
        // 경로 구조: [module/]build/reports/spotbugs/main.xml
        if (rel.getNameCount() > 3 && "build".equals(rel.getName(1).toString())) {
            return rel.getName(0).toString();
        }
        return "";
    }

    private List<String> buildCommand(Path repoPath, String... tasks) {
        List<String> cmd = new ArrayList<>();
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        if (isWindows) {
            // Windows: .bat 파일은 ProcessBuilder에서 직접 실행 불가 → cmd /c 필요
            cmd.add("cmd");
            cmd.add("/c");
            Path wrapper = repoPath.resolve("gradlew.bat");
            cmd.add(Files.exists(wrapper) ? wrapper.toAbsolutePath().toString() : "gradle");
        } else {
            Path wrapper = repoPath.resolve("gradlew");
            if (Files.exists(wrapper)) {
                wrapper.toFile().setExecutable(true);
                cmd.add(wrapper.toAbsolutePath().toString());
            } else {
                cmd.add("gradle");
            }
        }

        cmd.addAll(List.of(tasks));
        return cmd;
    }

    /**
     * JAR 내부에 번들된 init script를 임시 파일로 추출한다.
     * workspace/resources/spotbugs-init.gradle이 있으면 그것을 우선 사용한다.
     */
    private Path resolveInitScript() throws IOException {
        Path localScript = Path.of("resources", "spotbugs-init.gradle");
        if (Files.exists(localScript)) {
            log.debug("로컬 init script 사용: {}", localScript);
            return localScript;
        }

        try (InputStream is = getClass().getResourceAsStream("/spotbugs-init.gradle")) {
            if (is == null) {
                throw new RuntimeException("spotbugs-init.gradle을 클래스패스에서 찾을 수 없습니다.");
            }
            Path temp = Files.createTempFile("spotbugs-init-", ".gradle");
            temp.toFile().deleteOnExit();
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            log.debug("init script 임시 파일 생성: {}", temp);
            return temp;
        }
    }
}

package com.autospotbug.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportCollector {

    private static final Logger log = LoggerFactory.getLogger(ReportCollector.class);

    private final Path outputDir;

    public ReportCollector(String outputDir) {
        this.outputDir = Path.of(outputDir);
    }

    /**
     * XML 보고서를 output 디렉토리에 복사하고 저장된 경로 목록을 반환한다.
     */
    public List<String> save(String projectName, List<ReportFile> reportFiles) throws Exception {
        List<String> saved = new ArrayList<>();

        for (ReportFile rf : reportFiles) {
            Path dest = outputDir.resolve(rf.toDestName(projectName));
            Files.copy(rf.source(), dest, StandardCopyOption.REPLACE_EXISTING);
            log.info("[{}] 보고서 저장: {}", projectName, dest);
            saved.add(dest.toString());
        }

        return saved;
    }

    /**
     * 전체 분석 결과 요약을 로그로 출력한다.
     */
    public void printSummary(List<AnalysisResult> results) {
        long successCount = results.stream().filter(AnalysisResult::isSuccess).count();
        long failCount    = results.size() - successCount;

        log.info("=".repeat(60));
        log.info("분석 완료: {}개 프로젝트 / 성공 {} / 실패 {}", results.size(), successCount, failCount);
        log.info("=".repeat(60));

        List<AnalysisResult> successes = results.stream().filter(AnalysisResult::isSuccess).toList();
        List<AnalysisResult> failures  = results.stream().filter(r -> !r.isSuccess()).toList();

        if (!successes.isEmpty()) {
            log.info("[성공]");
            for (AnalysisResult r : successes) {
                for (String report : r.getReports()) {
                    log.info("  ✔ {} → {}", r.getProjectName(), report);
                }
            }
        }

        if (!failures.isEmpty()) {
            log.info("[실패]");
            for (AnalysisResult r : failures) {
                log.error("  ✘ {}: {}", r.getProjectName(), r.getErrorMessage());
            }
        }

        log.info("보고서 저장 위치: {}", outputDir.toAbsolutePath());
        log.info("=".repeat(60));
    }

}

package com.autospotbug.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProcessRunner {

    private static final Logger log = LoggerFactory.getLogger(ProcessRunner.class);

    /**
     * 외부 프로세스를 실행하고 결과를 반환한다.
     * stdout/stderr를 병렬로 읽어 버퍼 데드락을 방지한다.
     */
    public static ProcessResult run(List<String> command, Path workingDir)
            throws IOException, InterruptedException {

        log.debug("실행: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());

        Process process = pb.start();

        // stdout, stderr 병렬 읽기 (버퍼 데드락 방지)
        CompletableFuture<String> stdoutFuture = readAsync(process.getInputStream());
        CompletableFuture<String> stderrFuture = readAsync(process.getErrorStream());

        int exitCode = process.waitFor();
        String stdout = stdoutFuture.join();
        String stderr = stderrFuture.join();

        return new ProcessResult(exitCode, stdout, stderr);
    }

    private static CompletableFuture<String> readAsync(InputStream is) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "";
            }
        });
    }
}

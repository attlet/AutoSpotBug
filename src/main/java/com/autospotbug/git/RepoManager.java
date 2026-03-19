package com.autospotbug.git;

import com.autospotbug.config.ProjectConfig;
import com.autospotbug.process.ProcessResult;
import com.autospotbug.process.ProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RepoManager {

    private static final Logger log = LoggerFactory.getLogger(RepoManager.class);

    private final String gitlabUrl;
    private final String token;
    private final Path workspaceDir;

    public RepoManager(String gitlabUrl, String token, String workspaceDir) {
        this.gitlabUrl = gitlabUrl;
        this.token = token;
        this.workspaceDir = Path.of(workspaceDir);
    }

    /**
     * 프로젝트를 clone하거나, 이미 존재하면 최신 상태로 pull한다.
     *
     * @return 로컬 저장소 경로
     */
    public Path cloneOrPull(ProjectConfig project) throws IOException, InterruptedException {
        Path repoDir = workspaceDir.resolve(project.getName());
        String branch = project.getBranch();

        if (Files.isDirectory(repoDir.resolve(".git"))) {
            log.info("[{}] 기존 저장소 업데이트 (branch: {})", project.getName(), branch);
            pull(project.getName(), repoDir, branch);
        } else {
            log.info("[{}] clone (branch: {}) → {}", project.getName(), branch, repoDir);
            clone(project, repoDir, branch);
        }

        return repoDir;
    }

    private void clone(ProjectConfig project, Path repoDir, String branch)
            throws IOException, InterruptedException {

        String cloneUrl = buildCloneUrl(project.getPath());
        ProcessResult result = ProcessRunner.run(
                List.of("git", "clone", "--branch", branch, "--single-branch", cloneUrl, repoDir.toString()),
                workspaceDir
        );

        if (!result.isSuccess()) {
            throw new RuntimeException("git clone 실패: " + maskToken(result.tailStderr(500)));
        }
    }

    private void pull(String name, Path repoDir, String branch)
            throws IOException, InterruptedException {

        run(name, repoDir, List.of("git", "fetch", "origin"));
        run(name, repoDir, List.of("git", "checkout", branch));
        run(name, repoDir, List.of("git", "reset", "--hard", "origin/" + branch));
    }

    private void run(String name, Path cwd, List<String> cmd)
            throws IOException, InterruptedException {

        ProcessResult result = ProcessRunner.run(cmd, cwd);
        if (!result.isSuccess()) {
            throw new RuntimeException(
                    String.format("[%s] git 오류 (%s): %s",
                            name, cmd.get(1), maskToken(result.tailStderr(500)))
            );
        }
    }

    private String buildCloneUrl(String projectPath) {
        String[] parts = gitlabUrl.split("://", 2);
        String protocol = parts.length == 2 ? parts[0] : "https";
        String host     = parts.length == 2 ? parts[1] : gitlabUrl;

        return String.format("%s://oauth2:%s@%s/%s.git", protocol, token, host, projectPath);
    }

    private String maskToken(String text) {
        if (token == null || token.isBlank()) return text;
        return text.replace(token, "***");
    }
}

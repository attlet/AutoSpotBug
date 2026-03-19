package com.autospotbug.build;

import java.nio.file.Files;
import java.nio.file.Path;

public class BuildDetector {

    /**
     * 프로젝트 루트를 분석하여 빌드 도구를 자동 감지한다.
     *
     * @param repoPath 로컬 저장소 경로
     * @return GRADLE 또는 MAVEN
     * @throws RuntimeException 빌드 도구를 감지할 수 없는 경우
     */
    /**
     * 빌드 도구를 자동 감지한다.
     * projects.yaml에 build_tool이 명시된 경우 이를 우선 사용한다.
     */
    public BuildTool detect(Path repoPath, String explicitBuildTool) {
        if (explicitBuildTool != null && !explicitBuildTool.isBlank()) {
            return BuildTool.valueOf(explicitBuildTool.trim().toUpperCase());
        }

        if (Files.exists(repoPath.resolve("build.gradle")) ||
                Files.exists(repoPath.resolve("build.gradle.kts"))) {
            return BuildTool.GRADLE;
        }
        if (Files.exists(repoPath.resolve("pom.xml"))) {
            return BuildTool.MAVEN;
        }
        if (Files.exists(repoPath.resolve("Makefile")) ||
                Files.exists(repoPath.resolve("makefile"))) {
            return BuildTool.MAKEFILE;
        }
        throw new RuntimeException(
                "빌드 도구를 감지할 수 없습니다. build.gradle(kts), pom.xml, Makefile 중 하나가 필요합니다: " + repoPath
        );
    }
}

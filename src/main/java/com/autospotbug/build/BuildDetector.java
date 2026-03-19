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
    public BuildTool detect(Path repoPath) {
        if (Files.exists(repoPath.resolve("build.gradle")) ||
                Files.exists(repoPath.resolve("build.gradle.kts"))) {
            return BuildTool.GRADLE;
        }
        if (Files.exists(repoPath.resolve("pom.xml"))) {
            return BuildTool.MAVEN;
        }
        throw new RuntimeException(
                "빌드 도구를 감지할 수 없습니다. build.gradle(kts) 또는 pom.xml이 필요합니다: " + repoPath
        );
    }
}

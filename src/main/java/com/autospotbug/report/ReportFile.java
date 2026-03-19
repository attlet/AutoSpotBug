package com.autospotbug.report;

import java.nio.file.Path;

/**
 * SpotBugs가 생성한 XML 파일과 해당 서브모듈 이름을 묶은 값 객체.
 *
 * @param source     XML 원본 경로
 * @param moduleName 서브모듈 이름 (단일 모듈 프로젝트이면 빈 문자열)
 */
public record ReportFile(Path source, String moduleName) {

    /** output 디렉토리에 저장될 파일명을 생성한다. */
    public String toDestName(String projectName) {
        if (moduleName == null || moduleName.isBlank()) {
            return projectName + "-spotbugs.xml";
        }
        return projectName + "-" + moduleName + "-spotbugs.xml";
    }
}

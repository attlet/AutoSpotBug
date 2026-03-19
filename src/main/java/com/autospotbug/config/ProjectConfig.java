package com.autospotbug.config;

public class ProjectConfig {

    private final String name;
    private final String path;
    private final String branch;

    // Makefile 프로젝트용 선택 필드
    private final String buildTool;   // 명시적 빌드 도구 지정 (gradle | maven | makefile)
    private final String makeTarget;  // make 타겟 (생략 시 기본 타겟)
    private final String classesDir;  // 컴파일된 클래스 디렉토리 (repoRoot 기준 상대경로)
    private final String jarPath;     // 빌드된 JAR 경로 (repoRoot 기준 상대경로)

    public ProjectConfig(String name, String path, String branch,
                         String buildTool, String makeTarget,
                         String classesDir, String jarPath) {
        this.name       = name;
        this.path       = path;
        this.branch     = branch != null ? branch : "develop";
        this.buildTool  = buildTool;
        this.makeTarget = makeTarget;
        this.classesDir = classesDir;
        this.jarPath    = jarPath;
    }

    public String getName()       { return name; }
    public String getPath()       { return path; }
    public String getBranch()     { return branch; }
    public String getBuildTool()  { return buildTool; }
    public String getMakeTarget() { return makeTarget; }
    public String getClassesDir() { return classesDir; }
    public String getJarPath()    { return jarPath; }

    @Override
    public String toString() {
        return String.format("ProjectConfig{name='%s', path='%s', branch='%s'}", name, path, branch);
    }
}

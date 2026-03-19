package com.autospotbug.config;

public class ProjectConfig {

    private final String name;
    private final String path;
    private final String branch;

    public ProjectConfig(String name, String path, String branch) {
        this.name = name;
        this.path = path;
        this.branch = branch != null ? branch : "develop";
    }

    public String getName()   { return name; }
    public String getPath()   { return path; }
    public String getBranch() { return branch; }

    @Override
    public String toString() {
        return String.format("ProjectConfig{name='%s', path='%s', branch='%s'}", name, path, branch);
    }
}

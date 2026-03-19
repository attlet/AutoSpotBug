package com.autospotbug.config;

public class AppConfig {

    private final String gitlabUrl;
    private final String token;
    private final String configPath;
    private final String outputDir;
    private final String workspaceDir;

    public AppConfig(String gitlabUrl, String token, String configPath,
                     String outputDir, String workspaceDir) {
        this.gitlabUrl = gitlabUrl.endsWith("/") ? gitlabUrl.substring(0, gitlabUrl.length() - 1) : gitlabUrl;
        this.token = token;
        this.configPath = configPath;
        this.outputDir = outputDir;
        this.workspaceDir = workspaceDir;
    }

    public String getGitlabUrl()    { return gitlabUrl; }
    public String getToken()        { return token; }
    public String getConfigPath()   { return configPath; }
    public String getOutputDir()    { return outputDir; }
    public String getWorkspaceDir() { return workspaceDir; }
}

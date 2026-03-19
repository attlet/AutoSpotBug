package com.autospotbug.report;

import java.util.List;

public class AnalysisResult {

    private final String projectName;
    private final boolean success;
    private final List<String> reports;
    private final String errorMessage;

    private AnalysisResult(String projectName, boolean success, List<String> reports, String errorMessage) {
        this.projectName = projectName;
        this.success = success;
        this.reports = reports;
        this.errorMessage = errorMessage;
    }

    public static AnalysisResult success(String projectName, List<String> reports) {
        return new AnalysisResult(projectName, true, reports, null);
    }

    public static AnalysisResult failure(String projectName, String errorMessage) {
        return new AnalysisResult(projectName, false, List.of(), errorMessage);
    }

    public String getProjectName() { return projectName; }
    public boolean isSuccess()     { return success; }
    public List<String> getReports() { return reports; }
    public String getErrorMessage() { return errorMessage; }
}

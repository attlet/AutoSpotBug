package com.autospotbug.process;

public record ProcessResult(int exitCode, String stdout, String stderr) {

    public boolean isSuccess() {
        return exitCode == 0;
    }

    public String tailStdout(int chars) {
        if (stdout.length() <= chars) return stdout;
        return stdout.substring(stdout.length() - chars);
    }

    public String tailStderr(int chars) {
        if (stderr.length() <= chars) return stderr;
        return stderr.substring(stderr.length() - chars);
    }
}

package com.github.gavro081.codeexecutionservice.models;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ExecutionResult{
    String stdout;
    String stdin;
    String stderr;
    int exitCode;

    public boolean isSuccess(){
        return exitCode == 0;
    }
}

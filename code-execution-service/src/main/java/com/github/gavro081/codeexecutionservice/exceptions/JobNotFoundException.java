package com.github.gavro081.codeexecutionservice.exceptions;

public class JobNotFoundException extends RuntimeException{
    public JobNotFoundException(String message) {
        super(message);
    }
}

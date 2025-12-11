package com.github.gavro081.apiserver.exceptions;


public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(String message) {
        super(message);
    }
}

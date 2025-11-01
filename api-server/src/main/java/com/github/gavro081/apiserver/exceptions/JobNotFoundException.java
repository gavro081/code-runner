package com.github.gavro081.apiserver.exceptions;

import jakarta.validation.constraints.NotNull;
import org.aspectj.bridge.IMessage;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(String message) {
        super(message);
    }
}

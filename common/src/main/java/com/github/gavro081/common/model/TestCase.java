package com.github.gavro081.common.model;

import java.io.Serializable;

// todo: change
public record TestCase(
        String input,
        String expectedOutput
) implements Serializable {}

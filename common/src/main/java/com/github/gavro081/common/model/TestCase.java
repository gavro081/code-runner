package com.github.gavro081.common.model;

import java.io.Serializable;

public record TestCase(
        String input,
        String expectedOutput
) implements Serializable {}

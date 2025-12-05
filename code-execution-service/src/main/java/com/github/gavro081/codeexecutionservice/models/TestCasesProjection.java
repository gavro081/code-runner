package com.github.gavro081.codeexecutionservice.models;

import com.github.gavro081.common.model.TestCase;

import java.util.List;

public interface TestCasesProjection {
    List<TestCase> getTestCases();
}

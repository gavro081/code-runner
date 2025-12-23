package com.github.gavro081.apiserver.repository;

import com.github.gavro081.common.model.Problem;

public interface ProblemCustomRepository {
    Problem findRandomExceptId(String id);
}

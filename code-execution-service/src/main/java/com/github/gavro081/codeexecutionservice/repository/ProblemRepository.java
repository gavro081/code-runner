package com.github.gavro081.codeexecutionservice.repository;

import com.github.gavro081.codeexecutionservice.models.TestCasesProjection;
import com.github.gavro081.common.model.Problem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemRepository extends MongoRepository<Problem, String> {
    @Query(value = "{ '_id': ?0 }", fields = "{ 'testCases': 1, '_id': 0, 'methodName': 2 }")
    TestCasesProjection findTestCasesAndMethodNameById(String id);
}

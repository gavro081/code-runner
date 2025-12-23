package com.github.gavro081.apiserver.repository.impl;

import com.github.gavro081.apiserver.repository.ProblemCustomRepository;
import com.github.gavro081.common.model.Problem;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class ProblemCustomRepositoryImpl implements ProblemCustomRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public Problem findRandomExceptId(String id) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("_id").ne(id)),
                Aggregation.sample(1)
        );
        return mongoTemplate
                .aggregate(aggregation,"problems", Problem.class)
                .getUniqueMappedResult();
    }
}

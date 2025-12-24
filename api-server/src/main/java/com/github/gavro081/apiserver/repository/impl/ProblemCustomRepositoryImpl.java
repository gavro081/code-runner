package com.github.gavro081.apiserver.repository.impl;

import com.github.gavro081.apiserver.dto.ProblemSummaryDto;
import com.github.gavro081.apiserver.repository.ProblemCustomRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.bson.Document;

@Repository
@AllArgsConstructor
public class ProblemCustomRepositoryImpl implements ProblemCustomRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public String findRandomExceptId(String id) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("_id").ne(id)),
                Aggregation.sample(1),
                Aggregation.project("_id")
        );

        AggregationResults<Document> results = mongoTemplate
                .aggregate(aggregation, "problems", Document.class);

        Document resultDoc = results.getUniqueMappedResult();
        return (resultDoc != null) ? resultDoc.getString("_id") : null;
    }

    public List<ProblemSummaryDto> findAllAsSummaryDto() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.project("title", "description", "difficulty")
        );
        return mongoTemplate
                .aggregate(aggregation, "problems", ProblemSummaryDto.class)
                .getMappedResults();  // used for lists
    }
}

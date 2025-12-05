package com.github.gavro081.codeexecutionservice.service;

import com.github.gavro081.codeexecutionservice.dto.ProblemTestDto;
import com.github.gavro081.common.model.ProgrammingLanguage;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class ProblemService {
    private final MongoTemplate mongoTemplate;

    public ProblemService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public ProblemTestDto getStarters(String id, ProgrammingLanguage lang) {
        // todo: explain
        MatchOperation match = match(Criteria.where("_id").is(id));
        ProjectionOperation project = project()
                .and("testCases").as("testCases")
                .and("starterTemplates." + lang.name()).as("template");

        Aggregation agg = newAggregation(match, project);

        return mongoTemplate.aggregate(agg, "problems", ProblemTestDto.class)
                .getUniqueMappedResult();
    }


}

package com.github.gavro081.common.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.persistence.Column;

@Document(collection = "problems")
@Getter @Setter
public class Problem implements Serializable {
    @Id
    @Column(name = "id")
    @JsonAlias("_id")
    private String id;

    private String difficulty;
    private String methodName;

    private String title;
    private String description;

    private List<TestCase> exampleTestCases;

    private List<TestCase> testCases;

    private List<String> assumptions;
    private List<String> constraints;

    private Map<ProgrammingLanguage, String> starterTemplates;

}

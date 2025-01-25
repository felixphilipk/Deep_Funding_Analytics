package com.deepfunding.dependency_graph_analyzer.model;

import lombok.Data;

@Data
public class TestObservation {
    private String id;
    private int projectAId;
    private int projectBId;
    private String projectAURL;
    private String projectBURL;

    public TestObservation(String id, String projectAURL, String projectBURL) {
        this.id = id;
        this.projectAURL = projectAURL;
        this.projectBURL = projectBURL;
    }
}
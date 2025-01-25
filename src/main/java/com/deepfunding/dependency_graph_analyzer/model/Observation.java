package com.deepfunding.dependency_graph_analyzer.model;

import lombok.Data;

@Data
public class Observation {
    private int projectAId;
    private int projectBId;
    private String projectAURL;
    private String projectBURL;
    private double weightA;
    private double weightB;
    private double observedProbA;

    public Observation(String projectAURL, String projectBURL, double weightA, double weightB) {
        this.projectAURL = projectAURL;
        this.projectBURL = projectBURL;
        this.weightA = weightA;
        this.weightB = weightB;
        this.observedProbA = weightA / (weightA + weightB);
    }
}
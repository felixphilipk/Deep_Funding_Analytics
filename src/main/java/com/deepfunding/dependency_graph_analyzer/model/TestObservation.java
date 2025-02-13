package com.deepfunding.dependency_graph_analyzer.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TestObservation {
    private String id;
    private int projectAId;
    private int projectBId;
    private String projectAURL;
    private String projectBURL;
    private double totalAmountUSD;
    private double normalizedFunding;
    private String funder;
    private String quarter;

    public TestObservation(String id, String projectAURL, String projectBURL,double totalAmountUSD, String funder, String quarter) {
        this.id = id;
        this.projectAURL = projectAURL;
        this.projectBURL = projectBURL;
        this.totalAmountUSD = totalAmountUSD;
        this.funder = funder;
        this.quarter = quarter;
        this.normalizedFunding= 0.0;
    }

    public double getNormalizedFunding() {
        return this.normalizedFunding;
    }
    public void setNormalizedFunding(double normalizedFunding){
        this.normalizedFunding = normalizedFunding;
    }
}
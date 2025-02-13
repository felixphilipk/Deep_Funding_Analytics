package com.deepfunding.dependency_graph_analyzer.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Observation {
    private int projectAId;
    private int projectBId;
    private String projectAURL;
    private String projectBURL;
    private double weightA;
    private double weightB;
    private double observedProbA;
    private double totalAmountUSD;
    private double normalizedFunding;
    private String funder;
    private String quarter;

    private RepoFeatures repoAFeatures;
    private RepoFeatures repoBFeatures;

    public Observation(String projectAURL, String projectBURL, double weightA, double weightB, double totalAmountUSD, String funder, String quarter) {
        this.projectAURL = projectAURL;
        this.projectBURL = projectBURL;
        this.weightA = weightA;
        this.weightB = weightB;
        this.observedProbA = weightA / (weightA + weightB);
        this.totalAmountUSD = totalAmountUSD;
        this.funder = funder;
        this.quarter = quarter;
        this.normalizedFunding= 0.0;
    }
    public double getNormalizedFunding() {
        return this.normalizedFunding;
    }

    public void setNormalizedFunding(double normalizeFunding){
        this.normalizedFunding = normalizeFunding;
    }

    /**
     * New robust normalization method
     * Instead of simple max scaling we use median and IQR from the funding distribution
     * to compute a scaled value and then squash it via a Logistic function
     * 
     * @param median the median of totalAmountUSD in the dataset
     * @param iqr the interquartile range of totalAmountUSD in the dataset
     */
    public void computeNormalizedFundingRobust(double median, double iqr){
      if(iqr >0){
        //Scale the funding amount relative to the median and IQR
        double scaled = (this.totalAmountUSD - median) / iqr;   
        // Logistic function to squash the scaled value between 0 and 1
        this.normalizedFunding = 1.0 / (1.0 + Math.exp(-scaled));
      }
      else{
        //If IQR is zero or negative fallback to 0.0
        this.normalizedFunding = 0.0;
      }
    }
}
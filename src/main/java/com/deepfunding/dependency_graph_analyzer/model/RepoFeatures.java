package com.deepfunding.dependency_graph_analyzer.model;

import lombok.Data;

@Data
public class RepoFeatures {
    private double size;
    private int stars;
    private int forks;
    private int watchers;
    private int openIssues;

    public RepoFeatures(double size, int stars, int forks, int watchers, int openIssues) {
        this.size = size;
        this.stars = stars;
        this.forks = forks;
        this.watchers = watchers;
        this.openIssues = openIssues;
    }
}
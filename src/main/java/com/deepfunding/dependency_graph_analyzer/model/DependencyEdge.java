package com.deepfunding.dependency_graph_analyzer.model;


import lombok.Data;

@Data
public class DependencyEdge {
    private String relation;
    private double weight;
    private String source;
    private String target;
}
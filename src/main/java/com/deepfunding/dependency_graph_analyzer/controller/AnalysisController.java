package com.deepfunding.dependency_graph_analyzer.controller;

// AnalysisController.java


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.deepfunding.dependency_graph_analyzer.model.DependencyEdge;
import com.deepfunding.dependency_graph_analyzer.model.Observation;
import com.deepfunding.dependency_graph_analyzer.model.TestObservation;
import com.deepfunding.dependency_graph_analyzer.service.DataIngestionService;
import com.deepfunding.dependency_graph_analyzer.service.GitHubDataService;
import com.deepfunding.dependency_graph_analyzer.service.ModelService;

import jakarta.annotation.Resource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final DataIngestionService dataIngestionService;
    private final GitHubDataService gitHubDataService;
    private final ModelService modelService;

    @Autowired
    public AnalysisController(DataIngestionService dataIngestionService, GitHubDataService gitHubDataService, ModelService modelService) {
        this.dataIngestionService = dataIngestionService;
        this.gitHubDataService = gitHubDataService;
        this.modelService = modelService;
    }

    @PostMapping(value = "/run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ByteArrayResource> runAnalysis(
        @RequestPart("trainingData") MultipartFile trainingDataFile,
        @RequestPart("testData") MultipartFile testDataFile,
        @RequestPart("dependencyGraph") MultipartFile dependencyGraphFile) {
    try {
        // Read data from uploaded files
        List<Observation> trainingObservations = dataIngestionService.readTrainingData(trainingDataFile);
        List<TestObservation> testObservations = dataIngestionService.readTestData(testDataFile);
        List<DependencyEdge> dependencyEdges = dataIngestionService.readDependencyGraphFromCSV(dependencyGraphFile);

        // Initialize model service
        modelService.initialize(trainingObservations, testObservations, dependencyEdges);

        // Optimize parameters
        modelService.optimizeParameters();

        // Calculate training MSE
        double mse = modelService.calculateTrainingMSE();
        System.out.println("Training MSE: " + mse);

        // Predict test data and get output as InputStreamResource
        ByteArrayResource outputResource = modelService.predictTestData();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"output.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(outputResource);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
    }
}
}
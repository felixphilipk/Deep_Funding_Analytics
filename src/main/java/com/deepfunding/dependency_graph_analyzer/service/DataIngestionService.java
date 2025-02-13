package com.deepfunding.dependency_graph_analyzer.service;

// DataIngestionService.java

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.deepfunding.dependency_graph_analyzer.model.DependencyEdge;
import com.deepfunding.dependency_graph_analyzer.model.Observation;
import com.deepfunding.dependency_graph_analyzer.model.TestObservation;
import org.apache.commons.io.input.BOMInputStream;
import java.nio.charset.StandardCharsets;

import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataIngestionService {

    // Method to read training data from CSV
    public List<Observation> readTrainingData(MultipartFile file) throws IOException {
        List<Observation> trainingObservations = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord csvRecord : csvParser) {
                try{
                String projectAURL = csvRecord.get("project_a");
                String projectBURL = csvRecord.get("project_b");
                double weightA = Double.parseDouble(csvRecord.get("weight_a"));
                double weightB = Double.parseDouble(csvRecord.get("weight_b"));
                double totalAmountUSD = Double.parseDouble(csvRecord.get("total_amount_usd"));
                String funder = csvRecord.get("funder");
                String quarter = csvRecord.get("quarter");
                trainingObservations.add(new Observation(projectAURL, projectBURL, weightA, weightB, totalAmountUSD, funder, quarter));
                }
                catch( IllegalArgumentException e){
                    System.err.println("Invalid data format in row: " + csvRecord.getRecordNumber() + " - " + e.getMessage());
                }
            }
        }
        return trainingObservations;
    }

    // Method to read test data from CSV
    public List<TestObservation> readTestData(MultipartFile file) throws IOException {
        List<TestObservation> testObservations = new ArrayList<>();
        try (BOMInputStream bomIn = new BOMInputStream(file.getInputStream());
            Reader reader = new InputStreamReader(bomIn, StandardCharsets.UTF_8);
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withCommentMarker('/')    // Lines starting with '/' are ignored.
            .withFirstRecordAsHeader()
            .withIgnoreHeaderCase()
            .withTrim())) {

                for (CSVRecord csvRecord : csvParser) {
                String id = csvRecord.get("id");
                String projectAURL = csvRecord.get("project_a");
                String projectBURL = csvRecord.get("project_b");
                double totalAmountUSD = Double.parseDouble(csvRecord.get("total_amount_usd"));
                String funder = csvRecord.get("funder");
                String quarter = csvRecord.get("quarter");

                testObservations.add(new TestObservation(id, projectAURL, projectBURL, totalAmountUSD, funder, quarter));
            }
        }
        return testObservations;
    }
 
 // Method to read dependency graph from CSV
    public List<DependencyEdge> readDependencyGraphFromCSV(MultipartFile file) throws IOException {
        List<DependencyEdge> dependencyEdges = new ArrayList<>();
        try(Reader reader = new InputStreamReader(file.getInputStream());
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader()
            .withSkipHeaderRecord(false)
            .withHeader("id", "seed_repo_owner", "seed_repo_name", "package_name", "package_repo_owner", "package_repo_name", "package_source")
            .withIgnoreHeaderCase()
            .withTrim())) {
            for (CSVRecord csvRecord : csvParser) {
                //Read the source repository 
              String seedRepoOwner = csvRecord.get("seed_repo_owner");
              String seedRepoName = csvRecord.get("seed_repo_name");
              String sourceRepoURL = "https://github.com/" + seedRepoOwner + "/" + seedRepoName;

              //Read the target repository
                String packageRepoOwner = csvRecord.get("package_repo_owner");
                String packageRepoName = csvRecord.get("package_repo_name");

                String targetRepoURL = null;
                if (packageRepoOwner != null && !packageRepoOwner.isEmpty()
                && packageRepoName != null && !packageRepoName.isEmpty()) {
                    targetRepoURL = "https://github.com/" + packageRepoOwner + "/" + packageRepoName;
            }
            else{
                // If repository owner or name is missing, skip this edge
                continue;
            }

            //Create Dependency Edge
            DependencyEdge edge = new DependencyEdge();
            edge.setSource(sourceRepoURL);
            edge.setTarget(targetRepoURL);
            edge.setWeight(0.0);
            edge.setRelation(csvRecord.get("package_source"));
            dependencyEdges.add(edge);

            }
        }
        catch(NumberFormatException e){
            System.err.println("Invalid weight fromat in CSV: " + e.getMessage());
        }
        return dependencyEdges;
    }

    
}
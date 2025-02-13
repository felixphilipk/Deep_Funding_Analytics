package com.deepfunding.dependency_graph_analyzer.service;

// GitHubDataService.java

import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.deepfunding.dependency_graph_analyzer.model.RepoFeatures;

import java.io.IOException;
import java.util.*;

@Service
public class GitHubDataService {

    private final GitHub github;
    private final Map<String, RepoFeatures> repoFeaturesCache = new HashMap<>();

    public GitHubDataService(@Value("${github.token}") String token) throws IOException {
        this.github = GitHub.connectUsingOAuth(token);
    }
// Method to fetch features for a repository
public RepoFeatures getRepositoryFeatures(String repoURL) {
    if (repoFeaturesCache.containsKey(repoURL)) {
        return repoFeaturesCache.get(repoURL);
    }
    try {
        String[] parts = repoURL.replace("https://github.com/", "").split("/");
        String owner = parts[0];
        String repoName = parts[1];

        GHRepository repository = github.getRepository(owner + "/" + repoName);
        RepoFeatures features = new RepoFeatures(
                repository.getSize(),
                repository.getStargazersCount(),
                repository.getForksCount(),
                repository.getWatchersCount(),
                repository.getOpenIssueCount()
        );
        repoFeaturesCache.put(repoURL, features);
        return features;
    } catch (IOException e) {
        // Handle exceptions
        System.err.println("Failed to fetch data for: " + repoURL);
        // Assign default values if data fetching fails
        RepoFeatures defaultFeatures = new RepoFeatures(0.0, 0, 0, 0, 0);
        repoFeaturesCache.put(repoURL, defaultFeatures);
        return defaultFeatures;
    }
}

// Method to compare ingested data with GitHub data
public void compareData(String repoURL, Map<String, Object> ingestedData) {
    RepoFeatures actualFeatures = getRepositoryFeatures(repoURL);
    if(ingestedData.containsKey("size")){
        double ingestedSize = ((Number) ingestedData.get("size")).doubleValue();
        if (Math.abs(ingestedSize - actualFeatures.getSize()) > 0.1) {
            System.err.println("Size discrepancy for " + repoURL + ": Ingested " + ingestedSize + " vs Actual " + actualFeatures.getSize());
        }
    }
    
    if (ingestedData.containsKey("stars")) {
        int ingestedStars = ((Number) ingestedData.get("stars")).intValue();
        if (Math.abs(ingestedStars - actualFeatures.getStars()) > 10) { // Define what's a significant difference
            System.err.println("Stars discrepancy for " + repoURL + ": Ingested " + ingestedStars + " vs Actual " + actualFeatures.getStars());
        }
    }
    
    if (ingestedData.containsKey("forks")) {
        int ingestedForks = ((Number) ingestedData.get("forks")).intValue();
        if (Math.abs(ingestedForks - actualFeatures.getForks()) > 5) {
            System.err.println("Forks discrepancy for " + repoURL + ": Ingested " + ingestedForks + " vs Actual " + actualFeatures.getForks());
        }
    }
    
    if (ingestedData.containsKey("watchers")) {
        int ingestedWatchers = ((Number) ingestedData.get("watchers")).intValue();
        if (Math.abs(ingestedWatchers - actualFeatures.getWatchers()) > 5) {
            System.err.println("Watchers discrepancy for " + repoURL + ": Ingested " + ingestedWatchers + " vs Actual " + actualFeatures.getWatchers());
        }
    }
    
    if (ingestedData.containsKey("openIssues")) {
        int ingestedOpenIssues = ((Number) ingestedData.get("openIssues")).intValue();
        if (Math.abs(ingestedOpenIssues - actualFeatures.getOpenIssues()) > 5) {
            System.err.println("Open Issues discrepancy for " + repoURL + ": Ingested " + ingestedOpenIssues + " vs Actual " + actualFeatures.getOpenIssues());
        }
    }
}

}
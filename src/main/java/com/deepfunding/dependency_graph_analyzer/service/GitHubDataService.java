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
    // Compare actualFeatures with ingestedData
    // Implement your comparison logic here
}
}
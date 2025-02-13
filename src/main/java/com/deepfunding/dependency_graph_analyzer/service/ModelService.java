package com.deepfunding.dependency_graph_analyzer.service;

// ModelService.java


import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.*;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import com.deepfunding.dependency_graph_analyzer.model.Observation;
import com.deepfunding.dependency_graph_analyzer.model.TestObservation;
import com.deepfunding.dependency_graph_analyzer.model.DependencyEdge;
import com.deepfunding.dependency_graph_analyzer.model.RepoFeatures;
import com.deepfunding.dependency_graph_analyzer.service.GitHubDataService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

@Service
public class ModelService {
    // Logger instance
    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);

    private final GitHubDataService gitHubDataService;

    @Autowired
    public ModelService(GitHubDataService gitHubDataService) {
        this.gitHubDataService = gitHubDataService;
    }

    private Map<String, Integer> repoToId = new HashMap<>();
    private List<String> idToRepo = new ArrayList<>();

    private List<Observation> trainingObservations;
    private List<TestObservation> testObservations;
    private List<DependencyEdge> dependencyEdges;

    private int numRepos;

    private double[] beta;
    private double[] theta;

      // Repository features map (if needed)
      private Map<Integer, RepoFeatures> repoFeaturesMap = new HashMap<>();

    // Initialize the service with observations
    public void initialize(List<Observation> trainingObservations, List<TestObservation> testObservations,List<DependencyEdge> dependencyEdges) {
        this.trainingObservations = trainingObservations;
        this.testObservations = testObservations;
        this.dependencyEdges = dependencyEdges;

        assignRepositoryIds();
        numRepos = idToRepo.size();
        initializeParameters();
        loadRepositoryFeatures();
        double medianFunding = 50000.0; // Median funding amount
        double iqrFunding = 30000.0; // Interquartile range of funding amount
        normalizeFunding(medianFunding,iqrFunding);
    }

    // Assign integer IDs to repositories
    private void assignRepositoryIds() {
        for (Observation obs : trainingObservations) {
            int projectAId = getRepositoryId(obs.getProjectAURL());
            int projectBId = getRepositoryId(obs.getProjectBURL());
            obs.setProjectAId(projectAId);
            obs.setProjectBId(projectBId);
        }
        for (TestObservation obs : testObservations) {
            int projectAId = getRepositoryId(obs.getProjectAURL());
            int projectBId = getRepositoryId(obs.getProjectBURL());
            obs.setProjectAId(projectAId);
            obs.setProjectBId(projectBId);
        }
         // From dependency graph
         for (DependencyEdge edge : dependencyEdges) {
            getRepositoryId(edge.getSource());
            getRepositoryId(edge.getTarget());
        }
    }

    private int getRepositoryId(String repoURL) {
      return repoToId.computeIfAbsent(repoURL, url->{
        int newId = idToRepo.size();
        idToRepo.add(url);
        return newId;
      });
    }

    // Initialize theta parameters
    public void initializeParameters() {
        theta = new double[numRepos];
        for (int i = 0; i < numRepos; i++) {
            theta[i] = 0.0;
        }
    }

    // Normalize funding amounts
    private void normalizeFunding(double median , double iqr){ 
        for(Observation obs : trainingObservations){
        obs.computeNormalizedFundingRobust(median,iqr);
        }
       for(TestObservation obs : testObservations){
        if (iqr>0){
            double scaled = (obs.getTotalAmountUSD()- median)/iqr;
            obs.setNormalizedFunding(1.0/(1.0 + Math.exp(-scaled)));
        }
        else{
            obs.setNormalizedFunding(0.0);
        }
       }
    }
    private double calculateInitialBeta(int repoId) {
    RepoFeatures features = repoFeaturesMap.get(repoId);
    double baseWeight = 1.0;
    double totalNormalizedFunding =0.0;
    for(Observation obs : trainingObservations){
        if(obs.getProjectAId() == repoId){
            baseWeight += obs.getWeightA();
            totalNormalizedFunding += obs.getNormalizedFunding();
        }
        else if (obs.getProjectBId() == repoId){
            baseWeight += obs.getWeightB();
            totalNormalizedFunding += obs.getNormalizedFunding();
        }
    }

        return baseWeight * (1 + (features != null ? (features.getStars() * 0.001 + totalNormalizedFunding * 0.1) : 0));
    }

    // Load repository features 
    private void loadRepositoryFeatures() {
       for(int i=0; i<numRepos; i++){
           String repoURL = idToRepo.get(i);
            try{
                repoFeaturesMap.put(i,gitHubDataService.getRepositoryFeatures(repoURL));
            }
            catch(Exception e){
                logger.error("Failed to load features for repository: {}", repoURL, e);
                repoFeaturesMap.put(i, new RepoFeatures(0.0, 0, 0, 0, 0));
            }
       }
    }

    // Mean Square Error Function
    private class MSEFunction implements MultivariateFunction {
        @Override
        public double value(double[] thetas) {
            double mse = 0.0;
            // Loop over all training observations.
            for (Observation obs : trainingObservations) {
                int idA = obs.getProjectAId();
                int idB = obs.getProjectBId();
                double thetaA = thetas[idA];
                double thetaB = thetas[idB];
                
                // Adjusted multipliers: Increase repo effect and funding effect contribution.
                double repoEffectA = repoFeaturesMap.containsKey(idA)
                        ? repoFeaturesMap.get(idA).getStars() * 0.005  // increased from 0.001 to 0.005
                        : 0.0;
                double repoEffectB = repoFeaturesMap.containsKey(idB)
                        ? repoFeaturesMap.get(idB).getStars() * 0.005
                        : 0.0;
                
                // Increase funding multiplier from 0.1 to 0.3.
                double fundingEffectA = obs.getNormalizedFunding() * 0.3;
                
                // Compute effective strengths.
                double effectiveA = thetaA + repoEffectA + fundingEffectA;
                double effectiveB = thetaB + repoEffectB;
                
                // Use logistic function to predict probability.
                double prediction = 1.0 / (1.0 + Math.exp(-(effectiveA - effectiveB)));
                
                // Use the observed probability as target (assumed provided by the observation)
                double error = prediction - obs.getObservedProbA();
                mse += error * error;
            }
            mse /= trainingObservations.size();
            
            // L2 regularization to prevent overfitting.
            double l2norm = 0.0;
            for (double param : thetas) {
                l2norm += param * param;
            }
            double lambdaRegularization = 0.01;
            mse += lambdaRegularization * l2norm;
            return mse;
        }
    }

    // Optimize parameters using CMA-ES optimizer
    public void optimizeParameters() {
        logger.info("Starting optimization...");
        // Tolerance for convergence
        int maxIterations = 10000;
        int populationSize =200;
        double sigma = 0.5;

        // Create RandomGenerator instance
        RandomGenerator randomGenerator = new MersenneTwister(); // Seed can be set as needed

        // Create optimizer with custom convergence criteria
        CMAESOptimizer optimizer = new CMAESOptimizer(
            maxIterations,                   // maxIterations
                1e-9,      // stopFitness (no fitness stopping criterion)
                true,                   // isActiveCMA
                0,                      // diagonalOnly
                0,                      // checkFeasibleCount
                randomGenerator,        // random
                false,                  // generateStatistics
                null
        );
 // Initial guess is the current theta vector.
 double[] initialGuess = theta;
 // Set lower and upper bounds. Here parameters are assumed bounded by [-1000, 1000].
 double[] lowerBound = new double[theta.length];
 double[] upperBound = new double[theta.length];
 Arrays.fill(lowerBound, -1000);
 Arrays.fill(upperBound, 1000);

 // Wrap our objective function which computes (MSE + L2 regularization).
 MSEFunction mseFunction = new MSEFunction();
 try {
     PointValuePair result = optimizer.optimize(
             new MaxEval(10000), // Increased number of evaluations.
             new ObjectiveFunction(mseFunction),
             GoalType.MINIMIZE,
             new InitialGuess(initialGuess),
             new CMAESOptimizer.Sigma(Arrays.stream(initialGuess).map(x -> sigma).toArray()),
             new SimpleBounds(lowerBound, upperBound)
     );
     // Update theta with the optimized parameters.
     theta = result.getPoint();
     logger.info("Optimization complete. Optimized theta parameters updated.");
 } catch (Exception e) {
     logger.error("Optimization failed", e);
 }
        
    }

    // Predict test data
    // In ModelService.java
    public ByteArrayResource predictTestData() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            writer.write("id,pred\n");
            // For each test observation, predict the probability using theta parameters
            for (TestObservation obs : testObservations) {
                int projectAId = obs.getProjectAId();
                int projectBId = obs.getProjectBId();
                double thetaA = theta[projectAId];
                double thetaB = theta[projectBId];
            // Incorporate repository features. Here we use the 'stars' attribute as an example.
            double repoEffectA = repoFeaturesMap.containsKey(projectAId)
                    ? repoFeaturesMap.get(projectAId).getStars() * 0.001
                    : 0.0;
            double repoEffectB = repoFeaturesMap.containsKey(projectBId)
                    ? repoFeaturesMap.get(projectBId).getStars() * 0.001
                    : 0.0;
            
            // Incorporate funding information. For instance, adding a funding effect for project A.
            double fundingEffectA = obs.getNormalizedFunding() * 0.1;
            
            // Compute an effective strength by adding the base theta, repository and funding effects.
            double effectA = thetaA + repoEffectA + fundingEffectA;
            double effectB = thetaB + repoEffectB; // assuming project B gets no additional funding effect
            
            double diff = effectA - effectB;
            // Clip the diff to avoid overflow in exponential function.
            if (diff > 700) {
                diff = 700;
            } else if (diff < -700) {
                diff = -700;
            }
            double pred = 1.0 / (1.0 + Math.exp(-diff));
            
            String obsId = obs.getId();
            if (obsId == null || obsId.trim().isEmpty()) {
                obsId = String.format("%d_%d", projectAId, projectBId);
            }
            writer.write(String.format("%s,%.11f\n", obsId, pred));
        }
    }
    return new ByteArrayResource(outputStream.toByteArray());
    }

    // Calculate training MSE
    public double calculateTrainingMSE() {
        double mse = 0.0;
        for (Observation obs : trainingObservations) {
            double thetaA = theta[obs.getProjectAId()];
            double thetaB = theta[obs.getProjectBId()];
            double deltaTheta = thetaA - thetaB;
            double probA = 1.0 / (1.0 + Math.exp(-deltaTheta));
            double error = probA - obs.getObservedProbA();
            mse += error * error;
        }
        mse /= trainingObservations.size();
        return mse;
    }
}
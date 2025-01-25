package com.deepfunding.dependency_graph_analyzer.service;

// ModelService.java


import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.*;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import com.deepfunding.dependency_graph_analyzer.model.Observation;
import com.deepfunding.dependency_graph_analyzer.model.TestObservation;
import com.deepfunding.dependency_graph_analyzer.model.DependencyEdge;
import com.deepfunding.dependency_graph_analyzer.model.RepoFeatures;
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
        if (!repoToId.containsKey(repoURL)) {
            int id = idToRepo.size();
            repoToId.put(repoURL, id);
            idToRepo.add(repoURL);
        }
        return repoToId.get(repoURL);
    }

    // Initialize theta parameters
    public void initializeParameters() {
        theta = new double[numRepos];
        for (int i = 0; i < numRepos; i++) {
            theta[i] = Math.log(Math.max(calculateInitialBeta(i), 1e-6));
        }
    }

    private double calculateInitialBeta(int repoId) {
        String repoURL = idToRepo.get(repoId);
        double totalWeight = 0.0;
        int count = 0;
        for (Observation obs : trainingObservations) {
            if (obs.getProjectAId() == repoId) {
                totalWeight += obs.getWeightA();
                count++;
            } else if (obs.getProjectBId() == repoId) {
                totalWeight += obs.getWeightB();
                count++;
            }
        }
        return (count > 0) ? totalWeight / count : 1.0;
    }

    // Mean Square Error Function
    private class MSEFunction implements MultivariateFunction {
        private final double lambda = 1e-6; // Regularization parameter

        // Add a counter for function evaluations
        private int evaluationCount = 0;
        private final int logInterval = 10; // Adjust the interval as needed
        private final int maxEvaluations; // Match this to your MaxEval value

        public MSEFunction(int maxEvaluations) {
            this.maxEvaluations = maxEvaluations;
            logger.info("MSEFunction initialized with maxEvaluations = {}", maxEvaluations);
        }

        @Override
        public double value(double[] thetas) {
            logger.debug("MSEFunction.value() called");
            double mse = 0.0;
            try{
            for (Observation obs : trainingObservations) {
                Integer projectAId = obs.getProjectAId();
                Integer projectBId = obs.getProjectBId();

                if(projectAId==null || projectBId == null||
                projectAId<0 || projectAId>=thetas.length ||
                projectBId<0 || projectBId>=thetas.length){
                    logger.error("Invalid project ID's in observation: {}" + obs);
                    continue;
                }
                
                double thetaA = thetas[projectAId];
                double thetaB = thetas[projectBId];

                double deltaTheta = thetaA - thetaB;
                double probA = 1.0 / (1.0 + Math.exp(-deltaTheta));

                // Ensure probA is within (1e-10, 1 - 1e-10)
                probA = Math.min(Math.max(probA, 1e-10), 1 - 1e-10);
                double error = probA - obs.getObservedProbA();
                mse += error * error;
            }

            
            // Incorporate dependency graph into mse
            for (DependencyEdge edge : dependencyEdges) {
                Integer sourceId = repoToId.get(edge.getSource());
                Integer targetId = repoToId.get(edge.getTarget());
                
                if (sourceId == null || targetId == null ||
                sourceId < 0 || sourceId >= thetas.length ||
                targetId < 0 || targetId >= thetas.length) {
                logger.error("Invalid source/target IDs in edge: {}", edge);
                continue; // Skip or handle appropriately
            }

                double thetaSource = thetas[sourceId];
                double thetaTarget = thetas[targetId];

                // We can define a relationship, e.g., thetas should reflect the edge weights
                double edgeWeight = edge.getWeight();

                // Add a penalty if the difference between theta values doesn't align with edge weight
                double expectedThetaDiff = Math.log(edgeWeight + 1e-6); // To avoid log(0)
                double actualThetaDiff = thetaTarget - thetaSource;

                double edgeError = actualThetaDiff - expectedThetaDiff;
                mse += edgeError * edgeError;
            }

            // Mean error
            mse /= trainingObservations.size();

            // Regularization term
            double reg = 0.0;
            for (double t : thetas) {
                reg += t * t;
            }
            reg = (lambda / numRepos) * reg;
            mse += reg;

            // Increment evaluation count
            evaluationCount++;

            // Estimate progress percentage
            double progress = ((double) evaluationCount / maxEvaluations) * 100;

            // Log progress at specified intervals
            if (evaluationCount % logInterval == 0 || evaluationCount ==1) {
            logger.info("Function evaluations: {}/{} ({:.2f}%), Current MSE: {}", evaluationCount, maxEvaluations, progress, mse);
            }
        }
        catch (Exception e) {
            logger.error("Exception in MSEFunction.value()", e);
            throw e;
            }
        return mse;
        }
    }

    // Optimize parameters using CMA-ES optimizer
    public void optimizeParameters() {
        logger.info("Starting optimization...");
        // Tolerance for convergence
        double relativeThreshold = 1e-6;
        double absoluteThreshold = 1e-6;
        int maxEvaluations = 5; // Set your desired maximum evaluations

        // Create RandomGenerator instance
        RandomGenerator randomGenerator = new MersenneTwister(42); // Seed can be set as needed

        // Create optimizer with custom convergence criteria
        CMAESOptimizer optimizer = new CMAESOptimizer(
                maxEvaluations,                   // maxIterations
                -Double.MAX_VALUE,      // stopFitness (no fitness stopping criterion)
                true,                   // isActiveCMA
                0,                      // diagonalOnly
                0,                      // checkFeasibleCount
                randomGenerator,        // random
                false,                  // generateStatistics
                new SimpleValueChecker(relativeThreshold, absoluteThreshold)
        );

        // Initial guess
        double[] startPoint = theta.clone();

        //Adjust population size 
        int populationSize = 4 + (int) (3* Math.log(numRepos));

        // Initial standard deviation (sigma)
        double[] sigmas = new double[numRepos];
        Arrays.fill(sigmas, 0.5); // Set initial standard deviation for each parameter

        // Define optimization problem
        MSEFunction mseFunction = new MSEFunction(maxEvaluations);
        ObjectiveFunction objectiveFunction = new ObjectiveFunction(mseFunction);

        // Define bounds
        double[] lowerBounds = new double[numRepos];
        Arrays.fill(lowerBounds, -10); // Lower bound for theta

        double[] upperBounds = new double[numRepos];
        Arrays.fill(upperBounds, 10); // Upper bound for theta
         // Perform optimization
         logger.info("Number of repositories (numRepos): {}", numRepos);
         logger.info("Initial startPoint length: {}", startPoint.length);
         logger.info("Sigmas length: {}", sigmas.length);
         logger.info("Lower bounds length: {}", lowerBounds.length);
         logger.info("Upper bounds length: {}", upperBounds.length);

         // Optionally, log the contents
         logger.debug("Initial startPoint values: {}", Arrays.toString(startPoint));
         logger.debug("Sigmas values: {}", Arrays.toString(sigmas));
         logger.debug("Lower bounds: {}", Arrays.toString(lowerBounds));
         logger.debug("Upper bounds: {}", Arrays.toString(upperBounds));

         // Validate input parameters
         for (int i = 0; i < numRepos; i++) {
             if (Double.isNaN(startPoint[i]) || Double.isInfinite(startPoint[i])) {
                 logger.error("Invalid value in startPoint at index {}: {}", i, startPoint[i]);
                 throw new IllegalArgumentException("Invalid startPoint value.");
             }
             if (Double.isNaN(sigmas[i]) || Double.isInfinite(sigmas[i]) || sigmas[i] <= 0) {
                 logger.error("Invalid sigma at index {}: {}", i, sigmas[i]);
                 throw new IllegalArgumentException("Invalid sigma value.");
             }
             if (Double.isNaN(lowerBounds[i]) || Double.isInfinite(lowerBounds[i]) ||
                 Double.isNaN(upperBounds[i]) || Double.isInfinite(upperBounds[i])) {
                 logger.error("Invalid bounds at index {}: lower={}, upper={}", i, lowerBounds[i], upperBounds[i]);
                 throw new IllegalArgumentException("Invalid bounds value.");
             }
             if (lowerBounds[i] >= upperBounds[i]) {
                 logger.error("Lower bound is not less than upper bound at index {}: lower={}, upper={}", i, lowerBounds[i], upperBounds[i]);
                 throw new IllegalArgumentException("Bounds error.");
             }
             if (startPoint[i] < lowerBounds[i] || startPoint[i] > upperBounds[i]) {
                 logger.error("startPoint[{}] out of bounds: value={}, lower={}, upper={}", i, startPoint[i], lowerBounds[i], upperBounds[i]);
                 throw new IllegalArgumentException("startPoint out of bounds.");
             }
         }

        try {
        
            logger.info("Calling optimizer.optimize()...");
            PointValuePair optimum = optimizer.optimize(
                    new MaxEval(maxEvaluations),
                    objectiveFunction,
                    GoalType.MINIMIZE,
                    new InitialGuess(startPoint),
                    new CMAESOptimizer.Sigma(sigmas),
                    new SimpleBounds(lowerBounds, upperBounds),
                    new CMAESOptimizer.PopulationSize(populationSize)
            );
            logger.info("Optimization completed.");

            theta = optimum.getPoint().clone();

            // Compute beta_i = exp(theta_i)
            beta = new double[numRepos];
            for (int i = 0; i < numRepos; i++) {
                beta[i] = Math.exp(theta[i]);
            }
        } catch (Exception e) {
            logger.error("Optimization failed", e);
            System.err.println("Optimization failed");
            e.printStackTrace();
            
        }
    }

    // Predict test data
    // In ModelService.java
    public ByteArrayResource predictTestData() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            writer.write("id,pred\n");
            for (TestObservation obs : testObservations) {
                String projectAURL = obs.getProjectAURL();
                String projectBURL = obs.getProjectBURL();

                Integer projectAId = repoToId.get(projectAURL);
                Integer projectBId = repoToId.get(projectBURL);

                double betaA = 1.0;
                double betaB = 1.0;

                if (projectAId != null) {
                    betaA = beta[projectAId];
                } else {
                    System.out.println("Repository not found in training data: " + projectAURL);
                }
                if (projectBId != null) {
                    betaB = beta[projectBId];
                } else {
                    System.out.println("Repository not found in training data: " + projectBURL);
                }
                double probA = betaA / (betaA + betaB);

                // Round to 11 decimal places
                String formattedProb = String.format("%.11f", probA);
                writer.write(obs.getId() + "," + formattedProb + "\n");
            }
            writer.flush();
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
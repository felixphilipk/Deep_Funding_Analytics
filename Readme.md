# Dependency Graph Analyzer 📊

Welcome to the **Dependency Graph Analyzer**! This project aims to predict funding allocation between open-source software repositories based on historical data and their dependency relationships. This is a challenge project to build a deep funding model.

We plan to integrate **Airbyte** for ingesting real-time data from multiple sources, specifically for the data listed in the [deepfunding/dependency-graph](https://github.com/deepfunding/dependency-graph) repository.

## 🚀 Getting Started

These instructions will help you set up and run the project on your local machine for development and testing purposes.

### Prerequisites 📦

Before you begin, ensure you have met the following requirements:

- **Java Development Kit (JDK) 11 or higher** installed on your machine.
- **Maven** installed for building and managing the project dependencies.
- **Git** for cloning the repository.

### Required Files 📁

You will need the following data files to run the model which is located in data folder for real time data please visit the deep funding graph github repo :

1. **`data.csv`**: Training data containing funding allocations between pairs of repositories.
2. **`test.csv`**: Test data containing pairs of repositories for which you want to predict funding allocation.
3. **`dependency_graph.csv`**: A CSV file representing the dependency graph of repositories.

Download the necessary data files from the [deepfunding/dependency-graph](https://github.com/deepfunding/dependency-graph) repository or use your own data. Ensure these files are accessible to the application and change the schema accordingly.

> **Note:** We are planning to integrate real-time data ingestion using **Airbyte** in the future! 🌐

## 📥 Installation

Clone the repository to your local machine:

```bash
git clone https://github.com/YourUsername/dependency-graph-analyzer.git
cd dependency-graph-analyzer
```

## ⚙️ Configuration

⚠️GitHub Token is required in the configuration otherwise it will result in an error please add your github token.

If you have any application properties to set, you can modify the `src/main/resources/application.properties` file:
🔑
```properties
github.token= " Your Github Token Here "
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

## 🔌 Running the Application

You can run the application using Maven or by building a JAR file.

### Option 1: Using Maven

Ensure you are in the project directory and run:

```bash
mvn spring-boot:run
```

### Option 2: Building and Running the JAR File

1. **Build the JAR file:**

   ```bash
   mvn clean package
   ```

   This will generate a JAR file in the `target` directory.

2. **Run the JAR file:**

   ```bash
   java -jar target/dependency-graph-analyzer-0.0.1-SNAPSHOT.jar
   ```

The application will start on `http://localhost:8080`.

## 📊 Usage

The application exposes a RESTful API that you can interact with to run the analysis.

### API Endpoint ⚡

- **URL:** `http://localhost:8080/api/analysis/run`
- **Method:** `POST`
- **Content-Type:** `multipart/form-data`

### Request Parameters 📨

The API expects the following form-data parameters:

- `trainingData`: The `data.csv` file containing training data.
- `testData`: The `test.csv` file containing test data.
- `dependencyGraph`: The `dependency_graph.csv` file representing the dependency graph.

### Example Request Using `curl` 📤

```bash
curl -X POST \
  -F "trainingData=@/path/to/data.csv" \
  -F "testData=@/path/to/test.csv" \
  -F "dependencyGraph=@/path/to/dependency_graph.csv" \
  http://localhost:8080/api/analysis/run -o output.csv
```

This command will send the required files to the API and save the output predictions to `output.csv`.

### Response 📬

The API returns a CSV file with the following columns:

- **`id`**: The unique identifier for each test observation.
- **`pred`**: The predicted probability that `project_a` should receive funding over `project_b`.

## 🛠️ Project Structure

- **`src/main/java/com/deepfunding/dependency_graph_analyzer`**: Contains the Java source code.
  - **`controller`**: REST API controllers.
    - `AnalysisController.java`: Handles API requests and responses.
  - **`model`**: Data models representing observations and repository features.
    - `Observation.java`
    - `TestObservation.java`
    - `DependencyEdge.java`
    - `RepoFeatures.java`
  - **`service`**: Business logic and model computation.
    - `DataIngestionService.java`: Reads and processes input data files.
    - `ModelService.java`: Contains the core modeling and optimization logic.
    - `GitHubDataService.java`: (Currently not in use, planned for future enhancements.)
  - **`DependencyGraphAnalyzerApplication.java`**: Main application class.
- **`src/main/resources`**: Configuration files and resources.
  - `application.properties`: Application configuration.
- **`pom.xml`**: Maven project configuration file.

## 🧪 Running Tests

Currently, there are no unit tests included. Contributions for testing are welcome!

To run tests (once added):

```bash
mvn test
```

## 🔧 Configuration Options

- **Logging Level:** Adjust the logging level in `src/main/resources/application.properties`:

  ```properties
  logging.level.com.deepfunding=INFO
  ```

- **Model Parameters:** Modify model parameters in `ModelService.java` as needed, such as:

  - **Regularization Parameter (`lambda`):** Controls the strength of regularization.
  - **Optimizer Settings:** Adjust parameters like `maxEvaluations`, `relativeThreshold`, and `absoluteThreshold` for optimization.

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork the repository.**
2. **Create a new branch** for your feature or bug fix:

   ```bash
   git checkout -b feature-name
   ```

3. **Make your changes** and commit them:

   ```bash
   git commit -am 'Add new feature'
   ```

4. **Push to the branch**:

   ```bash
   git push origin feature-name
   ```

5. **Open a Pull Request** on GitHub.

Please ensure your code adheres to existing styles and includes appropriate comments.

### Planned Enhancements

- **Integration with Airbyte**: For ingesting real-time data from multiple sources.
- **Data Augmentation**: Including additional repository features (stars, forks, etc.) in the model.
- **Unit Tests**: Adding comprehensive tests to ensure code reliability.


## 📲 Support

If you have any questions or need assistance, please open an issue in the repository.

---

Happy coding! 👩‍💻👨‍💻

---

## 🌟 Future Plans

- **Real-Time Data Integration**: We plan to integrate [Airbyte](https://airbyte.io/) for ingesting real-time data from multiple sources, enhancing prediction accuracy.
- **Enhanced Analytics**: Additional analytical tools and visualizations to better understand the model outputs.
- **User Interface**: Developing a web-based user interface for easier interaction with the model.

Stay tuned for updates! 🚀

---

## Important Notes

- **Disclaimer**: This project is a challenge to build a deep funding model and is **not associated with Deep Funding**.
- **Data Sources**: The dependency graph and other data can be found in the [deepfunding/dependency-graph](https://github.com/deepfunding/dependency-graph) repository.

---

## Resources 📚

- **Dependency Graph Data**: [deepfunding/dependency-graph](https://github.com/deepfunding/dependency-graph)
- **Airbyte Integration**: [Airbyte Documentation](https://docs.airbyte.io/)



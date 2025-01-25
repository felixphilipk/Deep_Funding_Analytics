package com.deepfunding.dependency_graph_analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class DependencyGraphAnalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DependencyGraphAnalyzerApplication.class, args);
	}

}

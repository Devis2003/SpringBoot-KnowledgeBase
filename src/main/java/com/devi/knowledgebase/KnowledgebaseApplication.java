package com.devi.knowledgebase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan
public class KnowledgebaseApplication {

	public static void main(String[] args) {
		SpringApplication.run(KnowledgebaseApplication.class, args);
	}

}

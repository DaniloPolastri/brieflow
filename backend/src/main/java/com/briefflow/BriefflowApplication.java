package com.briefflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BriefflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(BriefflowApplication.class, args);
	}

}

package com.streamsphere.central;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CentralApplication {

	public static void main(String[] args) {
		SpringApplication.run(CentralApplication.class, args);
	}

}


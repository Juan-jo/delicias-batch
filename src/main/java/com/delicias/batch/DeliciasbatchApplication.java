package com.delicias.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeliciasbatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliciasbatchApplication.class, args);
	}

}

package com.delicias.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(scanBasePackages = {"com.delicias.batch", "com.delicias.soft.services"})
@EnableScheduling
public class DeliciasbatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliciasbatchApplication.class, args);
	}

}

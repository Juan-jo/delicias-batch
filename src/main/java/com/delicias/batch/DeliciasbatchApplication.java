package com.delicias.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SpringBootApplication(scanBasePackages = {"com.delicias.batch", "com.delicias.soft.services"})
@EnableScheduling
public class DeliciasbatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliciasbatchApplication.class, args);
	}


    @Bean(destroyMethod = "shutdown")
    //@Bean(name = "consumerExecutor", destroyMethod = "shutdown")
    public ExecutorService assignmentExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}

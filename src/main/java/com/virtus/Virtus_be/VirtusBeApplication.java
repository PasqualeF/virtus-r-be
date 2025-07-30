package com.virtus.Virtus_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "com.virtus")
@EnableScheduling
public class VirtusBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(VirtusBeApplication.class, args);
	}

}

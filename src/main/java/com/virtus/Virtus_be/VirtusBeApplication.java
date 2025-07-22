package com.virtus.Virtus_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.virtus")
public class VirtusBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(VirtusBeApplication.class, args);
	}

}

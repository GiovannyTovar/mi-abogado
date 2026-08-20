package com.miabogado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AbogadoApplication {

	public static void main(String[] args) {
		SpringApplication.run(AbogadoApplication.class, args);
	}

}

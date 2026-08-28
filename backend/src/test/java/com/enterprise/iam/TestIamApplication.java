package com.enterprise.iam;

import org.springframework.boot.SpringApplication;

public class TestIamApplication {

	public static void main(String[] args) {
		SpringApplication.from(IamApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

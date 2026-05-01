package br.com.coopticket.coopticket_api;

import org.springframework.boot.SpringApplication;

public class TestCoopticketApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoopticketApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

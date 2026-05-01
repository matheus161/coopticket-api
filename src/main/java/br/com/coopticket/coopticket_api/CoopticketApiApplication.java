package br.com.coopticket.coopticket_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "br.com.coopticket")
public class CoopticketApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoopticketApiApplication.class, args);
	}

}

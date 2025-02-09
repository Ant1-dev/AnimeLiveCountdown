package com.Toine.animeCountdownBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AnimeCountdownBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(AnimeCountdownBackendApplication.class, args);
	}

}

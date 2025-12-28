package ru.bauman.andesis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "ru.bauman.andesis")
public class AndesisApplication {

    public static void main(String[] args) {
        log.info("Starting Andesis Application...");
        SpringApplication.run(AndesisApplication.class, args);
        log.info("Andesis Application started successfully");
    }
}

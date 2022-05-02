package com.example;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class iemdbApplication {
    public static void main(String[] args) {
        System.setProperty("server.port", "8080");
        SpringApplication.run(iemdbApplication.class, args);
    }
}
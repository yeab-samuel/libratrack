package com.libratrack;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class LibratrackApplication {
    public static void main(String[] args) { SpringApplication.run(LibratrackApplication.class, args); }
}

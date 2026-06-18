package com.muasamcong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MuasamcongApplication {

    public static void main(String[] args) {
        SpringApplication.run(MuasamcongApplication.class, args);
    }

}

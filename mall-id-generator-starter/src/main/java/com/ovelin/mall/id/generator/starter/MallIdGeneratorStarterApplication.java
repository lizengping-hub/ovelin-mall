package com.ovelin.mall.id.generator.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MallIdGeneratorStarterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallIdGeneratorStarterApplication.class, args);
    }

}

package com.ovelin.mall.sharding.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MallShardingStarterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallShardingStarterApplication.class, args);
    }

}

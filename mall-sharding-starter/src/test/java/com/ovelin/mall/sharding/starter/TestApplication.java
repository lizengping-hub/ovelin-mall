package com.ovelin.mall.sharding.starter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;


@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class TestApplication {
}

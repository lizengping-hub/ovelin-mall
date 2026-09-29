package com.ovelin.mall.common.sharding.starter.autoconfigure;

import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.core.RandomShardResolver;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.io.File;
import java.net.URL;

@AutoConfiguration(beforeName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
public class ShardingSphereAutoConfiguration {

    @Bean
    public DataSource dataSource() throws Exception {
        URL yamlUrl = getClass().getClassLoader().getResource("sharding-config.yaml");
        if (yamlUrl == null) {
            throw new IllegalStateException("sharding-config.yaml not found in classpath");
        }
        File yamlFile = new File(
               yamlUrl.getFile()
        );
        return YamlShardingSphereDataSourceFactory.createDataSource(yamlFile);
    }

    @Bean
    public ShardResolver shardResolver() {
        return new RandomShardResolver();
    }
}
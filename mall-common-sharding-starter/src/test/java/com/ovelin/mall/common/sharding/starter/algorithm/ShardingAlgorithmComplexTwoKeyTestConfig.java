package com.ovelin.mall.common.sharding.starter.algorithm;

import java.util.Properties;
import java.util.Set;

public class ShardingAlgorithmComplexTwoKeyTestConfig {
    public record Config(Properties properties,int dsCount, int tableCount, Set<String> dsNames, Set<String> tableNames) {
    }
    public static Config CONFIG_2_2;
    public static Config CONFIG_4_2;
    public static Config CONFIG_4_4;
    static {
        Properties properties = new Properties();
        properties.setProperty("ds_count", "2");;
        properties.setProperty("table_count", "2");
        properties.setProperty("sharding_string_field_0", "identity_type");;
        properties.setProperty("sharding_string_field_1", "identifier");
        CONFIG_2_2 = new Config(properties, 2, 2, Set.of("ds0", "ds1"), Set.of("table0", "table1"));


        properties = new Properties();
        properties.setProperty("ds_count", "4");;
        properties.setProperty("table_count", "2");
        properties.setProperty("sharding_string_field_0", "identity_type");;
        properties.setProperty("sharding_string_field_1", "identifier");
        CONFIG_4_2 = new Config(properties, 4, 2, Set.of("ds0", "ds1", "ds2", "ds3"), Set.of("table0", "table1"));

        properties = new Properties();
        properties.setProperty("ds_count", "4");;
        properties.setProperty("table_count", "4");
        properties.setProperty("sharding_string_field_0", "identity_type");;
        properties.setProperty("sharding_string_field_1", "identifier");
        CONFIG_4_4 = new Config(properties, 4, 4, Set.of("ds0", "ds1", "ds2", "ds3"), Set.of("table0", "table1", "table2", "table3"));

    }
}

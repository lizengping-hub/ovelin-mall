package com.ovelin.mall.common.sharding.starter.algorithm;

import org.junit.jupiter.api.BeforeAll;

import java.util.Properties;
import java.util.Set;

public class AbstractShardingAlgorithmTest {
    public record Config(Properties properties,int dsCount, int tableCount, Set<String> dsNames, Set<String> tableNames) {
    }
    protected static Config CONFIG_2_2;
    protected static Config CONFIG_4_2;
    protected static Config CONFIG_4_4;
    @BeforeAll
    static void setUp() {
        Properties properties = new Properties();
        properties.setProperty("ds-count", "2");;
        properties.setProperty("table-count", "2");
        CONFIG_2_2 = new Config(properties, 2, 2, Set.of("ds0", "ds1"), Set.of("table0", "table1"));


        properties = new Properties();
        properties.setProperty("ds-count", "4");;
        properties.setProperty("table-count", "2");
        CONFIG_4_2 = new Config(properties, 4, 2, Set.of("ds0", "ds1", "ds2", "ds3"), Set.of("table0", "table1"));

        properties = new Properties();
        properties.setProperty("ds-count", "4");;
        properties.setProperty("table-count", "4");
        CONFIG_4_4 = new Config(properties, 4, 4, Set.of("ds0", "ds1", "ds2", "ds3"), Set.of("table0", "table1", "table2", "table3"));

    }
}

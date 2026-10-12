package com.ovelin.mall.common.sharding.core.core;

import org.junit.jupiter.api.Test;

public class RandomShardSelectorTest {
    @Test
    void shardSelector(){
        RandomShardSelector selector = new RandomShardSelector();
        for (int i = 0; i < 100; i++) {
            selector.select();
        }
    }
}

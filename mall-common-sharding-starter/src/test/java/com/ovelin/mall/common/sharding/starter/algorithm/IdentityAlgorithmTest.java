package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.Test;

public class IdentityAlgorithmTest {
    record ShardingResult(long databaseIndex, long tableIndex){}
    @Test
    void algorithmTets(){

        ShardingResult result = null;
        for (int i = 0; i < 100; i++) {
            int databaseCount = 4;
            int tableCount = 2;
            ShardedId sharedId = ShardedId.of(ShardId.of(i), System.currentTimeMillis());
            System.out.println("--------------------------------------------------");
            result = doSharding2(databaseCount, tableCount, sharedId.value());
            System.out.println("Sharding_2 ID: " + sharedId + ", database Count: " + databaseCount + ", table Count: " + tableCount + ", Database Index: " + result.databaseIndex() + ", Table Index: " + result.tableIndex());
            result = doSharding4(databaseCount, tableCount, sharedId.value());
            System.out.println("Sharding_4 ID: " + sharedId + ", database Count: " + databaseCount + ", table Count: " + tableCount + ", Database Index: " + result.databaseIndex() + ", Table Index: " + result.tableIndex());
            System.out.println();
            databaseCount = databaseCount * 2;
//            tableCount = tableCount * 2;
            result = doSharding2(databaseCount, tableCount, sharedId.value());
            System.out.println("Sharding_2 ID: " + sharedId + ", database Count: " + databaseCount + ", table Count: " + tableCount + ", Database Index: " + result.databaseIndex() + ", Table Index: " + result.tableIndex());
            result = doSharding4(databaseCount, tableCount, sharedId.value());
            System.out.println("Sharding_4 ID: " + sharedId + ", database Count: " + databaseCount + ", table Count: " + tableCount + ", Database Index: " + result.databaseIndex() + ", Table Index: " + result.tableIndex());
            System.out.println();
        }
    }
    ShardingResult doSharding1(int databaseCount, int tableCount, long id){
        long databaseIndex = id % databaseCount;
        long tableIndex = id % tableCount;
        return new ShardingResult(databaseIndex, tableIndex);
    }
    ShardingResult doSharding2(int databaseCount, int tableCount, long id){
        long databaseIndex = (id / tableCount) % databaseCount;
        long tableIndex = id % tableCount;
        return new ShardingResult(databaseIndex, tableIndex);
    }
    ShardingResult doSharding3(int databaseCount, int tableCount, long id){
        long databaseIndex = (id / tableCount) % databaseCount;
        long tableIndex = (id / databaseCount) % tableCount;
        return new ShardingResult(databaseIndex, tableIndex);
    }
    ShardingResult doSharding4(int databaseCount, int tableCount, long id){
        long shardingValue = id % ((long) databaseCount * tableCount);
        long databaseIndex = shardingValue / tableCount;
        long tableIndex = shardingValue % tableCount;
        return new ShardingResult(databaseIndex, tableIndex);
    }
    ShardingResult doSharding5(int databaseCount, int tableCount, long id){
        long databaseIndex =   id % databaseCount;
        long tableIndex = id / databaseCount % tableCount;
        return new ShardingResult(databaseIndex, tableIndex);
    }
}

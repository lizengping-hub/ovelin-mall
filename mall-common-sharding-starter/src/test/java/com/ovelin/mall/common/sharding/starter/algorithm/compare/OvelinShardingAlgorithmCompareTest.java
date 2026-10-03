package com.ovelin.mall.common.sharding.starter.algorithm.compare;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
public class OvelinShardingAlgorithmCompareTest {
    record ShardingResult(long databaseIndex, long tableIndex){}

    @Test
    void algorithm1Test(){
       assertThrows(AssertionFailedError.class, () -> {
            testAlgorithm(this::algorithm1);
        });
    }
    @Test
    void algorithm2Test(){
        testAlgorithm(this::algorithm2);
    }
    @Test
    void algorithm3Test(){
        assertThrows(AssertionFailedError.class, () -> {
            testAlgorithm(this::algorithm3);
        });

    }
    @Test
    void algorithm4Test(){
        testAlgorithm(this::algorithm4);
    }
    @Test
    void algorithm5Test(){
        assertThrows(AssertionFailedError.class, () -> {
            testAlgorithm(this::algorithm5);
        });
    }
    void testAlgorithm(TriFunction<Integer, Integer, Integer, ShardingResult> function){
        for (int i = 0; i < 100; i++) {
            int databaseCount = 4;
            int tableCount = 2;
            ShardedId sharedId = ShardedId.of(i, System.currentTimeMillis());
            ShardingResult result = function.apply(databaseCount, tableCount, sharedId.shardId().value());

            ShardingResult resultSplit = function.apply(databaseCount * 2, tableCount, sharedId.shardId().value());
            if (i % (databaseCount * 2 * tableCount) < databaseCount * 2){
                assertEquals(result.databaseIndex(), resultSplit.databaseIndex());
            }
            if (i % (databaseCount * 2 * tableCount) >= databaseCount * 2){
                assertEquals(result.databaseIndex() + databaseCount, resultSplit.databaseIndex());
            }
            assertEquals(result.tableIndex(), resultSplit.tableIndex());
        }
    }

    ShardingResult algorithm1(int databaseCount, int tableCount, long id){
        long databaseIndex = id % databaseCount;
        long tableIndex = id % tableCount;
        return new ShardingResult(databaseIndex, tableIndex);
    }
    ShardingResult algorithm2(int databaseCount, int tableCount, long id){
        long databaseIndex = (id / tableCount) % databaseCount;
        long tableIndex = id % tableCount;
        return new ShardingResult(databaseIndex, tableIndex);
    }
    ShardingResult algorithm3(int databaseCount, int tableCount, long id){
        long databaseIndex = (id / tableCount) % databaseCount;
        long tableIndex = (id / databaseCount) % tableCount;
        return new ShardingResult(databaseIndex, tableIndex);
    }
    ShardingResult algorithm4(int databaseCount, int tableCount, long id){
        long shardingValue = id % ((long) databaseCount * tableCount);
        long databaseIndex = shardingValue / tableCount;
        long tableIndex = shardingValue % tableCount;
        return new ShardingResult(databaseIndex, tableIndex);
    }
    ShardingResult algorithm5(int databaseCount, int tableCount, long id){
        long databaseIndex =   id % databaseCount;
        long tableIndex = id / databaseCount % tableCount;
        return new ShardingResult(databaseIndex, tableIndex);
    }
}
@FunctionalInterface
interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}
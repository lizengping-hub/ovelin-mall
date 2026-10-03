package com.ovelin.mall.common.sharding.core.ov;

import com.ovelin.mall.common.sharding.core.api.DataSourceIndexResolver;
import com.ovelin.mall.common.sharding.core.core.DataSourceIndexResolverImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DataSourceIndexResolverTest {
    DataSourceIndexResolver resolver = new DataSourceIndexResolverImpl();
    @Test
    void ds2Table2ResolveTest(){
        assertDs2Table2Resolve(0, 0);
        assertDs2Table2Resolve(1, 0);
        assertDs2Table2Resolve(2, 1);
        assertDs2Table2Resolve(3, 1);
        assertDs2Table2Resolve(4, 0);
        assertDs2Table2Resolve(5, 0);
        assertDs2Table2Resolve(6, 1);
        assertDs2Table2Resolve(7, 1);
    }
    @Test
    void ds4Table2ResolveTest(){
        assertDs4Table2Resolve(0, 0);
        assertDs4Table2Resolve(1, 0);
        assertDs4Table2Resolve(2, 1);
        assertDs4Table2Resolve(3, 1);
        assertDs4Table2Resolve(4, 2);
        assertDs4Table2Resolve(5, 2);
        assertDs4Table2Resolve(6, 3);
        assertDs4Table2Resolve(7, 3);
    }
    @Test
    void ds4Table4ResolveTest(){
        assertDs4Table4Resolve(0, 0);
        assertDs4Table4Resolve(1, 0);
        assertDs4Table4Resolve(2, 0);
        assertDs4Table4Resolve(3, 0);
        assertDs4Table4Resolve(4, 1);
        assertDs4Table4Resolve(5, 1);
        assertDs4Table4Resolve(6, 1);
        assertDs4Table4Resolve(7, 1);
    }
    void assertDs2Table2Resolve( int shardIdValue, int expectedIndex) {
        assertResolved(2, 2, shardIdValue, expectedIndex);
    }
    void assertDs4Table2Resolve( int shardIdValue, int expectedIndex) {
        assertResolved(4, 2, shardIdValue, expectedIndex);
    }
    void assertDs4Table4Resolve( int shardIdValue, int expectedIndex) {
        assertResolved(4, 4, shardIdValue, expectedIndex);
    }
    void assertResolved(int dsCount, int tableCount, int shardIdValue, int expectedIndex) {
        ShardId shardId = ShardId.of(shardIdValue);
        int index = resolver.resolve(shardId, dsCount, tableCount);
        assertThat(expectedIndex).isEqualTo(index);

    }
}

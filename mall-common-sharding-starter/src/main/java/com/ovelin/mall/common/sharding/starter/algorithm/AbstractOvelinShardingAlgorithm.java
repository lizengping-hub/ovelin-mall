package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.api.DataSourceIndexResolver;
import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.api.TableIndexResolver;
import com.ovelin.mall.common.sharding.core.core.DataSourceIndexResolverImpl;
import com.ovelin.mall.common.sharding.core.core.HashShardResolver;
import com.ovelin.mall.common.sharding.core.core.TableIndexResolverImpl;
import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.apache.shardingsphere.sharding.spi.ShardingAlgorithm;

import java.util.Properties;

/**
 * 分片算法基类，处理分片数据源数量每个数据源中表数量的配置，和将不同类型的字段应用不同的分片逻辑，目前支持ShardedId和String
 * @param <T>
 */
abstract class AbstractOvelinShardingAlgorithm<T extends Comparable<?>> implements ShardingAlgorithm {
    protected int dsCount;
    protected int tableCount;
    protected static final ShardResolver shardResolver = new HashShardResolver();
    protected static final DataSourceIndexResolver dataSourceIndexResolver = new DataSourceIndexResolverImpl();
    protected static final TableIndexResolver tableIndexResolver = new TableIndexResolverImpl();
    @Override
    public void init(Properties props) {
        Object dsCount = props.get("ds_count");
        if (dsCount == null) {
            throw new IllegalArgumentException("ds_count is required");
        }
        try {
            this.dsCount = Integer.parseInt(dsCount.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ds_count must be a valid number", e);
        }

        Object tableCount = props.get("table_count");
        if (tableCount == null) {
            throw new IllegalArgumentException("table_count is required");
        }
        try {
            this.tableCount = Integer.parseInt(tableCount.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("table_count must be a valid number", e);
        }
    }
    protected abstract int resolveIndex(ShardId shardId);
    protected int resolveIndex(Comparable<?> shardKeyObj) {
        if (shardKeyObj instanceof Long shardedId) {
            ShardId shardId = ShardId.of(ShardedId.shardOf(shardedId));
            return resolveIndex(shardId);
        } else if (shardKeyObj instanceof String shardKey) {
            ShardId shardId = shardResolver.resolve(shardKey); // validate shardKey
            return resolveIndex(shardId);
        } else {
            throw new IllegalArgumentException("Unsupported id type: " + shardKeyObj.getClass().getName());
        }
    }
}

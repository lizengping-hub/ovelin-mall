package com.ovelin.mall.common.sharding.starter.algorithm;

import org.apache.commons.lang3.Strings;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.*;
import java.util.stream.Collectors;
/**
 * 多个字段的每个字段都支持分片，这写字段的最终分片位置应该一样，有任意一个值即可完成分片；
 * 不校验字段名字，支持String和ShardedId(Long)两种类型
 */
abstract class AbstractOvelinComplexShardingAlgorithm extends AbstractOvelinShardingAlgorithm<Comparable<?>> implements ComplexKeysShardingAlgorithm<Comparable<?>> {
    private String shardingStringField0;
    private String shardingStringField1;
    @Override
    public void init(Properties props) {
        super.init(props);
        Object field0 = props.get("sharding_string_field_0");
        Object field1 = props.get("sharding_string_field_1");
        if ((field0 != null && field1 == null)
            || (field0 == null && field1 != null)){
            throw new IllegalArgumentException("sharding_string_field_0 and sharding_string_field_1 must both be set or unset");
        }
        if (field0 != null){
            shardingStringField0 = field0.toString().trim();
            shardingStringField1 = field1.toString().trim();
            if (shardingStringField0.contains(" ")){
                throw new IllegalArgumentException("sharding_string_field_0 contains empty char");
            }
            if (shardingStringField1.contains(" ")){
                throw new IllegalArgumentException("sharding_string_field_1 contains empty char");
            }

        }

    }
    /**
     * doSharding for data source or table name, if sharding value is range query, return all available target names
     * @param availableTargetNames available data sources or table names
     * @param shardingValue sharding value
     * @return
     */
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, ComplexKeysShardingValue<Comparable<?>> shardingValue) {
        if (shardingValue.getColumnNameAndRangeValuesMap() != null && !shardingValue.getColumnNameAndRangeValuesMap().isEmpty()) {
            return availableTargetNames; // 范围查询暂不优化，全库扫
        }

        Map<String, Collection<Comparable<?>>> values =
                shardingValue.getColumnNameAndShardingValuesMap();
        Collection<Comparable<?>> shardKeys = null;
        if (shardingStringField0 == null || values.size() == 1){
            for (Collection<Comparable<?>> valuesCollection : values.values()) {
                if (valuesCollection != null && !valuesCollection.isEmpty()) {
                    shardKeys = valuesCollection;
                    break;
                }
            }
        } else if ( values.size() == 3){
            // 一般情况下，两个String 联合 分片，找出另外一个可实现单独分片
            for (Collection<Comparable<?>> valuesCollection : values.values()) {
                if (valuesCollection != null && !valuesCollection.isEmpty() && !(valuesCollection.iterator().next() instanceof String)) {
                    shardKeys = valuesCollection;
                    break;
                }
            }
        } else if (values.size() == 2){
            // 只有两个分片
            Collection<Comparable<?>> field0Values = values.get(shardingStringField0);
            Collection<Comparable<?>> field1Values = values.get(shardingStringField1);
            if (field0Values == null || field1Values == null){
                throw new IllegalArgumentException("Value of %s or %s not found".formatted(shardingStringField0, shardingStringField1));
            }
            if (field0Values.size() != 1 || field1Values.size() != 1){
                throw new IllegalArgumentException("Value count must be 1 of %s or %s".formatted(shardingStringField0, shardingStringField1));
            }
            Comparable<?> field0Value =  field0Values.iterator().next();
            Comparable<?> field1Value =  field1Values.iterator().next();

            if (!(field0Value instanceof String) || !(field1Value instanceof String)){
                throw new IllegalArgumentException("Value must be String type of %s or %s".formatted(shardingStringField0, shardingStringField1));
            }

            shardKeys = Collections.singleton(AbstractOvelinShardingAlgorithm.shardResolver.buildShardKey((String) field0Value, (String) field1Value));
        }

        if (shardKeys == null) {
            return availableTargetNames;
        }
        if (shardKeys.size() == 1) {
            Comparable<?> shardKey = shardKeys.iterator().next();
            int resolvedIndex = resolveIndex(shardKey);
            return availableTargetNames.stream().filter(target -> target.endsWith(String.valueOf(resolvedIndex))).collect(Collectors.toSet());
        } else {
            Set<String> resolvedIndexes = shardKeys.stream().map(this::resolveIndex).map(String::valueOf).collect(Collectors.toSet());
            return availableTargetNames.stream().filter(target -> {
                for (String resolvedIndex : resolvedIndexes) {
                    if (target.endsWith(resolvedIndex)) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toSet());
        }
    }
}

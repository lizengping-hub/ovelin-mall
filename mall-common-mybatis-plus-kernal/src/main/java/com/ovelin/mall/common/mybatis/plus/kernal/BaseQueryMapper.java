package com.ovelin.mall.common.mybatis.plus.kernal;

import com.baomidou.mybatisplus.core.batch.BatchSqlSession;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.*;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.executor.BatchResult;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * DDD 查询通道专属：只读 Mapper 基类
 * 继承自 MyBatis-Plus BaseMapper，利用多态和默认方法彻底封死写入操作
 */
public interface BaseQueryMapper<T> extends BaseMapper<T> {

    // ==========================================
    // ❌ 彻底禁用所有写入、更新方法 (打上废弃标签并抛出异常)
    // ==========================================

    @Override
    @Deprecated
    default int insert(T entity) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 insert 写入操作！");
    }

    @Override
    @Deprecated
    default int deleteById(Serializable id) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 deleteById 删除操作！");
    }

    @Override
    @Deprecated
    default int deleteById(Object obj, boolean useFill) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 deleteById 删除操作！");
    }

    @Override
    @Deprecated
    default int deleteById(T entity) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 deleteById 删除操作！");
    }

    @Override
    @Deprecated
    default int deleteByMap(Map<String, Object> columnMap) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 deleteByMap 删除操作！");
    }

    @Override
    @Deprecated
    default int delete(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 delete 删除操作！");
    }

    @Override
    @Deprecated
    default int deleteBatchIds(@Param(Constants.COLL) Collection<?> idList) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 delete 批量删除操作！");
    }

    @Override
    @Deprecated
    default int deleteByIds(@Param(Constants.COLL) Collection<?> idList) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 delete 批量删除操作！");
    }

    @Override
    @Deprecated
    default int deleteByIds(@Param(Constants.COLL) Collection<?> collections, boolean useFill) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 delete 批量删除操作！");
    }

    @Override
    @Deprecated
    default int updateById(T entity) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 updateById 更新操作！");
    }

    @Override
    @Deprecated
    default int update(T entity, @Param(Constants.WRAPPER) Wrapper<T> updateWrapper) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 update 更新操作！");
    }

    @Override
    @Deprecated
    default int update(@Param(Constants.WRAPPER) Wrapper<T> updateWrapper) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 update 更新操作！");
    }
    @Override
    @Deprecated
    default boolean insertOrUpdate(T entity) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 insertOrUpdate 更新操作！");
    }


    @Override
    @Deprecated
    default List<BatchResult> insert(Collection<T> entityList) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 insert 更新操作！");
    }

    @Override
    @Deprecated
    default List<BatchResult> insert(Collection<T> entityList, int batchSize) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 insert 更新操作！");
    }

    @Override
    @Deprecated
    default List<BatchResult> updateById(Collection<T> entityList) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 updateById 更新操作！");
    }

    @Override
    @Deprecated
    default List<BatchResult> updateById(Collection<T> entityList, int batchSize) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 updateById 更新操作！");
    }

    @Override
    @Deprecated
    default List<BatchResult> insertOrUpdate(Collection<T> entityList) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 insertOrUpdate 更新操作！");
    }

    @Override
    @Deprecated
    default List<BatchResult> insertOrUpdate(Collection<T> entityList, int batchSize) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 insertOrUpdate 更新操作！");
    }

    @Override
    @Deprecated
    default List<BatchResult> insertOrUpdate(Collection<T> entityList, BiPredicate<BatchSqlSession, T> insertPredicate) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 insertOrUpdate 更新操作！");
    }

    @Override
    @Deprecated
    default List<BatchResult> insertOrUpdate(Collection<T> entityList, BiPredicate<BatchSqlSession, T> insertPredicate, int batchSize) {
        throw new UnsupportedOperationException("【架构守护】Query 通道禁止执行 insertOrUpdate 更新操作！");
    }
    // ==========================================
    // 🟢 正常保留 BaseMapper 的所有查询方法 (无需重写)
    // ==========================================
    // selectById, selectBatchIds, selectByMap, selectOne, selectCount,
    // selectList, selectMaps, selectMapsPage, selectPage 等查询能力完全不受影响
}

package com.ovelin.mall.sharding.starter.core;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * 把 {@link ShardingConfigurationException} 转换成简洁的启动失败提示，
 * 避免因为一处配置缺失就打印整份 Conditions Evaluation Report 和冗长堆栈。
 */
public class ShardingConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<ShardingConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ShardingConfigurationException cause) {
        String description = "分片配置（ovelin.sharding.*）不合法：" + cause.getMessage();
        String action = "请检查 application.yml/properties 中 ovelin.sharding.instances 与 "
                + "ovelin.sharding.shard-routes 的配置，确保 instances 已定义、"
                + "shard-routes 覆盖全部分片且引用的 instance 存在。";
        return new FailureAnalysis(description, action, cause);
    }
}

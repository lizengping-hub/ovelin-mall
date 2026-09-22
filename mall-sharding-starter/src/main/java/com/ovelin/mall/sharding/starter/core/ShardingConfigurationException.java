package com.ovelin.mall.sharding.starter.core;

/**
 * ovelin.sharding.* 配置缺失或不合法时抛出。
 * 配合 {@link ShardingConfigurationFailureAnalyzer} 使用，
 * 让启动失败时只打印一段简洁的提示，而不是整份 Conditions Evaluation Report。
 */
public class ShardingConfigurationException extends RuntimeException {
    public ShardingConfigurationException(String message) {
        super(message);
    }
}

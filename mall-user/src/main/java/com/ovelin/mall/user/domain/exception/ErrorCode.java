package com.ovelin.mall.user.domain.exception;

public enum ErrorCode {
    CONCURRENT_UPDATE("数据已被修改，请刷新后重试"),
    INVALID_ARGUMENT("参数错误"),
    INVALID_USER_STATUS("当前用户状态不允许该操作"),
    USER_NOT_FOUND("用户不存在");

    private final String clientMessage;
    ErrorCode(String clientMessage) { this.clientMessage = clientMessage; }
    public String clientMessage() { return clientMessage; }
}
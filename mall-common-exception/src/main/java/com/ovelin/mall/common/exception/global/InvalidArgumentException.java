package com.ovelin.mall.common.exception.global;

import com.ovelin.mall.common.exception.global.face.GlobalDomainException;
import com.ovelin.mall.common.exception.global.face.GlobalErrorCode;

/**
 * 该异常会将错误信息同时设置为message和clientMessage
 */
public class InvalidArgumentException extends GlobalDomainException {
    public InvalidArgumentException(String message) {
        super(GlobalErrorCode.INVALID_ARGUMENT, message);   // 校验文案本来就是给用户看的
    }
}
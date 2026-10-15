package com.ovelin.mall.common.exception.global;

import com.ovelin.mall.common.exception.DomainException;
import com.ovelin.mall.common.exception.global.face.GlobalDomainException;
import com.ovelin.mall.common.exception.global.face.GlobalErrorCode;

/**
 * 数据已被修改
 */
public class ConcurrentUpdateException extends GlobalDomainException {
    /**
     *
     * @param message 内部错误信息，外部信息使用GlobalErrorCode.CONCURRENT_UPDATE.clientMessage
     */
    public ConcurrentUpdateException(String message) {
        super(GlobalErrorCode.CONCURRENT_UPDATE, message);
    }
}

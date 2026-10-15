package com.ovelin.mall.common.exception.global;
import com.ovelin.mall.common.exception.DomainException;
import com.ovelin.mall.common.exception.global.face.GlobalDomainException;
import com.ovelin.mall.common.exception.global.face.GlobalErrorCode;
import lombok.Getter;

@Getter
public class InvalidStatusException extends GlobalDomainException {
    ;
    public InvalidStatusException(String message) {
        super(GlobalErrorCode.INVALID_STATUS, message);
    }

}

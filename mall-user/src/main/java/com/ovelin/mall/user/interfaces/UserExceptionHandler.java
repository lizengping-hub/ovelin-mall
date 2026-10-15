package com.ovelin.mall.user.interfaces;

import com.ovelin.mall.common.web.starter.exceptionHandler.ProblemBody;
import com.ovelin.mall.user.domain.exception.face.UserErrorCode;
import com.ovelin.mall.user.domain.exception.face.UserDomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.ovelin.mall.common.web.starter.exceptionHandler.DomainExceptionLogger.logByStatus;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(UserExceptionHandler.class);

    @ExceptionHandler(UserDomainException.class)
    public ResponseEntity<ProblemDetail> handle(UserDomainException e, HttpServletRequest req) {
        UserErrorCode errorCode = e.getErrorCode();
        HttpStatus status = mapStatus(errorCode);
        logByStatus(log, status, e, req);
        return ResponseEntity.status(status)
                .body(ProblemBody.problem(status, errorCode.name(), message(errorCode)));
    }

    private HttpStatus mapStatus(UserErrorCode code) {
        return switch (code) {
            case USER_NOT_FOUND      -> HttpStatus.NOT_FOUND;     // 404
            case INVALID_USER_STATUS, USER_CONCURRENT_UPDATE -> HttpStatus.CONFLICT;      // 409
        };
    }

    private String message(UserErrorCode code){
        return switch (code) {
            case USER_NOT_FOUND      -> "用户不存在";     // 404
            case INVALID_USER_STATUS ->"当前用户状态不允许该操作";
            case USER_CONCURRENT_UPDATE -> "用户数据已被修改，请刷新后重试";      // 409
        };
    }
}
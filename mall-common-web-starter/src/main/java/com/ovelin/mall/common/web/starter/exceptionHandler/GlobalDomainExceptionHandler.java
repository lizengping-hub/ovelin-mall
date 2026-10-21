package com.ovelin.mall.common.web.starter.exceptionHandler;

import com.ovelin.mall.common.exception.DomainException;
import com.ovelin.mall.common.exception.ErrorCode;
import com.ovelin.mall.common.exception.global.face.GlobalDomainException;
import com.ovelin.mall.common.exception.global.face.GlobalErrorCode;
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
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class GlobalDomainExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(GlobalDomainExceptionHandler.class);


    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> domainExceptionHandle(DomainException e, HttpServletRequest req) {
        ErrorCode errorCode = e.getErrorCode();
        logByStatus(log, HttpStatus.INTERNAL_SERVER_ERROR, e, req);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProblemBody.problem(HttpStatus.INTERNAL_SERVER_ERROR, errorCode.name(), "服务器内部错误"));
    }

    @ExceptionHandler(GlobalDomainException.class)
    public ResponseEntity<ProblemDetail> globalDomainExceptionHandle(GlobalDomainException e, HttpServletRequest req) {
        GlobalErrorCode errorCode = e.getErrorCode();
        HttpStatus status = mapStatus(errorCode);
        logByStatus(log, status, e, req);
        return ResponseEntity.status(status)
                .body(ProblemBody.problem(status, errorCode.name(),
                        message(errorCode, e)));
    }

    private HttpStatus mapStatus(GlobalErrorCode code) {
        return switch (code) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;   // 400
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;     // 404
            case INVALID_STATUS, CONCURRENT_UPDATE -> HttpStatus.CONFLICT;      // 409
        };
    }
    private String message(GlobalErrorCode code, GlobalDomainException e){
        return switch (code) {
            case RESOURCE_NOT_FOUND -> "访问的资源不存在";     // 404
            case INVALID_STATUS -> e.getMessage();
            case CONCURRENT_UPDATE -> "数据已被修改，请刷新后重试";
            case INVALID_ARGUMENT -> e.getMessage();
        };
    }

}

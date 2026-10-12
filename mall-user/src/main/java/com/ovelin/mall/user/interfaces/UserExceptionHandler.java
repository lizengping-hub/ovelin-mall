package com.ovelin.mall.user.interfaces;

import com.ovelin.mall.common.web.starter.exceptionHandler.ProblemBody;
import com.ovelin.mall.user.domain.exception.ErrorCode;
import com.ovelin.mall.user.domain.exception.UserDomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(UserExceptionHandler.class);
    @ExceptionHandler(UserDomainException.class)
    public ResponseEntity<ProblemDetail> handle(UserDomainException e, HttpServletRequest req) {
        HttpStatus status = mapStatus(e.getErrorCode());
        logByStatus(status, e, req);
        return ResponseEntity.status(status)
                .body(ProblemBody.problem(status, e.getErrorCode().name(), e.getClientMessage()));
    }
    private HttpStatus mapStatus(ErrorCode code) {
        return switch (code) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;   // 400
            case USER_NOT_FOUND      -> HttpStatus.NOT_FOUND;     // 404
            case INVALID_USER_STATUS -> HttpStatus.CONFLICT;      // 409
            case CONCURRENT_UPDATE   -> HttpStatus.CONFLICT;      // 409
        };
    }
    static void logByStatus(HttpStatusCode status, Exception e, HttpServletRequest req) {
        if (status.is5xxServerError()) {
            log.error("{} {} -> {}", req.getMethod(), req.getRequestURI(), status.value(), e);  // 带堆栈
        } else {
            log.warn("{} {} -> {}: {}", req.getMethod(), req.getRequestURI(), status.value(), e.getMessage());
        }
    }
}
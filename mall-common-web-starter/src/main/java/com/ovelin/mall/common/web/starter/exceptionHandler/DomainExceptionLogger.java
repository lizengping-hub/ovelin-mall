package com.ovelin.mall.common.web.starter.exceptionHandler;

import com.ovelin.mall.common.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.context.request.WebRequest;

public class DomainExceptionLogger {

    public static void logByStatus(Logger log, HttpStatusCode status, DomainException e, HttpServletRequest req) {
        if (status.is5xxServerError()) {
            log.error("{} {} -> {}: {}", req.getMethod(), req.getRequestURI(), status.value(), e.getErrorCode().name(), e);  // 带堆栈
        } else {
            log.warn("{} {} -> {}: {}: {}", req.getMethod(), req.getRequestURI(), status.value(), e.getErrorCode().name(), e.getMessage());
        }
    }
    public static void logByStatus(Logger log, HttpStatusCode status, Exception e,  String code, HttpServletRequest req) {
        if (status.is5xxServerError()) {
            log.error("{} {} -> {}: {}", req.getMethod(), req.getRequestURI(), status.value(), code, e);  // 带堆栈
        } else {
            log.warn("{} {} -> {}: {}: {}", req.getMethod(), req.getRequestURI(), status.value(),code, e.getMessage());
        }
    }
    public static void logByStatus(Logger log, HttpStatusCode status, Exception e, String code, WebRequest request){
        if (status.is5xxServerError()) {
            log.error("{} -> {}: {}", request.getDescription(false), status.value(), code, e);
        } else {
            log.warn("{} -> {}: {}: {}", request.getDescription(false), status.value(), code, e.getMessage());
        }
    }
}

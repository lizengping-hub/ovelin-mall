package com.ovelin.mall.common.web.starter.exceptionHandler;


import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 兜底：未预期的异常，记日志，对外不暴露细节
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e, HttpServletRequest req) {
        log.error("Unhandled exception: {} {}", req.getMethod(), req.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProblemBody.problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", detailOf(HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    /**
     * 校验失败：带上字段信息
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return handleExceptionInternal(ex, ProblemBody.problem(status, "INVALID_ARGUMENT", msg), headers, status, request);
    }

    private static String detailOf(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "请求参数错误";
            case 404 -> "资源不存在";
            case 405 -> "请求方法不支持";
            case 415 -> "不支持的内容类型";
            default -> status.is5xxServerError() ? "服务器内部错误" : "请求错误";
        };
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        if (status.is5xxServerError()) {
            log.error("{} -> {}", request.getDescription(false), status.value(), ex);
        } else {
            log.warn("{} -> {}: {}", request.getDescription(false), status.value(), ex.getMessage());
        }
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        if (body instanceof ProblemDetail pd) {
            if (pd.getProperties() == null || !pd.getProperties().containsKey("code")) {
                pd.setProperty("code", codeOf(status));
            }

        } else {
            body = ProblemBody.problem(status, codeOf(status), detailOf(status));
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    private static String codeOf(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "INVALID_ARGUMENT";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> null;
        };
    }
}
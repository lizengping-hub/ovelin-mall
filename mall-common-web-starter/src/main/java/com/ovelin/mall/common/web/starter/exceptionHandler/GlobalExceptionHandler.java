package com.ovelin.mall.common.web.starter.exceptionHandler;


import com.ovelin.mall.common.exception.DomainException;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

import java.util.stream.Collectors;

import static com.ovelin.mall.common.web.starter.exceptionHandler.DomainExceptionLogger.logByStatus;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 兜底：未预期的异常，记日志，对外不暴露细节
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e, HttpServletRequest req) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        logByStatus(log, status, e, "INTERNAL_ERROR", req);
        return ResponseEntity.status(status)
                .body(ProblemBody.problem(status, "INTERNAL_ERROR", detailOf(status)));
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
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletResponse response = servletWebRequest.getResponse();
            if (response != null && response.isCommitted()) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Response already committed. Ignoring: " + ex);
                }
                return null;
            }
        }

        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.updateAndGetBody(super.getMessageSource(), LocaleContextHolder.getLocale());
        }
        if (status.equals(HttpStatus.INTERNAL_SERVER_ERROR) && body == null) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }
        String code = codeOf(status);
        if (body instanceof ProblemDetail pd) {
            if (pd.getProperties() == null || !pd.getProperties().containsKey("code")) {
                pd.setProperty("code", code);
            }
        }
        if (body == null){
            body = ProblemBody.problem(status, code, detailOf(status));
        }

        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest httpRequest = servletWebRequest.getRequest();
            logByStatus(log, status, ex, code, httpRequest);
        } else {
            logByStatus(log, status, ex, code, request);
        }
        return createResponseEntity(body, headers, status, request);
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
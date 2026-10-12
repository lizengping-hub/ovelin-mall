package com.ovelin.mall.common.web.starter.exceptionHandler;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(100)   // 模拟业务模块的 advice，必须排在 Global 前面
class TestBusinessAdvice {
    @ExceptionHandler(ProbeController.BusinessConflictException.class)
    ResponseEntity<ProblemDetail> handle(ProbeController.BusinessConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ProblemBody.problem(HttpStatus.CONFLICT, "CONFLICT", "冲突"));
    }
}
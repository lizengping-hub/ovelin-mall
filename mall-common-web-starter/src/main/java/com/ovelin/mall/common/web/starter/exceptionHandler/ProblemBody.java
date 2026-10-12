package com.ovelin.mall.common.web.starter.exceptionHandler;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public class ProblemBody {
    public static ProblemDetail problem(HttpStatusCode status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);          // 序列化后和 detail 同级
        return pd;
    }
}

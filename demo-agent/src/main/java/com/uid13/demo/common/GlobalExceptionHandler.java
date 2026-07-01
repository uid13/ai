package com.uid13.demo.common;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * 统一捕获 Controller 层抛出的异常，返回 COLA 标准响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Response handleBizException(BizException e) {
        log.warn("[业务异常] code={}, message={}", e.getErrCode(), e.getMessage());
        Response response = new Response();
        response.setSuccess(false);
        response.setErrCode(e.getErrCode());
        response.setErrMessage(e.getMessage());
        return response;
    }

    @ExceptionHandler(SysException.class)
    public Response handleSysException(SysException e) {
        log.error("[系统异常] code={}, message={}", e.getErrCode(), e.getMessage(), e);
        Response response = new Response();
        response.setSuccess(false);
        response.setErrCode(e.getErrCode());
        response.setErrMessage(e.getMessage());
        return response;
    }

    @ExceptionHandler(Exception.class)
    public Response handleException(Exception e) {
        log.error("[系统异常]", e);
        Response response = new Response();
        response.setSuccess(false);
        response.setErrCode("500");
        response.setErrMessage("系统内部错误：" + e.getMessage());
        return response;
    }
}

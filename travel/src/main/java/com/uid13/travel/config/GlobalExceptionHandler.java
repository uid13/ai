package com.uid13.travel.config;

import com.alibaba.cola.dto.SingleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一处理控制器层抛出的异常，返回标准化错误响应
 * 在多 Agent 编排场景下，捕获 Agent 执行过程中的异常并返回友好提示
 *
 * @author uid13
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.uid13.travel.controller")
public class GlobalExceptionHandler {

    /**
     * 处理 GraphRunnerException（Agent 执行异常）
     *
     * @param e 异常对象
     * @return 标准化错误响应
     */
    @ExceptionHandler(com.alibaba.cloud.ai.graph.exception.GraphRunnerException.class)
    public SingleResponse<Object> handleGraphRunnerException(com.alibaba.cloud.ai.graph.exception.GraphRunnerException e) {
        log.error("Agent 执行失败", e);
        return SingleResponse.buildFailure("AGENT_ERROR", "Agent 执行失败：" + e.getMessage());
    }

    /**
     * 处理 IllegalArgumentException（参数校验异常）
     *
     * @param e 异常对象
     * @return 标准化错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public SingleResponse<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数校验失败：{}", e.getMessage());
        return SingleResponse.buildFailure("PARAM_ERROR", e.getMessage());
    }

    /**
     * 处理 RuntimeException（运行时异常）
     *
     * @param e 异常对象
     * @return 标准化错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public SingleResponse<Object> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return SingleResponse.buildFailure("SYSTEM_ERROR", "系统内部错误：" + e.getMessage());
    }

    /**
     * 处理 Exception（其他异常）
     *
     * @param e 异常对象
     * @return 标准化错误响应
     */
    @ExceptionHandler(Exception.class)
    public SingleResponse<Object> handleException(Exception e) {
        log.error("未预期的异常", e);
        return SingleResponse.buildFailure("SYSTEM_ERROR", "系统内部错误，请稍后重试");
    }
}

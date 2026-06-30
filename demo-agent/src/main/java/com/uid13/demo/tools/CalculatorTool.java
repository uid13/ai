package com.uid13.demo.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 计算器工具 - 供 CalculatorAgent 使用
 */
@Slf4j
@Component
public class CalculatorTool {

    @Tool(description = "根据源币种、目标币种和金额进行汇率换算，支持 CNY、USD、EUR")
    public String currencyExchange(
            @ToolParam(description = "源币种代码，例如：CNY（人民币）、USD（美元）、EUR（欧元）") String from,
            @ToolParam(description = "目标币种代码，例如：CNY、USD、EUR") String to,
            @ToolParam(description = "需要换算的金额，例如：100") double amount) {

        log.debug("[CalculatorTool] 汇率换算：{} {} -> {}", amount, from, to);

        double cnyRate = switch (from.toUpperCase()) {
            case "CNY" -> 1.0;
            case "USD" -> 7.25;
            case "EUR" -> 7.85;
            default -> throw new IllegalArgumentException("不支持的币种：" + from);
        };

        double targetRate = switch (to.toUpperCase()) {
            case "CNY" -> 1.0;
            case "USD" -> 7.25;
            case "EUR" -> 7.85;
            default -> throw new IllegalArgumentException("不支持的币种：" + to);
        };

        double amountInCny = amount * cnyRate;
        double result = amountInCny / targetRate;

        return String.format("%.2f %s = %.2f %s（参考汇率）", amount, from, result, to);
    }

    @Tool(description = "执行两个数的四则运算，支持加减乘除")
    public String calculate(
            @ToolParam(description = "第一个数字") double a,
            @ToolParam(description = "第二个数字") double b,
            @ToolParam(description = "运算符：+（加）、-（减）、*（乘）、/（除）") String operator) {

        log.debug("[CalculatorTool] 计算：{} {} {}", a, operator, b);

        double result = switch (operator) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> {
                if (b == 0) throw new ArithmeticException("除数不能为 0");
                yield a / b;
            }
            default -> throw new IllegalArgumentException("不支持的运算符：" + operator);
        };

        return String.format("%.2f %s %.2f = %.2f", a, operator, b, result);
    }
}

package com.myself.springai.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 演示定义工具的第二种方式：FunctionToolCallback（编程式 / 函数式）。
 *
 * 与 @Tool 注解方式不同，这里不要求工具是一个 Spring Bean 上的方法，
 * 而是直接用一个 Lambda 表达式 / 函数对象来构造工具。
 *
 * 适用场景：当工具逻辑很简单、或者想脱离 Spring Bean 直接定义一个函数时，
 * 可以用 FunctionToolCallback 快速构造一个工具。
 */
@Configuration
public class ToolCallbackConfig {

    /**
     * 工具入参的 Java 类型。
     * FunctionToolCallback 会根据这个类型自动生成对应的 JSON Schema，
     * 用于告诉大模型调用该工具时需要传哪些参数。
     */
    public record RandomNumberRequest(int min, int max) {}

    /**
     * 工具：在指定范围内生成一个随机整数。
     */
    @Bean
    public ToolCallback randomNumberTool() {
        return FunctionToolCallback
                // 第一个参数是工具名称，第二个参数是具体的函数实现（一个 Lambda）
                .builder("randomNumber", (RandomNumberRequest request) -> {
                    int value = ThreadLocalRandom.current().nextInt(request.min(), request.max() + 1);
                    return "在 [" + request.min() + ", " + request.max() + "] 之间的随机整数是：" + value;
                })
                // 工具描述，帮助大模型理解何时应该使用该工具
                .description("在指定范围内生成一个随机整数，min 为最小值，max 为最大值")
                // 入参类型，Spring AI 会根据它生成 JSON Schema
                .inputType(RandomNumberRequest.class)
                .build();
    }
}

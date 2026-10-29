package com.myself.springai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 工具类：定义大模型可以调用的“工具”（本质上是普通的 Java 方法）。
 *
 * 在 Spring AI 2 中，只要给一个普通方法加上 @Tool 注解，就可以把它暴露给大模型：
 * 大模型在回答问题的过程中，如果判断需要用到这些工具，就会返回一个“工具调用”请求，
 * Spring AI 会代为执行对应的 Java 方法，再把执行结果回传给大模型，让大模型基于结果作答。
 *
 * 这里把这些方法放在 @Component 组件里，之后通过 ChatClient.Builder 的
 * defaultTools(...) 方法把整个对象注册进去即可。
 */
@Component
public class Tools {

    // 模拟订单数据库
    private static final Map<String, String> ORDERS = Map.of(
            "A1001", "已发货，顺丰 SF1234567890",
            "A1002", "待支付，30 分钟后自动取消"
    );

    /**
     * 工具一：根据订单号查询订单状态和物流信息（单参数工具）。
     */
    @Tool(description = "根据订单号查询订单状态和物流信息")
    public String getOrderStatus(@ToolParam(description = "订单号，格式如 A1001") String orderId) {
        return ORDERS.getOrDefault(orderId, "订单号不存在");
    }

    /**
     * 工具二：查询天气（多参数工具）。
     */
    @Tool(description = "查询指定城市的天气，温度单位可选摄氏度或华氏度")
    public String getWeather(
            @ToolParam(description = "城市名称，例如：北京") String city,
            @ToolParam(description = "温度单位，celsius 表示摄氏度，fahrenheit 表示华氏度") String unit) {
        // 这里用模拟数据演示，实际项目中可以替换为调用真实的天气服务 API
        String temperature = "celsius".equalsIgnoreCase(unit) ? "25℃" : "77℉";
        return city + "今天晴，气温 " + temperature + "，适合出行";
    }

    /**
     * 工具三：获取当前时间（无参数工具）。
     */
    @Tool(description = "获取当前的日期和时间")
    public String getCurrentTime() {
        return "当前时间为：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

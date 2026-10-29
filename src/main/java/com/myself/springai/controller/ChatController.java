package com.myself.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class ChatController {

    // ChatClient 是 Spring AI 提供的客户端对象，
    // 我们可以通过它向大模型发送问题，并获取大模型生成的回答。
    private final ChatClient chatClient;

    // 构造方法注入：
    // Spring AI 自动配置会提供 ChatClient.Builder 类型的 Bean，
    // Spring 容器创建 ChatController 时，会自动把 Builder 注入进来。
    //
    // Builder 可以理解为 ChatClient 的构建器，
    // 通过 build() 方法创建一个可以实际使用的 ChatClient 对象。
    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // 参数 prompt 用于接收用户输入的问题。
    // 例如：用户请求 /ai/chat?prompt=你好
    // Spring MVC 会将请求参数 prompt 的值绑定到这个方法参数上。

    // 访问：http://localhost:8080/ai/chat?prompt=你好
    @RequestMapping("/chat")
    public String chat(String prompt) {

        // prompt()：开始构建一次大模型对话请求。
        return chatClient.prompt()

                // user(prompt)：将用户输入的问题设置为本次对话的用户消息。
                // 例如，prompt 的值是“你好”，就会把“你好”发送给大模型。
                .user(prompt)

                // call()：发起同步调用，向配置好的大模型服务发送请求，
                // 并等待模型返回结果。
                .call()

                // content()：从调用结果中提取模型生成的文本内容。
                // 最终返回一个 String，例如“你好！有什么可以帮你？”
                .content();
    }
}

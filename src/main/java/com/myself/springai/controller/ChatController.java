package com.myself.springai.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private static final Logger log = LogManager.getLogger(ChatController.class);
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

    @RequestMapping("/chat")
    public String chat(String prompt) {
        log.info("/chat，参数：{}", prompt);

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

    // text/html	响应内容的媒体类型，表示 HTML 文本
    // charset=UTF-8	声明文本采用 UTF-8 字符编码
    // 响应头 Content-Type: text/html;charset=UTF-8
    @RequestMapping(value = "/stream", produces = "text/html;charset=UTF-8")
    public Flux<String> stream(String prompt) {

        // prompt()：开始构建一次大模型对话请求。
        return chatClient.prompt()

                // user(prompt)：设置本次请求的用户消息，即用户输入的问题。
                .user(prompt)

                // stream()：以流式方式发起大模型调用。
                // 与 call() 不同，stream() 不需要等到模型生成完整回答后才返回，
                // 而是可以随着模型不断生成内容，逐段返回响应数据。
                .stream()

                // content()：提取流式响应中的文本内容。
                // 返回 Flux<String>，表示一个会陆续产生多个字符串片段的响应流。
                .content();
    }

    @RequestMapping("/issue")
    public String issue(@RequestParam String issue) {
        log.info("/issue，参数：{}", issue);
        return chatClient.prompt()
                .system("你是一位资深社会科学家，可以对用户提出的社会现象做出自己的解释")   // 系统人设
                .user(u -> u.text("你来说说为什么会有{issue}，可以给出更多的例子来支持你的观点吗？" +
                                "不要使用markdown格式进行回答，不要包含*#-等markdown格式，可以进行换行或者空格使得格式美观")
                        .param("issue", issue))           // 模板参数
                .call()
                .content();
    }

    // 先定义记录类，record 应该是一个语法糖
    public record BookInfo(String title, String author, double price, List<String> tags) {}

    @RequestMapping("/book")
    public BookInfo book(@RequestParam String keyword) {
        return chatClient.prompt()
                .user("推荐一本关于" + keyword + "的经典书，返回书名、作者、大致价格和3个标签")
                .call()
                .entity(BookInfo.class);    // 直接映射为 Java 对象并返回给前端
    }
}

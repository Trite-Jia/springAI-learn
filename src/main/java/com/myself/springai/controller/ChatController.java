package com.myself.springai.controller;

import com.myself.springai.tools.Tools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private static final Logger log = LogManager.getLogger(ChatController.class);

    // ChatClient 是 Spring AI 提供的客户端对象，
    // 我们可以通过它向大模型发送问题，并获取大模型生成的回答。
    private final ChatClient chatClient;

    // 保存工具对象，方便在 /ai/tools/definitions 接口中查看工具定义。
    private final Tools tools;
    private final ToolCallback randomNumberTool;

    // 构造方法注入：
    // - ChatClient.Builder：Spring AI 自动配置提供的 ChatClient 构建器；
    // - Tools：我们自定义的 @Tool 注解工具类；
    // - ToolCallback：用 FunctionToolCallback 定义的工具 Bean。
    public ChatController(ChatClient.Builder builder, Tools tools, ToolCallback randomNumberTool) {
        this.tools = tools;
        this.randomNumberTool = randomNumberTool;

        this.chatClient = builder
                // defaultTools(...) 是 Spring AI 2 中统一的工具注册方法，
                // 既能注册带 @Tool 注解的对象，也能注册 ToolCallback（如 FunctionToolCallback）。
                // 注意：defaultToolCallbacks(...) 在 2.0 中已废弃，统一使用 defaultTools(...)。
                .defaultTools(tools, randomNumberTool)
                .build();
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
                .user(prompt)

                // call()：发起同步调用，并等待模型返回结果。
                .call()

                // content()：从调用结果中提取模型生成的文本内容。
                .content();
    }

    // text/html	响应内容的媒体类型，表示 HTML 文本
    // charset=UTF-8	声明文本采用 UTF-8 字符编码
    // 响应头 Content-Type: text/html;charset=UTF-8
    @RequestMapping(value = "/stream", produces = "text/html;charset=UTF-8")
    public Flux<String> stream(String prompt) {

        // prompt()：开始构建一次大模型对话请求。
        return chatClient.prompt()

                // user(prompt)：设置本次请求的用户消息。
                .user(prompt)

                // stream()：以流式方式发起大模型调用，逐段返回响应数据。
                .stream()

                // content()：提取流式响应中的文本内容。
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

    /**
     * 工具调用演示接口。
     *
     * 用户用自然语言提问，大模型会自行判断是否需要调用已注册的工具。
     * 例如：
     *   - “帮我查一下订单 A1001 的状态”
     *   - “北京今天天气怎么样？用摄氏度”
     *   - “现在几点了？”
     *   - “生成一个 1 到 100 之间的随机整数”
     */
    @GetMapping("/tools")
    public String agent(@RequestParam String query) {
        log.info("/tools，参数：{}", query);
        return chatClient.prompt()
                .user(query)
                .call()
                .content();
    }

    /**
     * 查看已注册工具的元信息（名称、描述、JSON Schema）。
     * 通过它可以直接看到 Spring AI 为每个工具生成的 JSON Schema，
     * 帮助理解“Java 方法是如何被描述给大模型”的。
     */
    @GetMapping("/tools/definitions")
    public List<ToolInfo> definitions() {
        // 把注解方式定义的 @Tool 方法转换为 MethodToolCallback，
        // 再与函数式方式定义的 FunctionToolCallback 合并。
        return Stream.concat(
                        Arrays.stream(MethodToolCallbackProvider.builder()
                                .toolObjects(tools)
                                .build()
                                .getToolCallbacks()),
                        Stream.of(randomNumberTool))
                .map(callback -> {
                    ToolDefinition def = callback.getToolDefinition();
                    return new ToolInfo(def.name(), def.description(), def.inputSchema());
                })
                .toList();
    }

    // 返回给前端的工具信息
    public record ToolInfo(String name, String description, String inputSchema) {}
}

# Spring AI Learning

一个用于学习 Spring AI 2 的 Java 项目，通过多个版本逐步探索 Spring AI 的核心功能，并记录每个版本的实现代码、配置方式和相关知识。

## 版本记录

### v0.1：初识 Spring AI，实现基础大模型对话

**本版本目标：** 完成 Spring Boot 与大模型 API 的基础对接，实现一个最简单的 AI 问答接口。

**实现功能：**

- 配置 DeepSeek 模型 API。
- 使用 Spring AI 自动配置的 `ChatClient.Builder` 创建 `ChatClient`。
- 通过 `ChatClient` 向大模型发送用户消息。
- 获取大模型生成的文本并返回给客户端。
- 配置 Spring AI 相关日志，方便后续学习和调试。

**核心调用链：**

```text
用户请求
   ↓
ChatController
   ↓
ChatClient
   ↓
prompt()
   ↓
user(prompt)
   ↓
call()
   ↓
content()
   ↓
返回大模型生成的文本
```

**一、技术栈**

- Java
- Spring Boot
- Spring AI
- Maven
- Spring Web

具体依赖版本以项目 `pom.xml` 中的配置为准。

**二、配置说明**

在 `application.yml` 中配置大模型 API：

```yaml
spring:
  application:
    name: springAI

  ai:
    openai:
      # DeepSeek 官方 API 地址
      base-url: https://api.deepseek.com

      # 从环境变量读取 API Key
      api-key: ${DEEPSEEK_API_KEY}

      chat:
        options:
          model: deepseek-flash
          temperature: 0.7

    chat:
      client:
        enabled: true

logging:
  level:
    org.springframework.ai.chat.client.advisor: debug
    com.myself.springai: debug
```

> 注意：以上配置按照项目当前采用的 Spring AI OpenAI 兼容接口配置方式编写。不同 Spring AI 版本的配置属性可能存在差异，请以当前项目的依赖版本和实际配置为准。

**配置 API Key**

运行项目前，需要在本地设置变量 `DEEPSEEK_API_KEY`，并填入自己在 DeepSeek 平台创建的 API Key。

不要将真实密钥写入公开仓库。

**三、核心代码**

本版本通过 `ChatController` 提供基础问答接口。

```java
@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @RequestMapping("/chat")
    public String chat(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
```

**核心 API 说明**

| API | 作用 |
|---|---|
| `ChatClient.Builder` | 用于构建 `ChatClient`，由 Spring AI 自动配置提供 |
| `build()` | 创建 `ChatClient` 实例 |
| `prompt()` | 开始构建一次对话请求 |
| `user(prompt)` | 设置本次请求的用户消息 |
| `call()` | 发起同步调用并获取模型响应 |
| `content()` | 提取模型生成的文本内容 |

**调用过程**

1. 客户端向 `/ai/chat` 发送请求，并传入 `prompt` 参数。
2. `ChatController` 调用 `ChatClient` 构建对话请求。
3. Spring AI 将请求交给对应的模型实现。
4. 模型实现调用 DeepSeek API，并获取模型响应。
5. `content()` 提取生成的文本，作为接口响应返回。

**四、运行与测试**

启动 Spring Boot 应用，访问以下地址：

```text
http://localhost:8080/ai/chat?prompt=你好
```

如果 API Key、模型名称、网络连接及相关配置正确，接口将返回大模型生成的回答。

可以尝试更换 `prompt` 参数，观察模型对不同问题的回答。

本版本只实现了最基础的同步问答，还没有引入对话历史、持久化存储、工具调用、RAG 等能力。


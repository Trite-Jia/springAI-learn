# Spring AI Learning

一个用于学习 Spring AI 2 的 Java 项目，通过多个版本逐步探索 Spring AI 的核心功能，并记录每个版本的实现代码、配置方式和相关知识。 
下面是各个版本的代码，每个版本都会新增一些功能可供学习。

# v0.1：初识 Spring AI，实现基础大模型对话

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

### 一、技术栈

- Java
- Spring Boot
- Spring AI
- Maven
- Spring Web

具体依赖版本以项目 `pom.xml` 中的配置为准。

### 二、配置说明

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

### 三、核心代码

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

### 四、运行与测试

启动 Spring Boot 应用，访问以下地址：

```text
http://localhost:8080/ai/chat?prompt=你好
```

如果 API Key、模型名称、网络连接及相关配置正确，接口将返回大模型生成的回答。

可以尝试更换 `prompt` 参数，观察模型对不同问题的回答。

本版本只实现了最基础的同步问答，还没有引入对话历史、持久化存储、工具调用、RAG 等能力。

# v0.2：流式输出、提示词模板与结构化输出

本版本在 v0.1 基础问答功能的基础上，进一步学习 Spring AI 的三项能力：

- **流式输出（Streaming）**：通过 `stream()` 逐步获取大模型生成的文本。
- **提示词定制与模板参数（System Prompt & Prompt Template）**：通过 `system()` 设置模型的角色，通过 `param()` 动态填充提示词模板。
- **结构化输出（Structured Output）**：通过 `entity()` 将大模型生成的内容转换为指定的 Java 对象。

本版本重点学习 Spring AI 的链式 API，以及这些 API 背后的请求构建、响应处理和对象转换机制。

---

## 一、流式输出：stream()

### 1.1 功能介绍

在 v0.1 中，我们使用以下代码调用大模型：

```java
return chatClient.prompt()
        .user(prompt)
        .call()
        .content();
```

其中，`call()` 使用同步阻塞调用方式，`content()` 获取模型生成的完整文本。

例如，用户询问：

> 请介绍一下 Java 的发展历史。

模型可能需要几秒钟才能生成完整回答。使用 `call().content()` 时，业务代码需要等待调用完成，才能获得一个完整的AI回答的字符串。

流式输出则允许应用在模型生成回答的过程中，逐步接收文本片段，并不断返回给前端，提高用户体验。

本版本新增接口：

```java
@RequestMapping(
        value = "/stream",
        produces = "text/html;charset=UTF-8"
)
public Flux<String> stream(String prompt) {

    // 开始构建本次大模型请求
    return chatClient.prompt()

            // 设置用户消息
            .user(prompt)

            // 发起流式调用
            .stream()

            // 获取流式响应中的文本片段
            .content();
}
```

访问示例：

```text
http://localhost:8080/ai/stream?prompt=你好
```

需要注意，`Flux<String>` 表示一个可以陆续产生多个字符串的响应式数据流。它不代表一个完整的字符串，而是由多个文本片段组成的序列。

### 1.2 call() 与 stream() 的区别

call() 方法：同步阻塞，一次交付

- 行为：调用 call() 后，当前线程会阻塞（暂停执行），直到 AI 模型生成完整的响应后才返回。你可以把它想象成打电话，必须等对方把话全部说完，你才能挂断并开始处理信息。
- 返回值：通常是一个完整的字符串（String）或一个包含完整响应的 ChatResponse 对象。
- 适用场景：适合后台处理、任务型交互等不需要实时展示给用户的场景，代码逻辑简单直观。

stream() 方法：异步非阻塞，流式交付

- 行为：调用 stream() 后会立即返回一个 Flux<String>（Project Reactor 的响应式流类型），调用线程不会阻塞，可以继续执行其他任务。AI 模型每生成一小段内容（一个 token），就会通过 Flux 推送一次。这就像看直播，内容边产生边播放，而无需等整场活动结束。
- 返回值：Flux<String>，代表一个异步的、包含 0 到 N 个元素的数据流。
- 适用场景：需要实时、逐字显示效果的场景，如聊天界面、长文本生成等，能极大提升用户体验。


这里需要区分两个概念：**同步调用不等于阻塞整个应用，流式调用也不等于模型一定逐字返回。**

> 同步 vs. 异步：关注“结果如何返回”
> - 同步：调用者需要主动等待结果。在结果就绪前，调用者无法进行下一步（或需要不断轮询）。call() 是典型的同步操作。
> - 异步：调用者发出请求后立即返回，不等待结果。当结果准备好时，会通过回调、通知等方式告知调用者。stream() 是异步的，它返回 Flux 后，后续数据通过推送到达。

> 阻塞 vs. 非阻塞：关注“调用线程的状态”
> - 阻塞：在等待结果期间，调用线程被挂起，无法执行任何其他任务。call() 会阻塞调用它的线程。
> - 非阻塞：调用发出后，调用线程不被挂起，可以继续执行其他逻辑。stream() 是非阻塞的，它立即返回 Flux，调用线程得以释放。

> 流式 (Streaming)：一种数据传输模式
> - 流式并非严格的对立概念，而是描述数据像水流一样，一小份一小份地、连续地产生和传输。它天然与异步、非阻塞模型结合，以实现实时性。stream() 方法返回的 Flux 就是实现流式传输的载体。

### stream() 背后的实现机制

关键结论：

> **流式输出是 AI 模型服务商 API 的原生能力，Spring AI 负责开启这个能力并把数据搬运成 `Flux`。**

Spring AI 本身并不“生产”流式效果。它主要做两件事：

1. **开启流式开关**：向 AI 服务端发送请求时，把请求参数设置为 `stream=true`。
2. **搬运流式数据**：使用响应式 HTTP 客户端接收 AI 服务端持续返回的数据块，并适配成 Java 程序员熟悉的 `Flux`。

没有 AI 接口本身对流式的支持，Spring AI 的 `stream()` 方法无从谈起。

---

#### Spring AI 源码层面的处理过程

**1. 请求参数中的流式开关**

以 OpenAI 兼容接口为例，Spring AI 在 `stream()` 调用路径中构建请求对象时，会把 `stream` 字段设置为 `true`。

概念示意：

```java
// OpenAiChatModel.stream() 内部逻辑示意
ChatCompletionRequest request = createRequest(prompt, true);
```

其中第二个参数 `true` 表示开启流式。

在 DashScope 的实现中，`DashScopeChatModel.stream()` 内部也会强制以 `stream=true` 构建请求。

这个 `stream=true` 最终会体现在发给 AI 服务端的 HTTP 请求体中。

**2. HTTP 客户端从 RestClient 切换为 WebClient**

在 `call()` 同步调用中，Spring AI 通常使用 `RestClient` 一次性接收完整的 JSON 响应。

在 `stream()` 流式调用中，它会切换到响应式的 `WebClient`，以**服务端发送事件（Server-Sent Events，SSE）** 的方式持续接收响应流。

`WebClient` 接收到的是一个个 SSE 事件，每个事件里通常包含模型刚刚生成的一小段文本，也就是“增量块”（delta chunk）。

**3. 将 SSE 事件流适配为 Flux**

`WebClient` 拿到原始 SSE 事件后，Spring AI 会通过一系列映射和过滤操作，把每个事件中的文本片段提取出来，包装成 `ChatResponse`，再放入 `Flux` 中发射。

最终开发者拿到的就是：

```java
Flux<String>
```

或：

```java
Flux<ChatResponse>
```

这就是“边生成边推送”效果在 Spring AI 中的落地方式。

---

#### 发出的 HTTP 请求概念示意

```http
POST /v1/chat/completions
Content-Type: application/json

{
  "model": "gpt-4o",
  "messages": [
    {
      "role": "user",
      "content": "你好"
    }
  ],
  "stream": true
}
```

其中 `stream: true` 是关键开关，由 Spring AI 在流式调用路径中注入。

#### AI 服务端返回的流式响应概念示意

```http
HTTP/1.1 200 OK
Content-Type: text/event-stream

data: {"choices":[{"delta":{"content":"你"}}]}
data: {"choices":[{"delta":{"content":"好"}}]}
data: {"choices":[{"delta":{"content":"！"}}]}
data: [DONE]
```

Spring AI 的 `WebClient` 接收到这些 `data:` 行，解析其中的 JSON，提取出 `"你"`、`"好"`、`"！"` 等文本片段，然后依次通过 `Flux` 发射出去。

---

### produces 的作用

本版本的接口使用：

```java
produces = "text/html;charset=UTF-8"
```

它声明了响应的媒体类型和字符编码。

- `text/html`：表示响应内容采用 HTML 媒体类型。
- `charset=UTF-8`：声明文本采用 UTF-8 编码。
- `Content-Type`：HTTP 响应头中用于描述响应内容类型的字段。

Spring 会根据接口声明和实际响应处理机制生成相应的响应头。

这里的 `produces` **不是开启流式输出的开关**。流式调用由 `stream()` 实现，而 `produces` 主要影响媒体类型匹配与响应写出。

如果后续需要实现标准的 Server-Sent Events（SSE），可以进一步研究 `text/event-stream`、SSE 事件格式以及前端的事件流读取机制。`text/html` 和 `text/event-stream` 不是同一种协议。

---

## 二、System Prompt 与模板参数

### 2.1 功能介绍

在最基础的问答中，我们只通过 `.user(prompt)` 设置用户问题。

但是，实际应用往往需要控制模型的回答风格、身份和任务要求。

例如，希望大模型以社会科学研究者的身份，分析某种社会现象，并使用更多案例解释自己的观点。

本版本新增接口：

```java
@RequestMapping("/issue")
public String issue(@RequestParam String issue) {

    log.info("/issue，参数：{}", issue);

    return chatClient.prompt()

            // 设置系统提示词，定义模型在本次对话中的角色和行为要求
            .system("你是一位资深社会科学家，可以对用户提出的社会现象做出自己的解释")

            // 设置用户提示词模板
            .user(u -> u.text(
                    "你来说说为什么会有{issue}，可以给出更多的例子来支持你的观点吗？" +
                    "不要使用markdown格式进行回答，不要包含*#-等markdown格式，可以进行换行或者空格使得格式美观"
            )

            // 为模板中的占位符 issue 提供实际值
            .param("issue", issue))

            // 同步调用模型
            .call()

            // 获取模型生成的文本
            .content();
}
```

访问示例：

```text
http://localhost:8080/ai/issue?issue=年轻人不愿意结婚
```

### 2.2 什么是 System Prompt？

System Prompt 通常称为系统提示词，用于告诉模型本次任务应该遵循什么样的角色设定、行为规则和输出要求。

例如：

```java
.system("你是一位资深社会科学家，可以对用户提出的社会现象做出自己的解释")
```

这段内容不是用户实际提出的问题，而是开发者为模型设置的任务背景。

可以将两类消息理解为：

| 消息类型 | 作用 | 本例内容 |
|---|---|---|
| System Message | 告诉模型应该如何完成任务 | 你是一位资深社会科学家 |
| User Message | 告诉模型本次需要完成什么任务 | 为什么会出现某种社会现象？ |

在 Spring AI 中，`.system()` 用于设置系统消息，`.user()` 用于设置用户消息。Spring AI 会将这些消息组织成模型请求中的对应消息类型。

需要注意：System Prompt 是对模型行为的指导，不是严格的权限控制机制，也不能保证模型一定遵循所有要求。

### 2.3 什么是提示词模板？

本例中的用户提示词包含：

```java
u.text("你来说说为什么会有{issue}，可以给出更多的例子来支持你的观点吗？")
 .param("issue", issue)
```

这里的 `{issue}` 是模板占位符。

假设接口收到的参数为：

```text
issue = 年轻人不愿意结婚
```

Spring AI 会使用模板参数将占位符替换为对应的实际值，形成类似下面的用户消息：

```text
你来说说为什么会有年轻人不愿意结婚，
可以给出更多的例子来支持你的观点吗？
```

这样就不必为每个问题拼接一段新的提示词，而是可以重复使用同一个模板。

例如，接口收到不同的参数：

- `年轻人不愿意结婚`
- `城市房价持续上涨`
- `人工智能对就业市场的影响`

模板结构不变，变化的只有传入的参数。

### 2.4 param() 背后的实现机制

`param()` 不是直接向模型发送一条新消息，而是为提示词模板提供变量值。

可以把它理解为以下过程：

```text
用户提交 issue
        ↓
.user(u -> u.text(...))
        ↓
构建用户提示词模板
        ↓
.param("issue", issue)
        ↓
将 issue 的实际值填入模板
        ↓
得到完整的用户消息
        ↓
与 System Message 等消息一起组成请求
        ↓
调用大模型
```

在 Spring AI 的模板机制中，框架会根据模板内容和参数构建最终的提示词。具体模板渲染实现会随 Spring AI 版本及相关配置而有所不同。

这里的关键是：**模板参数用于动态生成提示词，而不是自动生成 SQL 参数，也不是自动调用 Java 方法。**

如果模板中存在未提供的变量，可能会导致模板渲染失败，因此变量名称需要与提供的参数名称相匹配。

另外，示例中把用户输入作为模板参数传入，优于直接把用户输入拼接进模板字符串，因为模板参数能够明确区分模板结构和动态值。不过，参数填充并不意味着可以完全消除提示词注入风险，仍然需要合理设计提示词与业务校验。

---

## 三、结构化输出：entity()

### 3.1 为什么需要结构化输出？

之前的接口通过：

```java
.call()
.content();
```

获取的是一个 `String`。

但是，假设希望模型推荐一本书，并返回以下信息：

- 书名
- 作者
- 大致价格
- 三个标签

如果只获取字符串，模型可能返回一段自然语言，也可能返回 JSON 文本。业务代码如果需要使用其中的每个字段，就还需要额外解析。

例如，后续业务可能需要：

- 将书名和作者分别显示在不同的页面区域。
- 根据价格进行排序。
- 根据标签筛选推荐结果。
- 将推荐信息传递给其他业务方法。

这时可以使用 Spring AI 的结构化输出能力。

本版本新增代码：

```java
// 定义记录类，用于描述期望获得的数据结构
public record BookInfo(
        String title,
        String author,
        double price,
        List<String> tags
) {}

@RequestMapping("/book")
public BookInfo book(@RequestParam String keyword) {

    return chatClient.prompt()

            // 告诉模型需要推荐哪一类书籍，并说明期望返回的字段
            .user("推荐一本关于" + keyword + "的经典书，返回书名、作者、大致价格和3个标签")

            // 同步调用模型
            .call()

            // 将模型响应转换为 BookInfo 对象
            .entity(BookInfo.class);
}
```

访问示例：

```text
http://localhost:8080/ai/book?keyword=Java
```

如果模型返回的信息能够成功转换，业务代码得到的就不再只是一个字符串，而是一个 `BookInfo` 对象。

### 3.2 entity() 的作用

```java
.entity(BookInfo.class)
```

这里的 `BookInfo.class` 用于告诉 Spring AI：希望将模型响应转换成什么 Java 类型。

`BookInfo` 是一个 Java record，包含四个组件：

| 字段 | 类型 | 作用 |
|---|---|---|
| `title` | `String` | 书名 |
| `author` | `String` | 作者 |
| `price` | `double` | 大致价格 |
| `tags` | `List<String>` | 书籍标签 |

Spring AI 会根据目标类型尝试将模型输出转换为对应的 Java 对象。

需要注意，`entity()` 并不是让大模型直接在 JVM 中创建 Java 对象。模型只能生成文本或结构化响应数据，真正的 Java 对象由应用端的转换机制创建。

### 3.3 entity() 背后的实现机制

可以将处理过程理解为以下步骤：

```text
ChatClient.prompt()
        ↓
.user(...)
        ↓
.call()
        ↓
Spring AI 构建模型请求
        ↓
在适用的实现中，根据目标类型生成输出格式指导
        ↓
模型生成符合预期结构的响应
        ↓
.entity(BookInfo.class)
        ↓
结构化输出转换机制解析响应
        ↓
生成 BookInfo 对象
```

这里需要区分两个环节。

**第一，要求模型按照目标结构生成响应。**

Spring AI 的结构化输出机制可以根据目标 Java 类型提供相应的格式指导，例如要求模型输出包含 `title`、`author`、`price` 和 `tags` 的 JSON。

不同模型实现及配置对结构化输出的支持程度不同。某些情况下可以使用模型提供的原生结构化输出功能；另一些情况下，则主要依靠提示词中的格式要求。

**第二，将模型响应转换成 Java 对象。**

在适用的 Spring AI 实现中，`entity()` 会利用结构化输出转换机制，将响应内容转换为指定的 Java 类型。

例如，模型返回的内容可能类似于：

```json
{
  "title": "Effective Java",
  "author": "Joshua Bloch",
  "price": 80.0,
  "tags": ["Java", "编程", "最佳实践"]
}
```

转换成功后，应用就可以直接使用 `BookInfo` 的字段，而不必自己编写字符串截取逻辑。

### 3.4 entity() 与 content() 的区别

| 对比项 | `content()` | `entity(BookInfo.class)` |
|---|---|---|
| 返回类型 | `String` | `BookInfo` |
| 获取内容 | 提取模型生成的文本 | 将模型响应转换成目标类型 |
| 适用场景 | 普通问答、文章生成 | 推荐信息、业务数据、结构化结果 |
| 后续处理 | 通常需要自行解析文本 | 可以直接使用对象字段 |

例如：

```java
// 获取普通文本
String answer = chatClient.prompt()
        .user("介绍一下 Java")
        .call()
        .content();
```

而结构化输出可以写成：

```java
// 获取结构化对象
BookInfo book = chatClient.prompt()
        .user("推荐一本 Java 经典书籍")
        .call()
        .entity(BookInfo.class);
```

这两种方式的区别不是模型有没有能力生成 JSON，而是 Spring AI 最终如何向应用提供结果。

### 3.5 使用结构化输出时的注意事项

**结构化输出不代表结果一定正确。**

即使转换成功，也可能出现书名不准确、价格不合理、标签数量不符合要求等问题。因此：

1. 根据实际业务校验字段是否合法。
2. 对可能缺失的字段做好处理。
3. 对价格、数量等字段进行范围校验。
4. 对需要严格格式保证的场景，优先考虑模型原生结构化输出能力，并根据实际模型验证兼容性。
5. 不要把模型生成的价格、作者等信息直接视为经过事实验证的数据。

另外，`entity()` 的使用依赖当前 Spring AI 版本提供的对应 API，以及模型响应转换所需的相关支持。

---

## 本版本的核心知识点总结

| 功能 | 核心 API | 解决的问题 |
|---|---|---|
| 流式输出 | `stream().content()` | 逐步获取模型生成的文本 |
| 系统提示词 | `system()` | 设置模型的角色与任务要求 |
| 模板参数 | `user(...).param(...)` | 将动态参数填入提示词模板 |
| 结构化输出 | `entity(BookInfo.class)` | 将模型响应转换成 Java 对象 |

通过 v0.2，进一步认识到 Spring AI 不仅能够发起基础的大模型调用，还可以控制模型输入、处理流式响应，以及将模型输出转换成应用需要的数据结构。

本版本尚未实现对话历史持久化、工具调用和知识库检索。这些能力将在后续版本中逐步学习和扩展。

# v0.3：工具调用（Tool Calling / Function Calling）

本版本在 v0.2 的基础上，学习 Spring AI 2 的**工具调用**能力：

- 让大模型在回答问题时，能够**调用我们自己编写的 Java 方法**（工具），例如查询订单状态、查询天气、获取当前时间、生成随机数等。
- 学习定义工具的**两种方式**：`@Tool` 注解方式 与 `FunctionToolCallback` 函数式方式。
- 学习如何把工具**注册**到 `ChatClient`。
- 理解工具调用背后的**实现机制**：Java 方法如何被描述给大模型、大模型如何“请求调用工具”、Spring AI 如何执行工具并回传结果。

本版本新增接口：

| 接口 | 作用 |
|---|---|
| `/ai/tools` | 工具调用演示，用户用自然语言提问，模型自行判断是否调用工具 |
| `/ai/tools/definitions` | 查看已注册工具的名称、描述和 JSON Schema |

---

## 一、什么是工具调用？

在 v0.1、v0.2 中，大模型只能基于**它自己训练时学到的知识**来回答。例如，用户问“帮我查一下订单 A1001 的物流”，大模型并不知道你的订单系统里 A1001 到底是什么状态。

工具调用（Tool Calling，也叫 Function Calling）解决的问题是：

> **让大模型在需要时，调用我们提供的函数来获取实时、准确的数据，再基于这些数据作答。**

整个过程可以理解为：

```text
用户：帮我查一下订单 A1001 的状态
        ↓
大模型：这个问题需要用到“查询订单”这个工具
        ↓
大模型：返回一个“工具调用”请求（工具名 getOrderStatus，参数 orderId=A1001）
        ↓
Spring AI：代大模型执行 getOrderStatus("A1001")，得到“已发货，顺丰 SF1234567890”
        ↓
Spring AI：把执行结果回传给大模型
        ↓
大模型：基于结果生成最终回答“订单 A1001 已经发货，顺丰单号 SF1234567890”
```

关键点：**大模型并不直接执行你的 Java 方法**。它只负责“决定调用哪个工具、传什么参数”，真正执行方法的是运行在你自己服务器上的 Spring AI 应用。

---

## 二、定义工具的两种方式

Spring AI 2 提供了多种定义工具的方式，本版本演示其中最常用的两种。

### 2.1 方式一：@Tool 注解方式（推荐）

给一个普通方法加上 `@Tool` 注解，方法的参数用 `@ToolParam` 描述，就可以把方法变成工具。

`Tools.java`：

```java
@Component
public class Tools {

    // 模拟订单数据库
    private static final Map<String, String> ORDERS = Map.of(
            "A1001", "已发货，顺丰 SF1234567890",
            "A1002", "待支付，30 分钟后自动取消"
    );

    @Tool(description = "根据订单号查询订单状态和物流信息")
    public String getOrderStatus(@ToolParam(description = "订单号，格式如 A1001") String orderId) {
        return ORDERS.getOrDefault(orderId, "订单号不存在");
    }

    @Tool(description = "查询指定城市的天气，温度单位可选摄氏度或华氏度")
    public String getWeather(
            @ToolParam(description = "城市名称，例如：北京") String city,
            @ToolParam(description = "温度单位，celsius 表示摄氏度，fahrenheit 表示华氏度") String unit) {
        String temperature = "celsius".equalsIgnoreCase(unit) ? "25℃" : "77℉";
        return city + "今天晴，气温 " + temperature + "，适合出行";
    }

    @Tool(description = "获取当前的日期和时间")
    public String getCurrentTime() {
        return "当前时间为：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
```

说明：

- `@Tool(description = "...")`：工具的描述，**这是给大模型看的**。描述越清晰，模型越容易判断什么时候该用这个工具。
- `@ToolParam(description = "...")`：参数的描述，同样用于帮助模型理解参数含义。
- 方法的返回值（这里是 `String`）会作为工具执行结果回传给大模型。

### 2.2 方式二：FunctionToolCallback 函数式方式

当工具逻辑很简单，或想脱离 Spring Bean 直接用一个 Lambda 定义工具时，可以使用 `FunctionToolCallback`。

`ToolCallbackConfig.java`：

```java
@Configuration
public class ToolCallbackConfig {

    // 工具入参的 Java 类型，Spring AI 会根据它生成 JSON Schema
    public record RandomNumberRequest(int min, int max) {}

    @Bean
    public ToolCallback randomNumberTool() {
        return FunctionToolCallback
                .builder("randomNumber", (RandomNumberRequest request) -> {
                    int value = ThreadLocalRandom.current().nextInt(request.min(), request.max() + 1);
                    return "在 [" + request.min() + ", " + request.max() + "] 之间的随机整数是：" + value;
                })
                .description("在指定范围内生成一个随机整数，min 为最小值，max 为最大值")
                .inputType(RandomNumberRequest.class)
                .build();
    }
}
```

说明：

- `FunctionToolCallback.builder("randomNumber", 函数)`：第一个参数是工具名，第二个参数是函数实现（一个 Lambda）。
- `.description(...)`：工具描述。
- `.inputType(RandomNumberRequest.class)`：指定入参类型，Spring AI 会根据这个类型自动生成 JSON Schema。
- 最后 `.build()` 返回一个 `ToolCallback`，并注册成 Spring Bean。

### 2.3 两种方式的对比

| 对比项 | @Tool 注解方式 | FunctionToolCallback 方式 |
|---|---|---|
| 定义位置 | Spring Bean 上的方法 | 任意 Lambda / 函数对象 |
| 参数描述 | `@ToolParam` 注解 | 通过入参类型 + `description` |
| 适用场景 | 方法较多、有业务逻辑、需要依赖注入 | 逻辑简单、想快速定义一个函数 |
| 可读性 | 更接近普通 Java 方法，推荐 | 更紧凑 |

两种方式最终都会转换成统一的 `ToolCallback` 接口，Spring AI 对它们一视同仁。

---

## 三、注册工具

工具定义好之后，需要注册到 `ChatClient`，大模型才知道有哪些工具可用。

`ChatController.java`：

```java
public ChatController(ChatClient.Builder builder, Tools tools, ToolCallback randomNumberTool) {
    this.chatClient = builder
            // defaultTools(...) 是统一的工具注册方法，可以混用不同方式定义的工具
            .defaultTools(tools, randomNumberTool)
            .build();
}
```

注册 API：

| API | 作用 |
|---|---|
| `defaultTools(Object...)` | Spring AI 2 的统一工具注册方法，可同时接收 `@Tool` 注解对象、`ToolCallback`、`ToolCallbackProvider` |

`defaultTools` 表示这些工具作为**默认工具**注册，之后每次调用 `chatClient` 都会携带这些工具定义。如果需要针对某一次请求临时注册工具，可以使用 `prompt().tools(...)`。

> 注意：早期版本用 `defaultToolCallbacks(ToolCallback...)` 单独注册 `ToolCallback`，该方法在 Spring AI 2.0 中已经**废弃**。现在无论哪种方式定义的工具，都统一通过 `defaultTools(...)` / `tools(...)` 注册。

---

## 四、运行与测试

启动应用后，访问工具调用接口：

```text
http://localhost:8080/ai/tools?query=帮我查一下订单 A1001 的状态
```

模型会判断需要调用 `getOrderStatus` 工具，最终返回类似：

```text
订单 A1001 已经发货，物流单号是顺丰 SF1234567890。
```

更多测试示例：

| 请求 query | 预期触发的工具 |
|---|---|
| 帮我查一下订单 A1001 的状态 | getOrderStatus |
| 北京今天天气怎么样？用摄氏度 | getWeather |
| 现在几点了？ | getCurrentTime |
| 生成一个 1 到 100 之间的随机整数 | randomNumber |

查看已注册工具的定义（名称、描述、JSON Schema）：

```text
http://localhost:8080/ai/tools/definitions
```

> 注意：工具调用需要大模型**支持 Function Calling**。DeepSeek 的 `deepseek-chat` 支持工具调用；如果当前使用的 `deepseek-flash` 模型不支持，可以尝试在 `application.yml` 中把 `spring.ai.openai.chat.model` 改为 `deepseek-chat`。

---

## 五、实现机制详解

### 5.1 从 Java 方法到 JSON Schema

大模型本身不认识 Java 方法。Spring AI 需要先通过反射，把工具方法“翻译”成大模型能理解的描述，也就是 **JSON Schema**。

以 `getOrderStatus(String orderId)` 为例，Spring AI 通过读取 `@Tool` / `@ToolParam` 注解以及方法签名，生成类似这样的工具定义：

```json
{
  "name": "getOrderStatus",
  "description": "根据订单号查询订单状态和物流信息",
  "inputSchema": {
    "type": "object",
    "properties": {
      "orderId": {
        "type": "string",
        "description": "订单号，格式如 A1001"
      }
    },
    "required": ["orderId"]
  }
}
```

其中：

- `name` 默认取方法名；
- `description` 来自 `@Tool(description = ...)`；
- `inputSchema` 根据方法参数自动生成：参数名作为属性名，参数类型对应 schema 的 `type`，`@ToolParam` 的 `description` 会填进去。

对于 `FunctionToolCallback`，`inputSchema` 则是根据 `.inputType(...)` 指定的 Java 类型生成。

### 5.2 工具调用的完整过程

一次完整的工具调用，大致经历以下步骤（以 OpenAI 兼容接口为例）：

```text
1. 发送请求（带上 tools）
   ChatClient.prompt().user(query).call()
        ↓
   请求体包含：
   - messages：用户消息
   - tools：所有注册工具的名称、描述、JSON Schema
        ↓
2. 模型返回“工具调用”而不是最终回答
   模型判断需要工具，返回：
   - tool_calls：[{ name: "getOrderStatus", arguments: {"orderId": "A1001"} }]
        ↓
3. Spring AI 执行真正的 Java 方法
   根据 tool_calls 找到对应的 ToolCallback，
   反序列化参数，反射调用 getOrderStatus("A1001")
   得到返回字符串 "已发货，顺丰 SF1234567890"
        ↓
4. 把执行结果回传给模型
   将结果作为一条 role=tool 的消息追加到对话，
   再次请求模型
        ↓
5. 模型基于工具结果生成最终回答
   "订单 A1001 已经发货，顺丰单号 SF1234567890"
```

### 5.3 请求与响应的概念示意

**第一步：请求大模型时，请求体里除了 messages，还带上 tools 定义**

```json
{
  "model": "deepseek-chat",
  "messages": [
    { "role": "user", "content": "帮我查一下订单 A1001 的状态" }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "getOrderStatus",
        "description": "根据订单号查询订单状态和物流信息",
        "parameters": {
          "type": "object",
          "properties": {
            "orderId": { "type": "string", "description": "订单号，格式如 A1001" }
          },
          "required": ["orderId"]
        }
      }
    }
  ]
}
```

**第二步：模型没有直接输出答案，而是返回 tool_calls**

```json
{
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": null,
        "tool_calls": [
          {
            "id": "call_123",
            "type": "function",
            "function": {
              "name": "getOrderStatus",
              "arguments": "{\"orderId\": \"A1001\"}"
            }
          }
        ]
      }
    }
  ]
}
```

**第三步：Spring AI 执行方法后，把结果作为 role=tool 消息再次发给模型**

```json
{
  "messages": [
    { "role": "user", "content": "帮我查一下订单 A1001 的状态" },
    { "role": "assistant", "tool_calls": [ "..." ] },
    { "role": "tool", "tool_call_id": "call_123", "content": "已发货，顺丰 SF1234567890" }
  ]
}
```

**第四步：模型基于工具结果生成最终自然语言回答。**

### 5.4 Spring AI 内部的关键角色

| 类 / 接口 | 作用 |
|---|---|
| `ToolCallback` | 工具的统一抽象，负责提供工具定义并执行工具调用 |
| `MethodToolCallback` | `@Tool` 注解方法的工具回调，通过反射调用方法 |
| `FunctionToolCallback` | 基于函数 / Lambda 的工具回调 |
| `ToolDefinition` | 工具元信息：`name`、`description`、`inputSchema` |
| `ToolCallingAdvisor` | 请求级处理器，负责检测 `tool_calls`、执行工具、回传结果 |

说明：以上是 Spring AI 2 中工具调用相关的主要组件。具体类名和内部实现可能随版本变化，学习时以当前依赖版本为准。

> 关键结论：**工具调用不是大模型“调用”了你的方法，而是大模型“建议”调用，由应用端真正执行，再把结果喂回大模型。**

---

## 六、核心 API 总结

| 功能 | 核心 API | 作用 |
|---|---|---|
| 注解定义工具 | `@Tool` / `@ToolParam` | 把普通方法标注为工具 |
| 函数式定义工具 | `FunctionToolCallback.builder(...)` | 用 Lambda 构造工具 |
| 注册默认工具 | `defaultTools(...)` | 统一注册 `@Tool` 对象 / `ToolCallback` / `ToolCallbackProvider` |
| 查看工具定义 | `ToolCallback.getToolDefinition()` | 获取工具名称、描述、JSON Schema |

通过 v0.3，我们学会了让大模型“学会使用我们的方法”，这是构建 AI Agent（智能体）的基础能力——Agent 的核心就是“模型 + 工具 + 循环”。

---

**版本记录**

- v0.1：基础大模型调用，使用 `ChatClient` 完成普通问答。
- v0.2：流式输出、System Prompt、提示词模板参数、结构化输出。
- v0.3：工具调用（Tool Calling），@Tool 注解与 FunctionToolCallback 两种定义方式，以及实现机制。

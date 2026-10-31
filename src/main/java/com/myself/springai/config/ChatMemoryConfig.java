package com.myself.springai.config;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 对话记忆（Chat Memory）相关配置。
 *
 * 默认情况下，大模型是"无状态"的：它不记得上一轮对话说了什么，
 * 每次请求都是独立的。要让它"记得"之前的聊天内容，就需要：
 * 1. 把历史消息保存下来（ChatMemory）；
 * 2. 每次提问时把历史一起发给大模型（MessageChatMemoryAdvisor）。
 *
 * 本类通过定义两个 Bean 来实现这个能力。
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 定义"窗口式"对话记忆。
     *
     * MessageWindowChatMemory 只会为每个会话保留最近的 N 条消息（滑动窗口），
     * 超过窗口的旧消息会被自动丢弃。这样既能"记住"上下文，又不会让对话
     * 无限变长、消耗越来越多 token。
     *
     * 这里把窗口大小设为 10 条消息（大约 5 轮问答）。
     * 如果不设置，默认窗口是 20 条消息。
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                // 每个会话最多保留最近的 10 条消息
                .maxMessages(10)
                .build();
    }

    /**
     * 定义记忆顾问（Advisor）。
     *
     * Advisor 是 Spring AI 中的"请求处理器"，会在请求发出前、响应返回后
     * 插入额外的逻辑。MessageChatMemoryAdvisor 具体做两件事：
     *   1. 请求前：根据会话 ID 从 ChatMemory 取出历史消息，拼到本次请求里；
     *   2. 响应后：把本次的"用户消息 + 模型回答"存回 ChatMemory。
     *
     * 这样下次再提问时，大模型就能看到之前的聊天记录。
     */
    @Bean
    public MessageChatMemoryAdvisor chatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}

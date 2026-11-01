package com.myself.springai.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 嵌入模型（Embedding Model）演示。
 *
 * 嵌入模型的作用：把一段文本（句子、段落、文档等）转换成一个“向量”
 * （一组数字，例如 2048 个 float）。语义越相近的文本，它们对应的向量
 * 在空间中方向越接近、距离越近。
 */
@RestController
@RequestMapping("/ai/embedding")
public class EmbeddingController {

    private static final Logger log = LogManager.getLogger(EmbeddingController.class);

    // EmbeddingModel 是 Spring AI 提供的嵌入模型抽象，
    // 在 application.yml 配置好 embedding 相关属性后，Spring 会自动注入对应实现。
    private final EmbeddingModel embeddingModel;

    public EmbeddingController(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 演示：把三句话转换成向量，并计算两两之间的余弦相似度。
     *
     * 默认三句话里，前两句语义相近（都在说“苹果”），第三句语义不同（天气），
     * 所以前两句的相似度会明显高于它们各自和第三句的相似度。
     */
    @GetMapping("/similarity")
    public SimilarityResult similarity(
            @RequestParam(defaultValue = "我喜欢吃苹果") String text1,
            @RequestParam(defaultValue = "苹果是我最喜欢的水果") String text2,
            @RequestParam(defaultValue = "今天天气很好") String text3) {

        log.info("/embedding/similarity，text1={}，text2={}，text3={}", text1, text2, text3);

        // 1. 一次性把三句话交给嵌入模型，得到三个向量（float[]）
        List<float[]> vectors = embeddingModel.embed(List.of(text1, text2, text3));

        // 2. 计算两两之间的余弦相似度（范围约 0~1，越接近 1 表示越相似）
        double sim12 = cosineSimilarity(vectors.get(0), vectors.get(1));
        double sim13 = cosineSimilarity(vectors.get(0), vectors.get(2));
        double sim23 = cosineSimilarity(vectors.get(1), vectors.get(2));

        return new SimilarityResult(
                text1, text2, text3,
                vectors.get(0).length,   // 向量维度，用来确认配置（如 2048）是否生效
                sim12, sim13, sim23);
    }

    /**
     * 计算两个向量的余弦相似度。
     *
     * 公式：cos = (a·b) / (|a| * |b|)
     *   - a·b 是点积：对应位置相乘再相加
     *   - |a|、|b| 是向量的长度（模）
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;   // 点积
        double normA = 0.0; // a 的长度的平方
        double normB = 0.0; // b 的长度的平方
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // 返回给前端的结果
    public record SimilarityResult(
            String text1,
            String text2,
            String text3,
            int dimension,
            double text1Text2Similarity,
            double text1Text3Similarity,
            double text2Text3Similarity) {}
}

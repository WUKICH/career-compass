package com.coze.aicoze.chat;


import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.vo.HollandAnalysisVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ChatLoadDocumentTestTest {
//
//    @Resource
//    private ChatLoadDocumentTest chatLoadDocumentTest;
//
//    @Test
//    void chat() {
//        String chat = chatLoadDocumentTest.chat("核心技能：Python",1);
//        System.out.println(chat);
//    }
    private static final String SIMILARITY_URL = "http://localhost:5000/similarity";
    @Autowired
    private RestTemplate restTemplate;


    public double getSimilarity(String text1, String text2) {
        Map<String, String> request = Map.of("text1", text1, "text2", text2);
        var response = restTemplate.postForObject(
                "http://localhost:5000/similarity", request, Map.class);
        return (double) response.get("similarity");
    }

    public String classifyRelevance(String question, String answer) {
        double sim = getSimilarity(question, answer);
        System.out.println("Similarity: " + sim);
        if (sim >= 0.65) return "高";
        else if (sim <= 0.35) return "低";
        else return "中";
    }

    /**
     * 将文本转换为向量
     */
    private float[] getEmbedding(String text) {
        throw new UnsupportedOperationException("Embedding model not configured in current test profile.");
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double cosineSimilarity(float[] vecA, float[] vecB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += Math.pow(vecA[i], 2);
            normB += Math.pow(vecB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 判断相关性
     */
//    public String classifyRelevance(String question, String answer) {
//        float[] questionEmbedding = getEmbedding(question);
//        float[] answerEmbedding = getEmbedding(answer);
//        double similarity = cosineSimilarity(questionEmbedding, answerEmbedding);
//        System.out.println("Similarity: " + similarity);
//
//        if (similarity >= 0.6) return "高";
//        else if (similarity <= 0.3) return "低";
//        else return "中";
//    }

    @Test
    public void testHighRelevance() {
        String question = "这个问题很实在！项目实战经验确实是微服务架构方向的关键门槛。...你最需要哪方面的支持？";
        String answer = "技能提升路径";
        String relevance = classifyRelevance(question, answer);
        System.out.println(relevance);
        assertEquals("高", relevance);
    }

    @Test
    public void testLowRelevance() {
        String question = "说说你喜欢的岗位";
        String answer = "你说的对。";
        String relevance = classifyRelevance(question, answer);
        System.out.println(relevance);
        assertEquals("低", relevance);
    }

    @Test
    public void testMediumRelevance() {
        String question = "你最喜欢的编程语言是什么？为什么？";
        String answer = "Java 很流行，生态丰富。";
        String relevance = classifyRelevance(question, answer);
        System.out.println(relevance);
        assertEquals("中", relevance);
    }

    @Test
    public void testHighRelevanceSynonym() {
        String question = "如何提高微服务架构的稳定性？";
        String answer = "服务熔断、限流、重试机制，还有分布式链路追踪。";
        String relevance = classifyRelevance(question, answer);
        System.out.println(relevance);
        assertEquals("高", relevance);
    }

    @Test
    public void testLowRelevanceOffTopic() {
        String question = "请介绍一下你的项目经验";
        String answer = "我平时喜欢打篮球和看电影。";
        String relevance = classifyRelevance(question, answer);
        System.out.println(relevance);
        assertEquals("低", relevance);
    }

    @Test
    public void testBoundaryShortAnswer() {
        String question = "请详细描述你在高并发场景下如何设计缓存架构，包括缓存穿透、缓存雪崩的解决方案。";
        String answer = "布隆过滤器。";
        String relevance = classifyRelevance(question, answer);
        System.out.println(relevance);
        // 根据你的阈值，这里可能是“中”或“低”，跑一下实际值再调整断言
        assertTrue(relevance.equals("中") || relevance.equals("低"));
    }
    @TestConfiguration
    static class TestConfig {
        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @Test
    public void testToBeanQuiet(){
        String s = """
{
  "霍兰德职业兴趣代码": "I A C",
  "类型组合": "研究型(I)-艺术型(A)-常规型(C)",
  "霍兰德六个维度分析": [
    {
      "类型": "现实型(R)",
      "得分": "0/5",
      "倾向描述": "您的现实型(R)倾向在本测试中未显现。后端开发中需要一定的动手解决具体技术问题的能力,建议在工具使用、环境搭建等实践操作中加以关注。"
    },
    {
      "类型": "研究型(I)",
      "得分": "2/5",
      "倾向描述": "您的研究型(I)倾向明显。您喜欢通过分析、测试和验证来解决问题（如通过编写测试程序验证JVM参数）,这体现了后端开发中所需的深度排查、逻辑推理和原理探究能力。"
    },
    {
      "类型": "艺术型(A)",
      "得分": "2/5",
      "倾向描述": "您的艺术型(A)倾向非常突出。您高度重视代码的优雅性、结构的设计美感和系统的可演化性（如关注优雅的代码结构、系统的灵活性与可维护性）,这与追求高质量、可读性强、易于扩展的代码哲学高度契合。"
    },
    {
      "类型": "社会型(S)",
      "得分": "0/5",
      "倾向描述": "您的社会型(S)倾向在本测试中未显现。后端开发虽以独立编码为主,但仍需与产品、前端、测试等角色协作。建议在沟通接口、理解业务背景等方面有意识地加强。"
    },
    {
      "类型": "企业型(E)",
      "得分": "0/5",
      "倾向描述": "您的企业型(E)倾向在本测试中未显现。在技术选型、方案推动等场景中,影响他人、展示价值的能力同样重要,可以适当培养说服力和项目推动力。"
    },
    {
      "类型": "常规型(C)",
      "得分": "1/5",
      "倾向描述": "您的常规型(C)倾向存在。您认同并愿意遵循成熟、规范的流程来开展工作（如按照SOP进行性能优化）,这有助于保证工作的稳定性、可预测性,符合工程化开发的要求。"
    }
  ],
  "与目标岗位匹配度分析": {
    "优势匹配": {
      "标题": "优势匹配",
      "描述": "您的**研究型(I)**与**艺术型(A)**特质形成了强大的组合优势。研究型特质使您能深入技术细节,通过分析和测试解决复杂问题（如内存溢出排查）；艺术型特质则驱动您追求卓越的代码设计、优雅的架构和系统的长期可维护性。这两者结合,正是高级后端工程师向架构师发展的核心能力——既要有解决底层技术难题的钻研精神,也要有设计高扩展性、可持续演进系统的审美与远见。"
    },
    "潜在优势": {
      "标题": "潜在优势",
      "描述": "您的**常规型(C)**倾向是团队协作中的稳定器。遵循SOP和规范（如性能优化流程）,表明您重视工程纪律和流程的可靠性,这能有效降低线上风险,保证项目交付质量,是构建稳健、可维护的大型系统的重要基础。"
    },
    "需关注方向": {
      "标题": "需关注方向",
      "描述": "您的**社会型(S)**和**企业型(E)**倾向相对较弱。后端开发并非孤岛,需要:1) **加强协作(S)**:主动与产品、前端等角色沟通,深入理解业务需求而非仅实现功能,这能提升您所设计系统的业务贴合度。2) **提升影响力(E)**:在技术选型或方案设计中,学习如何清晰阐述技术方案的业务价值,说服团队成员,推动技术决策落地。这两项能力的补充,将使您从优秀的"执行者"成长为有影响力的"技术主导者"。"
    }
  }
}
                """;
        HollandAnalysisVO beanQuiet = JsonParseUtil.toBeanQuiet(s, HollandAnalysisVO.class);
        System.out.println(beanQuiet);
    }
}
/**
 * 0.9584615883361965
 * 0.7337213989614164
 * 0.6066696550015888
 * 0.5857927871734758
 * 0.6820138397764494
 * 0.6217683624167394
 */
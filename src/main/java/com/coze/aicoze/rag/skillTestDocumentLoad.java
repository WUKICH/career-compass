//package com.coze.aicoze.rag;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
//import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.support.ResourcePatternResolver;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Objects;
//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class skillTestDocumentLoad {
//    private final ResourcePatternResolver resourcePatternResolver;
//
//    public List<Document> loadMarkdown() {
//        ArrayList<Document> allDocuments = new ArrayList<>();
//        try {
//            Resource[] resources = resourcePatternResolver.getResources("classpath:document/file.md");
//            if (resources.length < 1) {
//                log.warn("该路径下没有找到文件");
//                return allDocuments;
//            }
//            for (Resource resource : resources) {
//                String filename = resource.getFilename();
//                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
//                        .withHorizontalRuleCreateDocument(true)
//                        .withIncludeCodeBlock(false)
//                        .withIncludeBlockquote(false)
//                        .withAdditionalMetadata("filename", filename)
//                        .build();
//
//                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
//                allDocuments.addAll(reader.get());
//            }
//        } catch (IOException e) {
//            log.error("题库读取失败:{}",e.getMessage());
//        }
//        return allDocuments;
//    };
//}

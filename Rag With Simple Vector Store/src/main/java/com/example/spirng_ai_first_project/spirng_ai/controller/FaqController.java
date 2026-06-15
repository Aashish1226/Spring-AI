package com.example.spirng_ai_first_project.spirng_ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/olympics")
@Slf4j
public class FaqController {  // class names should be PascalCase

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:/prompts/rag-prompt-template.st")
    private Resource ragTemplate;

    // single constructor — no @RequiredArgsConstructor needed
    FaqController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    @GetMapping
    public String faq(@RequestParam(value = "question" , required = true) String question) throws IOException {

        // Step 1: search vector store for relevant chunks
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(5).build()  // topK(5) = return top 5 most relevant chunks
        );

        // Step 2: extract actual text from documents (not toString!)
        List<String> contentList = similarDocuments.stream()
                .map(Document::getText)  // fixed: was Document::toString
                .toList();

        // Step 3: read template file content and pass params
        String templateContent = ragTemplate.getContentAsString(StandardCharsets.UTF_8);  // fixed: was ragTemplate.toString()

        return chatClient.prompt()
                .user(u -> u.text(templateContent)
                        .param("questions", question)
                        .param("documents", String.join("\n", contentList)))
                .call()
                .content();
    }
}
package com.example.spirng_ai_first_project.spirng_ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController("/popular")
public class Food {

    private ChatClient chatClient;

    @Value("classpath:/Prompts/FoodPrompt.st")
    private Resource foodPromptResource;

     Food(ChatClient.Builder  builder) {
         this.chatClient = builder.build();
    }



//     UNDERSTAND THE PROMPT TEMPLATING..

//    1 st APPROACH IS USING THE PROMPT TEMPLATE OBEJCT , THEN CREATE PROMPT OBJECT THEN CALL THE API

    @GetMapping("/popular")
    public String popularOldMethod(@RequestParam(value = "genre" , defaultValue = "food") String genre) {
//        String message = "List of 10 of the most popular ${genre} along with their info. If you don't know the answer , just say I don't know";

        PromptTemplate promptTemplate = new PromptTemplate(foodPromptResource);
        Prompt prompt = promptTemplate.create(Map.of("genre", genre));
        return chatClient.prompt(prompt).call().content();
    }


//    2 ND APPROACH --> USING THE user().param() to reduece the verbose..

    @GetMapping("/popular-user")
    public String popularNewWay(@RequestParam(defaultValue = "food") String genre) {
        return chatClient.prompt()
                .user(u -> u.text("List 10 of the most popular {genre} along with their info. If you don't know, say I don't know.")
                        .param("genre", genre))
                .call()
                .content();
    }


//    3rd approach user + system

    @GetMapping("/popular-system-user")
    public String popularSystemUser(@RequestParam(defaultValue = "food") String genre) {
        return chatClient.prompt()
                .system("You are a helpful assistant. If you don't know the answer, say I don't know.")
                .user(u -> u.text("List 10 of the most popular {genre} along with their info.")
                        .param("genre", genre))
                .call()
                .content();
    }





}

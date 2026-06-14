package com.example.spirng_ai_first_project.spirng_ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/openai/chat")
@Slf4j
public class OpenAIChatController {


	// We use ChatClient.Builder because ChatClient has many optional settings.
	//  The Builder Pattern avoids large constructors and improves readability. 
	// Spring AI also auto-configures the builder with the underlying AI model, 
	// allowing us to create customized ChatClient instances with different prompts
	// , advisors, or tools while reusing the same base configuration.

	private ChatClient chatClient;

	OpenAIChatController(ChatClient.Builder builder){
		log.info("Building the OpenAI chat client");
		this.chatClient = builder.build();
	}


    @GetMapping("")
    public ResponseEntity<?> chat(@RequestParam String contents) {
        var apiResponse = chatClient.prompt(new Prompt("sdfsd")).call().content();
        return ResponseEntity.ok(chatClient.prompt("Tell me a dad Joke").call().content());
    }



}

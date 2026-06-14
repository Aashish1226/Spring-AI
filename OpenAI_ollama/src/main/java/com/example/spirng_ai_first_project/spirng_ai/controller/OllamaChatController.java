package com.example.spirng_ai_first_project.spirng_ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.chat.model.ChatModel;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RestController
@RequestMapping("/ollama/chat")
@Slf4j
public class OllamaChatController {

	private final ChatClient chatClient;

	// OllamaChatController(ChatClient.Builder builder) {
	// 	log.info("Building the Ollama chat client");
	// 	this.chatClient = builder.build();
	// }

	// IF WE WANT TO INJECT THE BEAN OF CHAT CLIENT USING THT CHATMODEL WE CAN DO LIKE THIS

    OllamaChatController(ChatModel chatModel) {
		log.info("Building the Ollama chat client");

		// org.springframework.ai.ollama.OllamaChatModel
		log.info("Chat Model we are using is: {}", chatModel.getClass().getName());
		this.chatClient = ChatClient.builder(chatModel).build();
    }


	@GetMapping
	public ResponseEntity<?> chat(@RequestParam(value = "q", required = true) String q) {
		log.info("Getting the Ollama chat request");
		try {

//            call().content() --> Extracts only the text content from the AI's response
//            call().chatResponse() -- Returns the full ChatResponse object
    // we can get from chatResponse things below:)
    //metadata
    //token usage
    //finish reason
    //model information (depending on provide

//            var apiResponse = chatClient.prompt(q).call().content();

			var aiResponse = chatClient.prompt(q).
                    call().chatResponse();

            return ResponseEntity.ok(aiResponse);
		} catch (Exception e) {
			log.error("Error in Ollama chat", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in chat");
		}
	}



//     UNDERSTAND THE PROMPT TEMPLATING..

//    1 st APPROACH IS USING THE PROMPT TEMPLATE OBEJCT , THEN CREATE PROMPT OBJECT THEN CALL THE API

    @GetMapping("/popular")
    public String popularOldMethod(@RequestParam(value = "genre" , defaultValue = "food") String genre) {
        String message = "List of 10 of the most popular ${genre} along with their info. If you don't know the answer , just say I don't know";

        PromptTemplate promptTemplate = new PromptTemplate(message);
        Prompt prompt = promptTemplate.create(Map.of("genre", genre));
        return chatClient.prompt(prompt).call().content();
    }


//    2 ND APPROACH --> USING THE user().param() to reduce the verbose..

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

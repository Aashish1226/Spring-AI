package com.example.spirng_ai_first_project.spirng_ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// USING THE OLLAMA AS WE DON'T HAVE THE OPENAI PAID KEY.....:)


// ============================================================
//  WHAT IS THIS FILE?
//  A Spring REST controller that talks to an LLM (via Spring AI).
//  It has ONE endpoint: GET /olympics/2024
//  That endpoint answers questions about 2024 Olympic sports.
//
//  TWO APPROACHES are shown here side by side:
//    - Approach 1 → PromptTemplate (loads from .st file)
//    - Approach 2 → ChatClient fluent builder (inline prompt)
//
//  The key concept being demonstrated is "Prompt Stuffing" (RAG lite):
//  When stuffit=true → we inject a real document into the prompt as context
//  When stuffit=false → LLM answers from its own trained knowledge only
// ============================================================

@RestController
// Marks this class as a REST controller.
// Every method return value is written directly into the HTTP response body as JSON/String.
// No view/template rendering happens — pure API.

@RequestMapping("/olympics")
// All endpoints in this controller are prefixed with /olympics.
// So our method below becomes: GET /olympics/2024

@Slf4j
// Lombok annotation — auto-injects a `log` variable (SLF4J logger).
// Lets you do log.info(), log.error() etc. without boilerplate.

public class OlympicController {

    // --------------------------------------------------------
    //  DEPENDENCY: ChatClient
    //  This is Spring AI's main class for talking to any LLM.
    //  Think of it as an HTTP client, but for AI models.
    //  It abstracts away OpenAI / Ollama / Anthropic etc.
    //  You just call .prompt().call().content() and get a String back.
    // --------------------------------------------------------
    private final ChatClient chatClient;


    // --------------------------------------------------------
    //  RESOURCE: olympic-sports.st  (the PROMPT TEMPLATE file)
    //
    //  @Value("classpath:/Prompts/olympic-sports.st")
    //  This annotation tells Spring to load this file from
    //  src/main/resources/Prompts/olympic-sports.st at startup.
    //
    //  The file content looks like this:
    //  ┌─────────────────────────────────────────────────────┐
    //  │ Use the following pieces of context to answer the   │
    //  │ question at the end. If you don't know the answer   │
    //  │ just say "I'm sorry but I don't know the answer".   │
    //  │                                                     │
    //  │ {context}                                           │
    //  │                                                     │
    //  │ Question: {question}                                │
    //  └─────────────────────────────────────────────────────┘
    //
    //  {context} and {question} are PLACEHOLDERS.
    //  Spring AI will substitute them with actual values at runtime.
    //
    //  Why a .st file? → "st" = StringTemplate format.
    //  Keeping prompts in files (not hardcoded in Java) means:
    //    1. You can change the prompt without recompiling
    //    2. Non-developers (PMs, designers) can edit it
    //    3. Cleaner code — logic stays in Java, words stay in files
    // --------------------------------------------------------
    @Value("classpath:/Prompts/olympic-sports.st")
    private Resource olympicSportsResource;


    // --------------------------------------------------------
    //  RESOURCE: olympic-sports.txt  (the DOCUMENT / CONTEXT file)
    //
    //  This is the actual knowledge document.
    //  It contains factual info about 2024 Olympics sports.
    //  Example content: "32 sports are included in Paris 2024.
    //  They include Athletics, Swimming, Gymnastics..."
    //
    //  When stuffit=true → this document is injected into {context}
    //  in the prompt template above.
    //
    //  This is called PROMPT STUFFING or RAG (Retrieval Augmented Generation) lite.
    //  The idea: instead of hoping the LLM knows the answer,
    //  you physically paste the relevant document INTO the prompt.
    //  The LLM reads it and answers from THAT, not from its training.
    //
    //  This matters because:
    //    - LLMs have a knowledge cutoff date
    //    - Private/internal data was never in training
    //    - You get more accurate, grounded answers
    // --------------------------------------------------------
    @Value("classpath:docs/olympics-sports.txt")
    private Resource docsResource;


    // --------------------------------------------------------
    //  CONSTRUCTOR: Building the ChatClient
    //
    //  Spring injects ChatClient.Builder (pre-configured via application.yml).
    //  Your yml file has stuff like:
    //    spring.ai.ollama.base-url=http://localhost:11434
    //    spring.ai.ollama.chat.model=llama3
    //
    //  builder.build() creates the actual ChatClient instance.
    //  We log a message so you can confirm in the console that
    //  the client was initialized when the app starts.
    // --------------------------------------------------------
    OlympicController(ChatClient.Builder builder ) {
        log.info("Building the Ollama chat client");
        this.chatClient = builder.build();
    }


    // ============================================================
    //  APPROACH 1: PromptTemplate (recommended for RAG / complex prompts)
    //
    //  FLOW:
    //  Request → load .st file → fill placeholders → build Prompt → send to LLM → return String
    //
    //  URL examples:
    //    GET /olympics/2024                          → default question, no context
    //    GET /olympics/2024?stuffit=true             → default question + document injected
    //    GET /olympics/2024?message=How many sports? → custom question, no context
    //    GET /olympics/2024?message=How many sports?&stuffit=true → custom + context
    // ============================================================
    @GetMapping("/2024")
    public String get2024OlympicSports_Approach1(
            @RequestParam(value = "message", defaultValue = "What sports are being included in the 2024 Summer Olympics?") String message,
            // @RequestParam → reads from URL query string (?message=...)
            // defaultValue → if user doesn't pass ?message=, this string is used
            // So the LLM always has a question to answer

            @RequestParam(value = "stuffit", defaultValue = "false") boolean stuffit
            // stuffit=true  → inject the .txt document as context (RAG mode)
            // stuffit=false → let LLM answer from its own knowledge (vanilla mode)

    ) throws IOException {

        // STEP 1: Load the prompt template from the .st file
        // olympicSportsResource is the Resource pointing to olympic-sports.st
        // PromptTemplate reads that file and prepares it for variable substitution
        // Think of it like a SQL PreparedStatement — template with ? placeholders
        PromptTemplate promptTemplate = new PromptTemplate(olympicSportsResource);

        // STEP 2: Build the variable map that fills the {placeholders}
        // Keys in this map MUST match placeholder names in the .st file
        // {question} → replaced by whatever the user passed as `message`
        // {context}  → replaced by document content OR empty string
        Map<String, Object> map = new HashMap<>();
        map.put("question", message);
        // Note: key is "question" because the .st file has {question}, not {message}

        if (stuffit) {
            // RAG MODE: inject the actual olympic-sports.txt document
            // docsResource is a Spring Resource — PromptTemplate will call
            // docsResource.getContentAsString() internally to read the file text
            // Now {context} in the template = full text of olympic-sports.txt
            // The LLM reads this document and answers FROM it
            String docsContent = docsResource.getContentAsString(StandardCharsets.UTF_8);
            map.put("context", docsContent);
        } else {
            // VANILLA MODE: no document, LLM uses its own trained knowledge
            // {context} = empty string → the "context" section of prompt is blank
            map.put("context", "");
        }

        // STEP 3: Create the Prompt object
        // promptTemplate.create(map) substitutes all {placeholders} with map values
        // Result is a fully resolved Prompt object with the final text ready to send
        // Example final prompt text:
        // "Use the following pieces of context...
        //  [content of olympic-sports.txt or empty]
        //  Question: What sports are being included in the 2024 Summer Olympics?"
        Prompt prompt = promptTemplate.create(map);
        log.info("Prompt :  {}" , prompt.toString());
        log.info("Prompt :  {}" , prompt.getContents());
        // STEP 4: Send to LLM and return the response
        // chatClient.prompt(prompt) → pass the built Prompt object
        // .call()                   → execute the request (blocking/sync)
        // .content()                → extract just the text string from the response
        return chatClient.prompt(prompt).call().content();
    }


    // ============================================================
    //  APPROACH 2: ChatClient Fluent Builder (good for simple / dynamic prompts)
    //
    //  FLOW:
    //  Request → build prompt inline with lambdas → send to LLM → return String
    //
    //  No external .st file needed.
    //  Prompt is defined directly in Java code using a lambda.
    //  Variable substitution via .param() calls instead of a Map.
    //
    //  Same URL patterns work here too.
    // ============================================================
    @GetMapping("/2024-v2")
    public String get2024OlympicSports_Approach2(
            @RequestParam(value = "message", defaultValue = "What sports are being included in the 2024 Summer Olympics?") String message,
            @RequestParam(value = "stuffit", defaultValue = "false") boolean stuffit
    ) throws IOException {

        // STEP 1: Prepare context value (same logic as Approach 1)
        Object context;
        if (stuffit) {
            context = docsResource; // inject document → RAG mode
        } else {
            context = "";           // empty → vanilla LLM mode
        }

        // STEP 2: Use ChatClient's fluent builder to construct and send the prompt inline
        return chatClient
                .prompt()
                // .prompt() → starts a new prompt builder (no pre-built Prompt object)
                // This is the "blank canvas" mode

                .user(u -> u
                                // .user() → defines the USER message (as opposed to SYSTEM message)
                                // In LLM APIs there are roles: system, user, assistant
                                // system → sets behaviour/persona of the AI
                                // user   → the actual question being asked
                                // assistant → the AI's reply (you don't set this, LLM fills it)

                                .text("""
                                Use the following pieces of context to answer the question at the end.
                                If you don't know the answer just say "I'm sorry but I don't know the answer to that".
                                
                                {context}
                                
                                Question: {question}
                                """)
                                // .text() → the raw prompt string with {placeholders}
                                // This is the INLINE version of what was in olympic-sports.st
                                // Advantage: everything in one place, easy to read
                                // Disadvantage: changing prompt = recompile + redeploy

                                .param("question", message)
                                // .param() → substitutes {question} in the text above
                                // Equivalent to map.put("question", message) in Approach 1

                                .param("context", context)
                        // .param() → substitutes {context} in the text above
                        // If stuffit=true, context = docsResource (Spring AI reads the file)
                        // If stuffit=false, context = "" (blank section in prompt)
                )
                .call()
                // .call() → execute the LLM request synchronously
                // Spring AI handles HTTP, serialization, retries etc. under the hood

                .content();
        // .content() → extract the LLM's reply as a plain String
        // ChatClient returns a ChatResponse object internally,
        // .content() is a convenience method to get just the text
    }


    // ============================================================
    //  QUICK COMPARISON SUMMARY
    //
    //  Feature               Approach 1 (PromptTemplate)    Approach 2 (Fluent Builder)
    //  ─────────────────────────────────────────────────────────────────────────────
    //  Prompt location       External .st file               Inline in Java code
    //  Change prompt         Edit file, no recompile         Must recompile Java
    //  Variable syntax       {placeholder} via Map           {placeholder} via .param()
    //  Code complexity       Slightly more setup             Less setup, one chain
    //  Best for              RAG, complex prompts, prod      Prototyping, simple prompts
    //  Add system message?   Needs separate SystemPrompt     Easy: just add .system()
    //  Team collaboration    PMs/designers can edit .st      Only devs can edit
    //
    //  FOR YOUR NL-TO-SQL PROJECT:
    //  Use Approach 1 — your schema DDL files = docsResource
    //  stuffit=true injects the schema into the prompt
    //  LLM reads the schema and generates correct SQL for that schema
    //  This is exactly the RAG pattern you're building!
    // ============================================================

}
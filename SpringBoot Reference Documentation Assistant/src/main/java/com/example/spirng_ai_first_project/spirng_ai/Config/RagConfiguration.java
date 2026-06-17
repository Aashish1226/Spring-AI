package com.example.spirng_ai_first_project.spirng_ai.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Configuration  // Marks this class as a Spring configuration class — Spring will scan this and register the beans defined inside it
@Slf4j          // Lombok annotation — auto-generates a `log` variable so you can do log.info(), log.error() etc without manually creating a Logger
public class RagConfiguration {

    @Value("classpath:/docs/olympic-faq.txt")  // Injects the file located at src/main/resources/docs/olympic-faq.txt into this variable at startup
    private Resource resource;                  // Spring's Resource is a wrapper around any file/URL — makes it easy to read files from classpath, disk, URL etc

    @Value("vectorstore.json")       // Injects the string "vectorstore.json" — this will be used as the filename for saving/loading the vector store
    private String vectorStoreName;  // Just a filename string, not the full path — full path is built in getVectorStoreFile()

    @Bean  // Tells Spring to manage this method's return value as a bean — other classes can now @Autowire SimpleVectorStore
    SimpleVectorStore simpleVectorStore(EmbeddingModel embeddingModel) throws IOException {
        // EmbeddingModel is injected by Spring — it's the Ollama embedding model (nomic-embed-text etc) configured in application.properties
        // It converts text → float[] vectors (numbers) so similarity search can work mathematically

        var simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        // Creates an in-memory vector store backed by a ConcurrentHashMap
        // SimpleVectorStore is Spring AI's basic vector store — not for production, good for learning/demos
        // builder() pattern used because direct constructor was deprecated in Spring AI 1.0+

        var vectorStoreFile = getVectorStoreFile();
        // Calls the helper method below to get a File object pointing to src/main/resources/data/vectorstore.json
        // This file is where the vector store will be saved/loaded from — avoids re-embedding documents on every startup

        if (vectorStoreFile.exists()) {
            // Checks if vectorstore.json already exists on disk
            // If yes — no need to re-read and re-embed the documents, just load the already computed vectors

            log.info("Vector Store File Exists, loading from file");  // Just a log message so you can see what's happening in console
            simpleVectorStore.load(vectorStoreFile);                   // Deserializes the JSON file back into in-memory vectors — skips the expensive embedding step
        } else {
            // vectorstore.json doesn't exist yet — first time running the app
            // Need to read the raw text, split it, embed it, and save it

            log.info("Vector Store File Does Not Exist, loading documents");  // Log so you know it's doing the full embedding process

            TextReader textReader = new TextReader(resource);
            // TextReader is Spring AI's utility to read a Resource (file) as a list of Documents
            // A Document in Spring AI = text content + metadata map

            textReader.getCustomMetadata().put("filename", "olympic-faq.txt");
            // Attaches metadata to every document chunk read from this file
            // Metadata is useful later — you can filter search results by filename, source, date etc

            List<Document> documents = textReader.get();
            // Actually reads the file and returns it as a List<Document>
            // At this point it's the whole file as one or few large documents — not split yet

            TextSplitter textSplitter = TokenTextSplitter.builder().build();
            // TokenTextSplitter breaks large documents into smaller chunks based on token count
            // Why? Because embedding models have token limits, and smaller chunks = more precise similarity search
            // .builder().build() uses default settings: ~800 tokens per chunk

            List<Document> splitDocuments = textSplitter.apply(documents);
            // Actually performs the splitting — takes your big documents and returns many smaller Document chunks
            // e.g. 1 large document → 10 smaller chunks, each ~800 tokens

            simpleVectorStore.add(splitDocuments);
            // THIS is where the embedding happens — for each chunk, it calls Ollama's embedding model
            // Each chunk of text gets converted to a float[] vector and stored in memory
            // This is the expensive step — hits Ollama API once per chunk

            simpleVectorStore.save(vectorStoreFile);
            // Serializes the entire in-memory vector store to vectorstore.json on disk
            // Next time app starts, the `if` block above will load this file instead of re-embedding everything
        }

        return simpleVectorStore;
        // Returns the populated vector store as a Spring bean
        // Other classes (like your RAG service/controller) will inject this and call similaritySearch() on it
    }

    private File getVectorStoreFile() {
        Path path = Paths.get("src", "main", "resources", "data");
        // Builds a relative path → src/main/resources/data
        // Paths.get() is Java NIO — cleaner way to build file paths across OS (handles / vs \ automatically)

        String absolutePath = path.toFile().getAbsolutePath() + "/" + vectorStoreName;
        // Converts relative path to absolute path based on where the app is running from
        // Then appends "/vectorstore.json" to get the full file path
        // e.g. /home/aashish/projects/spring-ai/src/main/resources/data/vectorstore.json

        return new File(absolutePath);
        // Returns a File object pointing to that full path
        // Note: this does NOT create the file — just represents the path
        // Make sure src/main/resources/data/ directory exists or simpleVectorStore.save() will throw an error
    }
}
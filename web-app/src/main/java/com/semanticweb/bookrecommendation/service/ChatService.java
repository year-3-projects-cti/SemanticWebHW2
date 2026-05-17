package com.semanticweb.bookrecommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semanticweb.bookrecommendation.model.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    @Value("${app.llm.api-url:https://openrouter.ai/api/v1/chat/completions}")
    private String llmApiUrl;

    @Value("${app.llm.api-key:}")
    private String llmApiKey;

    @Value("${app.llm.model:openai/gpt-4o-mini}")
    private String llmModel;

    @Autowired
    private VectorDbService vectorDbService;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String processMessage(String message, String pageContext) {
        try {
            String dbContext = vectorDbService.getContextForQuery(message);
            String systemPrompt = "You are a helpful book recommendation assistant. " +
                    "Answer questions ONLY based on the following book database. " +
                    "Do not use your general knowledge — only the data below.\n\n" +
                    dbContext +
                    (pageContext != null && !pageContext.isBlank()
                            ? "\nThe user is currently on page: " + pageContext
                            : "");
            return callLlm(systemPrompt, message);
        } catch (Exception e) {
            return "Error communicating with AI service: " + e.getMessage();
        }
    }

    public List<String> getConversationStarters(String pageContext, String bookTitle) {
        try {
            String prompt;
            if (bookTitle != null && !bookTitle.isBlank()) {
                String ctx = vectorDbService.getContextForQuery(bookTitle);
                prompt = "Generate exactly 3 short conversation starter questions about the book \"" + bookTitle + "\". " +
                        "Context:\n" + ctx + "\nReturn ONLY 3 questions, one per line, no numbering or bullets.";
            } else if (pageContext != null && pageContext.contains("/books")) {
                prompt = "Generate exactly 3 short conversation starters for a books list page " +
                        "in a book recommendation system. Return ONLY 3 questions, one per line, no numbering.";
            } else {
                prompt = "Generate exactly 3 short conversation starters for a book recommendation chatbot home page. " +
                        "Return ONLY 3 questions, one per line, no numbering.";
            }
            String raw = callLlm("You are a helpful book recommendation assistant.", prompt);
            List<String> starters = new ArrayList<>();
            for (String line : raw.split("\n")) {
                line = line.trim().replaceAll("^[0-9]+[.)\\s]+", "");
                if (!line.isBlank()) starters.add(line);
                if (starters.size() == 3) break;
            }
            while (starters.size() < 3) starters.add("What books would you recommend?");
            return starters;
        } catch (Exception e) {
            return List.of("What books are available?", "Which book should I read first?", "Tell me about the authors.");
        }
    }

    private String callLlm(String systemPrompt, String userMessage) throws Exception {
        if (llmApiKey == null || llmApiKey.isBlank() || llmApiKey.startsWith("YOUR_")) {
            return "Chat is not configured. Please set app.llm.api-key in application.properties.";
        }
        Map<String, Object> body = Map.of(
                "model", llmModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(llmApiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + llmApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<?, ?> resp = objectMapper.readValue(response.body(), Map.class);
        List<?> choices = (List<?>) resp.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<?, ?> msg = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            if (msg != null) return (String) msg.get("content");
        }
        return "No response received from AI service.";
    }
}

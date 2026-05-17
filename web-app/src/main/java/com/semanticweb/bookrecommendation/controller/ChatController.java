package com.semanticweb.bookrecommendation.controller;

import com.semanticweb.bookrecommendation.model.ChatMessage;
import com.semanticweb.bookrecommendation.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/chat")
    public String chatPage() {
        return "redirect:/";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatMessage message,
                                                    @RequestParam(required = false) String context) {
        String response = chatService.processMessage(message.getContent(), context);
        return ResponseEntity.ok(Map.of("response", response));
    }
}

package com.semanticweb.bookrecommendation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semanticweb.bookrecommendation.model.GraphData;
import com.semanticweb.bookrecommendation.service.RdfService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/rdf")
public class RdfController {

    private final RdfService rdfService;
    private final ObjectMapper objectMapper;

    public RdfController(RdfService rdfService, ObjectMapper objectMapper) {
        this.rdfService = rdfService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "rdf/upload";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, Model model) throws Exception {
        GraphData graphData = rdfService.parseRdfToGraph(file.getInputStream(), file.getOriginalFilename());
        model.addAttribute("graphJson", objectMapper.writeValueAsString(graphData));
        model.addAttribute("filename", file.getOriginalFilename());
        model.addAttribute("nodeCount", graphData.getNodes().size());
        model.addAttribute("edgeCount", graphData.getEdges().size());
        return "rdf/graph";
    }
}

package com.semanticweb.bookrecommendation.controller;

import com.semanticweb.bookrecommendation.model.Book;
import com.semanticweb.bookrecommendation.service.RdfService;
import com.semanticweb.bookrecommendation.service.VectorDbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/books")
public class BookController {

    @Autowired
    private RdfService rdfService;

    @Autowired
    private VectorDbService vectorDbService;

    @GetMapping
    public String listBooks(Model model) {
        model.addAttribute("books", rdfService.getAllBooks());
        return "books/list";
    }

    @GetMapping("/add")
    public String addBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("formAction", "/books/add");
        model.addAttribute("allLevels", List.of("Beginner", "Intermediate", "Advanced"));
        model.addAttribute("allGenres", List.of("ScienceFiction", "Fantasy", "Mystery", "Murder", "Romance", "Thriller"));
        return "books/form";
    }

    @PostMapping("/add")
    public String addBook(@ModelAttribute Book book, RedirectAttributes redirectAttributes) {
        rdfService.addBook(book);
        vectorDbService.rebuildIndex();
        redirectAttributes.addFlashAttribute("success", "Book \"" + book.getTitle() + "\" added successfully.");
        return "redirect:/books";
    }

    @GetMapping("/{localName}")
    public String bookDetail(@PathVariable String localName, Model model) {
        Book book = rdfService.getBookByLocalName(localName);
        if (book == null) return "redirect:/books";
        model.addAttribute("book", book);
        return "books/detail";
    }

    @GetMapping("/{localName}/edit")
    public String editBookForm(@PathVariable String localName, Model model) {
        Book book = rdfService.getBookByLocalName(localName);
        if (book == null) return "redirect:/books";
        model.addAttribute("book", book);
        model.addAttribute("localName", localName);
        model.addAttribute("formAction", "/books/" + localName + "/edit");
        model.addAttribute("allLevels", List.of("Beginner", "Intermediate", "Advanced"));
        model.addAttribute("allGenres", List.of("ScienceFiction", "Fantasy", "Mystery", "Murder", "Romance", "Thriller"));
        return "books/form";
    }

    @PostMapping("/{localName}/edit")
    public String updateBook(@PathVariable String localName, @ModelAttribute Book book,
                             RedirectAttributes redirectAttributes) {
        rdfService.updateBook(localName, book);
        vectorDbService.rebuildIndex();
        redirectAttributes.addFlashAttribute("success", "Book updated successfully.");
        return "redirect:/books";
    }
}

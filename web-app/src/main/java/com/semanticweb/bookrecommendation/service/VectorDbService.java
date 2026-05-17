package com.semanticweb.bookrecommendation.service;

import com.semanticweb.bookrecommendation.model.Book;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory vector store using TF-IDF term vectors and cosine similarity.
 * Books are indexed at startup from the RDF model; call rebuildIndex() after mutations.
 */
@Service
public class VectorDbService {

    @Autowired
    private RdfService rdfService;

    private final List<BookEntry> index = new ArrayList<>();

    @PostConstruct
    public void buildIndex() {
        List<Book> books = rdfService.getAllBooks();
        index.clear();
        for (Book book : books) {
            String text = toText(book);
            Map<String, Double> vector = termFrequencyVector(text);
            index.add(new BookEntry(book, vector));
        }
    }

    public void rebuildIndex() {
        buildIndex();
    }

    /** Returns up to topK books most similar to the query string. */
    public List<Book> findSimilar(String query, int topK) {
        if (index.isEmpty()) return Collections.emptyList();
        Map<String, Double> queryVec = termFrequencyVector(query.toLowerCase());
        return index.stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.book, cosineSimilarity(queryVec, e.vector)))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /** Structured search: filter by author and/or genre with vector ranking as tiebreak. */
    public List<Book> findByAuthorAndGenre(String author, String genre) {
        String query = ((author != null ? author : "") + " " + (genre != null ? genre : "")).trim();
        List<Book> ranked = findSimilar(query.isEmpty() ? " " : query, index.size());
        if (ranked.isEmpty()) {
            ranked = index.stream().map(e -> e.book).collect(Collectors.toList());
        }
        return ranked.stream()
                .filter(b -> {
                    boolean matchAuthor = author == null || author.isBlank()
                            || b.getAuthor().toLowerCase().contains(author.toLowerCase());
                    boolean matchGenre = genre == null || genre.isBlank()
                            || b.getGenres().stream().anyMatch(g -> g.toLowerCase().contains(genre.toLowerCase()));
                    return matchAuthor && matchGenre;
                })
                .collect(Collectors.toList());
    }

    /** Builds a human-readable context string from top-3 books relevant to the query. */
    public String getContextForQuery(String query) {
        List<Book> relevant = findSimilar(query, 3);
        if (relevant.isEmpty()) relevant = index.stream().map(e -> e.book).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder("Relevant books from the database:\n");
        for (Book b : relevant) {
            sb.append("- \"").append(b.getTitle()).append("\"");
            if (b.getAuthor() != null && !b.getAuthor().isEmpty())
                sb.append(" by ").append(b.getAuthor());
            if (!b.getGenres().isEmpty())
                sb.append(", genres: ").append(String.join(", ", b.getGenres()));
            if (b.getReadingLevel() != null && !b.getReadingLevel().isEmpty())
                sb.append(", reading level: ").append(b.getReadingLevel());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String toText(Book book) {
        return String.join(" ",
                nvl(book.getTitle()),
                nvl(book.getAuthor()),
                book.getGenres() != null ? String.join(" ", book.getGenres()) : "",
                nvl(book.getReadingLevel())
        ).toLowerCase();
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private Map<String, Double> termFrequencyVector(String text) {
        Map<String, Double> freq = new HashMap<>();
        String[] words = text.replaceAll("[^a-z0-9 ]", " ").split("\\s+");
        for (String w : words) {
            if (!w.isBlank()) freq.merge(w, 1.0, Double::sum);
        }
        double norm = Math.sqrt(freq.values().stream().mapToDouble(v -> v * v).sum());
        if (norm > 0) freq.replaceAll((k, v) -> v / norm);
        return freq;
    }

    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            dot += e.getValue() * b.getOrDefault(e.getKey(), 0.0);
        }
        return dot;
    }

    private static class BookEntry {
        final Book book;
        final Map<String, Double> vector;

        BookEntry(Book book, Map<String, Double> vector) {
            this.book = book;
            this.vector = vector;
        }
    }
}

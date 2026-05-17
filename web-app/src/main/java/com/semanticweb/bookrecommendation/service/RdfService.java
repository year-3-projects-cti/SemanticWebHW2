package com.semanticweb.bookrecommendation.service;

import com.semanticweb.bookrecommendation.model.Book;
import com.semanticweb.bookrecommendation.model.GraphData;
import jakarta.annotation.PostConstruct;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class RdfService {

    private static final String NS = "http://example.org/books#";

    @Value("${app.rdf.file}")
    private String rdfFilePath;

    private Model model;
    private File dataFile;

    @PostConstruct
    public void init() {
        dataFile = new File(rdfFilePath);
        model = ModelFactory.createDefaultModel();
        if (dataFile.exists()) {
            try (InputStream in = new FileInputStream(dataFile)) {
                model.read(in, null, "RDF/XML");
            } catch (IOException e) {
                loadFromClasspath();
            }
        } else {
            loadFromClasspath();
        }
    }

    private void loadFromClasspath() {
        InputStream in = getClass().getClassLoader().getResourceAsStream(rdfFilePath);
        if (in != null) {
            model.read(in, null, "RDF/XML");
        }
    }

    private void saveModel() {
        dataFile.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(dataFile)) {
            model.write(out, "RDF/XML-ABBREV");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save RDF model", e);
        }
    }

    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, model.createResource(NS + "Book"));
        while (iter.hasNext()) {
            books.add(resourceToBook(iter.nextResource()));
        }
        books.sort(Comparator.comparing(Book::getTitle));
        return books;
    }

    public Book getBookByLocalName(String localName) {
        Resource r = model.createResource(NS + localName);
        if (!model.containsResource(r)) return null;
        Statement typeStmt = model.getProperty(r, RDF.type);
        if (typeStmt == null || !typeStmt.getResource().getURI().equals(NS + "Book")) return null;
        return resourceToBook(r);
    }

    public void addBook(Book book) {
        String localName = book.getTitle().replaceAll("[^a-zA-Z0-9]", "");
        Resource bookRes = model.createResource(NS + localName);
        model.add(bookRes, RDF.type, model.createResource(NS + "Book"));
        model.add(bookRes, model.createProperty(NS + "title"), book.getTitle());
        if (book.getAuthor() != null && !book.getAuthor().isEmpty()) {
            model.add(bookRes, model.createProperty(NS + "author"), book.getAuthor());
        }
        if (book.getGenres() != null) {
            for (String genre : book.getGenres()) {
                model.add(bookRes, model.createProperty(NS + "hasGenre"),
                        model.createResource(NS + genre));
            }
        }
        if (book.getReadingLevel() != null && !book.getReadingLevel().isEmpty()) {
            model.add(bookRes, model.createProperty(NS + "suitableForLevel"),
                    model.createResource(NS + book.getReadingLevel()));
        }
        saveModel();
    }

    public void updateBook(String localName, Book updated) {
        Resource bookRes = model.createResource(NS + localName);
        Property titleProp = model.createProperty(NS + "title");
        Property authorProp = model.createProperty(NS + "author");
        Property genreProp = model.createProperty(NS + "hasGenre");
        Property levelProp = model.createProperty(NS + "suitableForLevel");

        model.removeAll(bookRes, titleProp, null);
        model.removeAll(bookRes, authorProp, null);
        model.removeAll(bookRes, genreProp, null);
        model.removeAll(bookRes, levelProp, null);

        model.add(bookRes, titleProp, updated.getTitle());
        if (updated.getAuthor() != null && !updated.getAuthor().isEmpty()) {
            model.add(bookRes, authorProp, updated.getAuthor());
        }
        if (updated.getGenres() != null) {
            for (String genre : updated.getGenres()) {
                model.add(bookRes, genreProp, model.createResource(NS + genre));
            }
        }
        if (updated.getReadingLevel() != null && !updated.getReadingLevel().isEmpty()) {
            model.add(bookRes, levelProp, model.createResource(NS + updated.getReadingLevel()));
        }
        saveModel();
    }

    private Book resourceToBook(Resource r) {
        Property titleProp = model.createProperty(NS + "title");
        Property authorProp = model.createProperty(NS + "author");
        Property genreProp = model.createProperty(NS + "hasGenre");
        Property levelProp = model.createProperty(NS + "suitableForLevel");

        String title = r.hasProperty(titleProp) ? r.getProperty(titleProp).getString() : r.getLocalName();
        String author = r.hasProperty(authorProp) ? r.getProperty(authorProp).getString() : "";

        List<String> genres = new ArrayList<>();
        StmtIterator genreIter = r.listProperties(genreProp);
        while (genreIter.hasNext()) {
            genres.add(genreIter.next().getResource().getLocalName());
        }

        String readingLevel = r.hasProperty(levelProp)
                ? r.getProperty(levelProp).getResource().getLocalName() : "";

        return new Book(r.getURI(), title, author, genres, readingLevel);
    }

    public GraphData parseRdfToGraph(InputStream inputStream, String filename) {
        Model tempModel = ModelFactory.createDefaultModel();
        String lang = "RDF/XML";
        if (filename != null) {
            if (filename.endsWith(".ttl")) lang = "TURTLE";
            else if (filename.endsWith(".n3")) lang = "N3";
            else if (filename.endsWith(".nt")) lang = "N-TRIPLE";
        }
        tempModel.read(inputStream, null, lang);
        return modelToGraphData(tempModel);
    }

    private GraphData modelToGraphData(Model m) {
        GraphData graphData = new GraphData();
        Set<String> nodeIds = new LinkedHashSet<>();

        StmtIterator stmts = m.listStatements();
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            Resource subject = stmt.getSubject();
            Property predicate = stmt.getPredicate();
            RDFNode object = stmt.getObject();

            String subId = subject.isURIResource() ? subject.getURI() : "_:" + subject.getId().toString();
            String subLabel = subject.isURIResource() ? subject.getLocalName() : subject.getId().toString();

            if (nodeIds.add(subId)) {
                graphData.getNodes().add(new GraphData.GraphNode(subId, subLabel, "subject"));
            }

            if (object.isURIResource()) {
                Resource objRes = object.asResource();
                String objId = objRes.getURI();
                String objLabel = objRes.getLocalName();
                if (nodeIds.add(objId)) {
                    graphData.getNodes().add(new GraphData.GraphNode(objId, objLabel, "object"));
                }
                graphData.getEdges().add(new GraphData.GraphEdge(subId, objId, predicate.getLocalName()));
            } else if (object.isLiteral()) {
                String litLabel = object.asLiteral().getString();
                if (litLabel.length() > 30) litLabel = litLabel.substring(0, 27) + "...";
                String litId = subId + "_" + predicate.getLocalName() + "_" + nodeIds.size();
                graphData.getNodes().add(new GraphData.GraphNode(litId, litLabel, "literal"));
                graphData.getEdges().add(new GraphData.GraphEdge(subId, litId, predicate.getLocalName()));
            }
        }

        return graphData;
    }
}

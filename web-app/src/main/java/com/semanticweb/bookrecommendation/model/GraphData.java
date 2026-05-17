package com.semanticweb.bookrecommendation.model;

import java.util.ArrayList;
import java.util.List;

public class GraphData {
    private List<GraphNode> nodes = new ArrayList<>();
    private List<GraphEdge> edges = new ArrayList<>();

    public List<GraphNode> getNodes() { return nodes; }
    public List<GraphEdge> getEdges() { return edges; }

    public static class GraphNode {
        private String id;
        private String label;
        private String group; // "subject", "object", "literal" — used by vis.js color mapping

        public GraphNode(String id, String label, String group) {
            this.id = id;
            this.label = label;
            this.group = group;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getGroup() { return group; }
    }

    public static class GraphEdge {
        private String from;
        private String to;
        private String label;

        public GraphEdge(String from, String to, String label) {
            this.from = from;
            this.to = to;
            this.label = label;
        }

        public String getFrom() { return from; }
        public String getTo() { return to; }
        public String getLabel() { return label; }
    }
}

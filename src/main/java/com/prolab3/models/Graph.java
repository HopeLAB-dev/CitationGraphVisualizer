package com.prolab3.models;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class Graph {
    // ID -> Node eşleşmesi
    public Map<String, Node> nodes = new HashMap<>();
    
    // Toplam istatistikler için kenar sayısı (yönlü)
    public int totalEdges = 0;

    public void addNode(Node node) {
        nodes.putIfAbsent(node.id, node);
    }

    public Node getNode(String id) {
        return nodes.get(id);
    }
    
    public void addEdge(String fromId, String toId) {
        Node from = nodes.get(fromId);
        Node to = nodes.get(toId);
        
        if (from != null && to != null) {
            // Check existence to avoid duplicate edge counts if called multiple times
            if (!from.outgoingEdges.contains(to)) {
                from.addOutgoing(to);
                to.addIncoming(from);
                totalEdges++;
            }
        }
    }
    
    public List<Node> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }
    
    public void clear() {
        nodes.clear();
        totalEdges = 0;
    }
}

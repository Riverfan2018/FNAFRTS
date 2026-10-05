package fnafrts.model.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Node {
    private final String id;
    private final String sectorId;
    private final int x;
    private final int y;
    private final NodeType type;
    private final List<String> neighbors = new ArrayList<>();

    public Node(String id, String sectorId, int x, int y, NodeType type) {
        this.id = id;
        this.sectorId = sectorId;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public String getId() { return id; }
    public String getSectorId() { return sectorId; }
    public int getX() { return x; }
    public int getY() { return y; }
    public NodeType getType() { return type; }

    public List<String> getNeighbors() {
        return Collections.unmodifiableList(neighbors);
    }

    void addNeighbor(String neighborId) {
        if (!neighbors.contains(neighborId)) {
            neighbors.add(neighborId);
        }
    }

    @Override
    public String toString() {
        return String.format("Node[%s sector=%s pos=(%d,%d) type=%s vecinos=%s]",
                id, sectorId, x, y, type, neighbors);
    }
}
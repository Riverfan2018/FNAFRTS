package fnafrts.model.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sector {
    private final String id;
    private final String displayName;
    private final int x, y;
    private final List<String> nodeIds = new ArrayList<>();

    public Sector(String id, String displayName, int x, int y) {
        this.id = id;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getX() { return x; }
    public int getY() { return y; }

    public List<String> getNodeIds() {
        return Collections.unmodifiableList(nodeIds);
    }

    void addNode(String nodeId) {
        if (!nodeIds.contains(nodeId)) {
            nodeIds.add(nodeId);
        }
    }

    @Override
    public String toString() {
        return String.format("Sector[%s \"%s\" pos=(%d,%d) nodos=%d]",
                id, displayName, x, y, nodeIds.size());
    }
}
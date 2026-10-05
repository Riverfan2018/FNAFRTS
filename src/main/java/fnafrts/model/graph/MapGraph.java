package fnafrts.model.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapGraph {
    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, Sector> sectors = new LinkedHashMap<>();
    private final Map<String, List<String>> pathCache = new HashMap<>();

    // Oficina: id de puerta -> nodo ENTRY_*
    private String leftEntryNodeId;
    private String rightEntryNodeId;

    // ---------- Construcción ----------

    public void addSector(Sector sector) {
        if (sectors.containsKey(sector.getId())) {
            throw new IllegalArgumentException("Sector duplicado: " + sector.getId());
        }
        sectors.put(sector.getId(), sector);
    }

    public void addNode(Node node) {
        if (nodes.containsKey(node.getId())) {
            throw new IllegalArgumentException("Nodo duplicado: " + node.getId());
        }
        if (!sectors.containsKey(node.getSectorId())) {
            throw new IllegalArgumentException(
                    "Nodo " + node.getId() + " referencia sector inexistente: " + node.getSectorId());
        }
        nodes.put(node.getId(), node);
        sectors.get(node.getSectorId()).addNode(node.getId());
    }

    public void addLink(String fromId, String toId, boolean bidirectional) {
        Node from = requireNode(fromId);
        Node to = requireNode(toId);
        from.addNeighbor(toId);
        if (bidirectional) {
            to.addNeighbor(fromId);
        }
    }

    public void setOfficeEntries(String leftEntryNodeId, String rightEntryNodeId) {
        this.leftEntryNodeId = leftEntryNodeId;
        this.rightEntryNodeId = rightEntryNodeId;
    }

    // ---------- Consultas ----------

    public Node getNode(String id) {
        return nodes.get(id);
    }

    public Sector getSector(String id) {
        return sectors.get(id);
    }

    public List<Node> getNodesOfSector(String sectorId) {
        Sector s = sectors.get(sectorId);
        if (s == null) return List.of();
        List<Node> result = new ArrayList<>();
        for (String nid : s.getNodeIds()) {
            result.add(nodes.get(nid));
        }
        return result;
    }

    public List<Node> getNeighbors(String nodeId) {
        Node n = requireNode(nodeId);
        List<Node> result = new ArrayList<>();
        for (String nid : n.getNeighbors()) {
            result.add(nodes.get(nid));
        }
        return result;
    }

    /**
     * Ruta más corta (BFS) de un nodo a otro. Incluye origen y destino.
     * Devuelve lista vacía si no hay ruta. Cachea el resultado.
     */
    public List<String> shortestPath(String fromId, String toId) {
        return shortestPath(fromId, toId, java.util.Collections.emptySet());
    }

    public List<String> shortestPath(String fromId, String toId, Set<String> avoid) {
        String key = cacheKey(fromId, toId, avoid);
        List<String> cached = pathCache.get(key);
        if (cached != null) return cached;
        List<String> computed = computeShortestPath(fromId, toId, avoid);
        pathCache.put(key, computed);
        return computed;
    }

    private String cacheKey(String fromId, String toId, Set<String> avoid) {
        if (avoid.isEmpty()) return fromId + ">" + toId;
        List<String> sorted = new ArrayList<>(avoid);
        Collections.sort(sorted);
        return fromId + ">" + toId + "!" + String.join(",", sorted);
    }

    private List<String> computeShortestPath(String fromId, String toId, Set<String> avoid) {
        if (fromId.equals(toId)) return List.of(fromId);
        if (!nodes.containsKey(fromId) || !nodes.containsKey(toId)) return List.of();

        Map<String, String> parent = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(fromId);
        parent.put(fromId, null);

        while (!queue.isEmpty()) {
            String cur = queue.poll();
            for (String nb : nodes.get(cur).getNeighbors()) {
                if (parent.containsKey(nb)) continue;
                if (avoid.contains(nb)) continue;
                parent.put(nb, cur);
                if (nb.equals(toId)) {
                    LinkedList<String> path = new LinkedList<>();
                    String n = toId;
                    while (n != null) {
                        path.addFirst(n);
                        n = parent.get(n);
                    }
                    return path;
                }
                queue.add(nb);
            }
        }
        return List.of();
    }

    public List<Node> findNodesByType(NodeType type) {
        List<Node> result = new ArrayList<>();
        for (Node n : nodes.values()) {
            if (n.getType() == type) result.add(n);
        }
        return result;
    }

    public int getWorldX(String nodeId) {
        Node n = requireNode(nodeId);
        return sectors.get(n.getSectorId()).getX() + n.getX();
    }

    public int getWorldY(String nodeId) {
        Node n = requireNode(nodeId);
        return sectors.get(n.getSectorId()).getY() + n.getY();
    }

    public String getOfficeNodeId() {
        for (Node n : nodes.values()) {
            if (n.getType() == NodeType.OFFICE) return n.getId();
        }
        return null;
    }

    public String getOfficeSectorId() {
        String officeId = getOfficeNodeId();
        return officeId == null ? null : nodes.get(officeId).getSectorId();
    }

    public String getLeftEntryNodeId() { return leftEntryNodeId; }
    public String getRightEntryNodeId() { return rightEntryNodeId; }

    public Map<String, Node> getNodes() { return Collections.unmodifiableMap(nodes); }
    public Map<String, Sector> getSectors() { return Collections.unmodifiableMap(sectors); }

    // ---------- Validación ----------

    /**
     * Lanza IllegalStateException si el grafo no es jugable.
     * Útil tras cargar un mapa desde archivo.
     */
    public void validate() {
        List<String> errors = new ArrayList<>();

        if (nodes.isEmpty()) errors.add("El mapa no tiene nodos.");
        if (sectors.isEmpty()) errors.add("El mapa no tiene sectores.");

        // Nodos huérfanos (sin vecinos)
        for (Node n : nodes.values()) {
            if (n.getNeighbors().isEmpty()) {
                errors.add("Nodo huérfano (sin vecinos): " + n.getId());
            }
        }

        // Entradas de oficina
        if (leftEntryNodeId == null) {
            errors.add("Falta OFFICE left.");
        } else if (!nodes.containsKey(leftEntryNodeId)) {
            errors.add("OFFICE left apunta a nodo inexistente: " + leftEntryNodeId);
        } else if (nodes.get(leftEntryNodeId).getType() != NodeType.ENTRY_LEFT) {
            errors.add("Nodo " + leftEntryNodeId + " no es de tipo ENTRY_LEFT.");
        }

        if (rightEntryNodeId == null) {
            errors.add("Falta OFFICE right.");
        } else if (!nodes.containsKey(rightEntryNodeId)) {
            errors.add("OFFICE right apunta a nodo inexistente: " + rightEntryNodeId);
        } else if (nodes.get(rightEntryNodeId).getType() != NodeType.ENTRY_RIGHT) {
            errors.add("Nodo " + rightEntryNodeId + " no es de tipo ENTRY_RIGHT.");
        }
        
        // Oficina: única y correctamente conectada
        List<Node> offices = findNodesByType(NodeType.OFFICE);
        if (offices.size() > 1) {
            errors.add("Hay más de un nodo OFFICE en el mapa.");
        }
        if (offices.size() == 1) {
            Node office = offices.get(0);
            if (office.getNeighbors().isEmpty()) {
                errors.add("El nodo OFFICE no está conectado a ninguna puerta.");
            }
            for (String nid : office.getNeighbors()) {
                NodeType t = nodes.get(nid).getType();
                if (t != NodeType.ENTRY_LEFT && t != NodeType.ENTRY_RIGHT) {
                    errors.add("El nodo OFFICE solo puede conectarse a ENTRY_LEFT o ENTRY_RIGHT, "
                            + "pero conecta con " + nid + " (" + t + ").");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Mapa inválido:\n - " + String.join("\n - ", errors));
        }
    }

    private Node requireNode(String id) {
        Node n = nodes.get(id);
        if (n == null) throw new IllegalArgumentException("Nodo inexistente: " + id);
        return n;
    }
}
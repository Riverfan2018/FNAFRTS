package fnafrts.model.graph;

public enum NodeType {
    NORMAL,
    OFFICE,
    ENTRY_LEFT,
    ENTRY_RIGHT;

    public static NodeType fromToken(String token) {
        try {
            return NodeType.valueOf(token.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de nodo desconocido: " + token);
        }
    }
}
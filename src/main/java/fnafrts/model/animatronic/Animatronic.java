package fnafrts.model.animatronic;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fnafrts.core.GameState;
import fnafrts.model.graph.Node;
import fnafrts.model.graph.NodeType;

public class Animatronic {

    private final String id;
    private final String displayName;
    private final String symbol;
    private final Color color;

    private String currentNodeId;
    private final String homeNodeId;
    private final String respawnNodeId;

    // --- Compartido con subclases ---
    protected final Random rng;
    protected double timeSinceAttempt = 0.0;
    protected double moveInterval = 5.0;

    // --- Parámetros de IA (ajustables por animatrónico) ---
    private int aiLevel = 5;              // 0-20
    private double blockedThreshold = 6.0; // segundos esperando en puerta antes de respawn

    // --- Estado interno ---
    private double blockedTime = 0.0;
    private String lastSeenNodeId = null;

    public Animatronic(String id, String displayName, String symbol, Color color,
                       String homeNodeId, String respawnNodeId,
                       Random rng) {
        this.id = id;
        this.displayName = displayName;
        this.symbol = symbol;
        this.color = color;
        this.currentNodeId = homeNodeId;
        this.homeNodeId = homeNodeId;
        this.respawnNodeId = respawnNodeId;
        this.rng = rng;
    }

    public String getRespawnNodeId() { return respawnNodeId; }

    public boolean isForcedVisible() { return false; }

    public boolean canAcceptPizzaOrder() { return false; }
    public boolean acceptPizzaOrder(GameState state) { return false; }
    /** Por defecto, un animatrónico no puede ser shockeado. */
    public boolean canBeShocked() { return false; }
    public boolean acceptShock(GameState state) { return false; }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getSymbol() { return symbol; }
    public Color getColor() { return color; }
    public String getCurrentNodeId() { return currentNodeId; }
    public String getHomeNodeId() { return homeNodeId; }

    public void setAiLevel(int aiLevel) { this.aiLevel = Math.max(0, Math.min(20, aiLevel)); }
    public void setMoveInterval(double seconds) { this.moveInterval = seconds; }
    public void setBlockedThreshold(double seconds) { this.blockedThreshold = seconds; }

    public int getAiLevel() { return aiLevel; }
    public double getMoveInterval() { return moveInterval; }

    public String getLastSeenNodeId() { return lastSeenNodeId; }
    private String previousNodeId = null;
    public void setLastSeenNodeId(String nodeId) { this.lastSeenNodeId = nodeId; }
    public String getPreviousNodeId() { return previousNodeId; }
    public void setPreviousNodeId(String id) { this.previousNodeId = id; }

    public void setCurrentNodeId(String nodeId) { this.currentNodeId = nodeId; }

    /**
    * Intenta moverse al nodo deseado. Si está ocupado, retrocede al nodo anterior.
    * Devuelve true si logró moverse en cualquier dirección.
    */
    protected boolean moveOrRetreat(String targetNodeId, GameState state) {
        if (state.canEnter(getId(), targetNodeId)) {
            state.moveAnimatronic(this, targetNodeId);
            return true;
        }
        String prev = getPreviousNodeId();
        if (prev != null && !prev.equals(getCurrentNodeId())
                && state.canEnter(getId(), prev)) {
            state.moveAnimatronic(this, prev);
            return true;
        }
        return false;
    }

    // ---------- Tick ----------

    public void tick(double dt, GameState state) {
        Node here = state.getMap().getNode(currentNodeId);

        if (here.getType() == NodeType.ENTRY_LEFT || here.getType() == NodeType.ENTRY_RIGHT) {
            handleAtDoor(dt, state, here);
            return;
        }

        timeSinceAttempt += dt;
        if (timeSinceAttempt < moveInterval) return;
        timeSinceAttempt = 0.0;

        if (rng.nextInt(20) >= aiLevel) return;

        List<Node> neighbors = state.getMap().getNeighbors(currentNodeId);
        List<Node> available = new ArrayList<>();
        for (Node n : neighbors) {
            if (state.canEnter(getId(), n.getId())) {
                available.add(n);
            }
        }

        if (available.isEmpty()) return;

        Node target = available.get(rng.nextInt(available.size()));
        state.moveAnimatronic(this, target.getId());
    }

    private void handleAtDoor(double dt, GameState state, Node here) {
        boolean leftDoor = (here.getType() == NodeType.ENTRY_LEFT);

        if (!state.getDoors().isBlocked(here.getId())) {
            String officeId = state.getMap().getOfficeNodeId();
            if (officeId != null && !state.isNodeOccupiedByOther(officeId, getId())) {
                state.moveAnimatronic(this, officeId);
            }
            state.triggerGameOver(displayName + " ha entrado por la puerta "
                    + (leftDoor ? "izquierda" : "derecha"));
            return;
        }

        blockedTime += dt;
        if (blockedTime >= blockedThreshold) {
            respawn(state);
            blockedTime = 0.0;
        }
    }

    private void respawn(GameState state) {
        state.log(displayName + " bloqueado, vuelve a " + respawnNodeId);
        state.moveAnimatronic(this, respawnNodeId);
    }
}
package fnafrts.model.animatronic;

import java.awt.Color;
import java.util.Random;

import fnafrts.core.GameState;

public abstract class Animatronic {

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

    // --- Estado interno ---
    private String lastSeenNodeId = null;
    private String previousNodeId = null;

    /** Devuelve el id del nodo ENTRY que este animatrónico está bloqueando, o null. */
    public String getBlockedDoorNodeId() { return null; }

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

    public int getAiLevel() { return aiLevel; }
    public boolean isActive() { return aiLevel > 0; }
    public double getMoveInterval() { return moveInterval; }

    public String getLastSeenNodeId() { return lastSeenNodeId; }
    public void setLastSeenNodeId(String nodeId) { this.lastSeenNodeId = nodeId; }
    public String getPreviousNodeId() { return previousNodeId; }
    public void setPreviousNodeId(String id) { this.previousNodeId = id; }

    public void setCurrentNodeId(String nodeId) { this.currentNodeId = nodeId; }

    /** Cada subclase implementa su propia lógica de tick. */
    public abstract void tick(double dt, GameState state);

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

    // ---------- AI Level ----------

    protected static final double AI_FACTOR_AT_1  = 1.5;
    protected static final double AI_FACTOR_AT_20 = 0.5;
    protected static final double JITTER_RATIO    = 0.4;

    /**
     * Factor multiplicativo del intervalo según AI level.
     * IA 1 → 1.5 (intervalos más largos = más lento)
     * IA 20 → 0.5 (intervalos más cortos = más rápido)
     */
    protected double aiFactor() {
        int lvl = getAiLevel();
        if (lvl <= 0) return AI_FACTOR_AT_1;
        return AI_FACTOR_AT_1 - (lvl - 1) * (AI_FACTOR_AT_1 - AI_FACTOR_AT_20) / 19.0;
    }

    /**
     * Probabilidad de moverse cuando el timer expira.
     * Curva estándar: IA 1 → 57.5%, IA 20 → 94.5%.
     * IA 0 → 0% (nunca se mueve).
     */
    protected double aiProbability() {
        int lvl = getAiLevel();
        if (lvl <= 0) return 0.0;
        return aiLerp(0.575, 0.945);
    }

    /** Tira el dado contra aiProbability. */
    protected boolean passesAiRoll() {
        return rng.nextDouble() < aiProbability();
    }

    /**
     * Intervalo con jitter y factorIA aplicados.
     * base = valor central del intervalo en segundos.
     * Rango resultante: base * [1 - J/2, 1 + J/2] * factorIA
     */
    protected double rollInterval(double base) {
        double jitter = 1.0 - JITTER_RATIO / 2.0 + rng.nextDouble() * JITTER_RATIO;
        return base * jitter * aiFactor();
    }

    /**
     * Interpolación lineal entre dos valores según AI level.
     * IA 1 → at1, IA 20 → at20.
     */
    protected double aiLerp(double at1, double at20) {
        int lvl = getAiLevel();
        if (lvl <= 0) return at1;
        if (lvl >= 20) return at20;
        return at1 + (lvl - 1) * (at20 - at1) / 19.0;
    }
}
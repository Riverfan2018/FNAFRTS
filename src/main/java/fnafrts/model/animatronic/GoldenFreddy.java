package fnafrts.model.animatronic;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import fnafrts.core.GameState;
import fnafrts.model.graph.Node;
import fnafrts.model.graph.NodeType;

public class GoldenFreddy extends Animatronic {

    private static final double TELEPORT_MIN = 14.0;
    private static final double TELEPORT_MAX = 21.0;
    private static final double OFFICE_CHANCE_INCREMENT = 0.03;

    private static final double WATCH_KILL_TIME = 6.0;

    private static final double OFFICE_STAY_TIME = 5.0;
    private static final double CAMERAS_OFF_REQUIRED = 1.5;

    private static final double COOLDOWN_TIME = 20.0;

    private static final Set<String> FORBIDDEN_SECTORS = Set.of("entrance", "employee");

    public enum State {
        WANDERING,
        IN_OFFICE,
        COOLDOWN
    }

    private State state = State.WANDERING;
    private double teleportTimer;
    private double watchTimer = 0.0;
    private double officeTimer = 0.0;
    private double camerasOffAccum = 0.0;
    private double cooldownTimer = 0.0;
    private double officeChance = 0.0;

    public GoldenFreddy(String id, String displayName, String symbol, Color color,
                        String homeNodeId, Random rng) {
        super(id, displayName, symbol, color, homeNodeId, homeNodeId, rng);
        this.teleportTimer = rollTeleportInterval();
    }

    public State getState() { return state; }

    @Override
    public void tick(double dt, GameState gameState) {
        switch (this.state) {
            case WANDERING -> tickWandering(dt, gameState);
            case IN_OFFICE -> tickInOffice(dt, gameState);
            case COOLDOWN  -> tickCooldown(dt, gameState);
        }
    }

    // ---------- WANDERING ----------

    private void tickWandering(double dt, GameState gameState) {
        if (isObserved(gameState)) {
            watchTimer += dt;
            if (watchTimer >= WATCH_KILL_TIME) {
                gameState.triggerGameOver(getDisplayName() + " te atrapó mientras lo mirabas");
                return;
            }
        } else {
            watchTimer = 0;
        }

        teleportTimer -= dt;
        if (teleportTimer > 0) return;

        if (rng.nextDouble() < officeChance) {
            teleportToOffice(gameState);
            this.officeChance = 0.0;
            return;
        }

        teleportRandom(gameState);
        this.officeChance += OFFICE_CHANCE_INCREMENT;
        this.teleportTimer = rollTeleportInterval();
    }

    // ---------- IN_OFFICE ----------

    private void tickInOffice(double dt, GameState gameState) {
        officeTimer += dt;

        if (gameState.isCamerasOff()) {
            camerasOffAccum += dt;
            if (camerasOffAccum >= CAMERAS_OFF_REQUIRED) {
                gameState.log(getDisplayName() + " se desvanece.");
                gameState.moveAnimatronic(this, getHomeNodeId());
                resetAfterOffice();
                this.state = State.COOLDOWN;
                this.cooldownTimer = 0;
                return;
            }
        }

        if (officeTimer >= OFFICE_STAY_TIME) {
            gameState.triggerGameOver(getDisplayName() + " te atrapó en la oficina");
        }
    }

    // ---------- COOLDOWN ----------

    private void tickCooldown(double dt, GameState gameState) {
        cooldownTimer += dt;
        if (cooldownTimer >= COOLDOWN_TIME) {
            this.state = State.WANDERING;
            this.teleportTimer = rollTeleportInterval();
            this.cooldownTimer = 0;
        }
    }

    // ---------- Helpers ----------

    private void teleportToOffice(GameState gameState) {
        String officeId = gameState.getMap().getOfficeNodeId();
        if (officeId != null) {
            gameState.moveAnimatronic(this, officeId);
        }
        gameState.log(getDisplayName() + " está en la oficina.");
        this.state = State.IN_OFFICE;
        this.officeTimer = 0;
        this.camerasOffAccum = 0;
        this.watchTimer = 0;
    }

    private void teleportRandom(GameState gameState) {
        List<Node> candidates = new ArrayList<>();
        for (Node n : gameState.getMap().getNodes().values()) {
            if (FORBIDDEN_SECTORS.contains(n.getSectorId())) continue;
            if (n.getType() == NodeType.OFFICE) continue;
            if (n.getType() == NodeType.ENTRY_LEFT || n.getType() == NodeType.ENTRY_RIGHT) continue;
            if (!gameState.canEnter(getId(), n.getId())) continue;
            candidates.add(n);
        }
        if (candidates.isEmpty()) return;

        Node target = candidates.get(rng.nextInt(candidates.size()));
        gameState.moveAnimatronic(this, target.getId());
        this.watchTimer = 0;
    }

    private void resetAfterOffice() {
        this.officeTimer = 0;
        this.camerasOffAccum = 0;
        this.watchTimer = 0;
    }

    private double rollTeleportInterval() {
        return TELEPORT_MIN + rng.nextDouble() * (TELEPORT_MAX - TELEPORT_MIN);
    }

    private boolean isObserved(GameState gameState) {
        Node n = gameState.getMap().getNode(getCurrentNodeId());
        if (n == null) return false;
        String active = gameState.getActiveSectorId();
        return active != null && active.equals(n.getSectorId());
    }
}
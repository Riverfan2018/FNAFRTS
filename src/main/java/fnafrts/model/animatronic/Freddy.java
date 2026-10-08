package fnafrts.model.animatronic;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import fnafrts.core.GameState;
import fnafrts.model.graph.Node;

public class Freddy extends Animatronic {

    private static final double MOVE_BASE = 9.5;

    private static final double AT_DOOR_NOISY_TIME = 8.0;
    private static final double AT_DOOR_SILENT_MIN = 1.0;
    private static final double AT_DOOR_SILENT_MAX = 5.0;
    private static final double AT_DOOR_KILL_THRESHOLD = 3.0;
    private static final double AT_DOOR_ATTENTION_RATE = 0.25;
    private static final double BLINK_PERIOD = 0.5;

    private static final String LEFT_DOOR  = "PI";
    private static final String RIGHT_DOOR = "PD";

    private static final int ROUTE_SWITCH_THRESHOLD = 3;

    public enum State {
        GOING_TO_DOOR,
        AT_DOOR,
        DONE
    }

    private State state = State.GOING_TO_DOOR;
    private String targetDoor;

    private double doorTime = 0;
    private double doorOpenAccum = 0;
    private double silentDuration = 0;
    private boolean noisyPhase = true;
    private boolean blinkOn = true;
    private double blinkTimer = 0.0;

    private int nodesMoved = 0;

    public Freddy(String id, String displayName, String symbol, Color color,
                  String homeNodeId, String respawnNodeId, Random rng) {
        super(id, displayName, symbol, color, homeNodeId, respawnNodeId, rng);
        this.moveInterval = rollInterval(MOVE_BASE);
        this.targetDoor = rng.nextBoolean() ? LEFT_DOOR : RIGHT_DOOR;
    }

    public State getState() { return state; }

    @Override
    public boolean isForcedVisible() {
        if (state != State.AT_DOOR) return false;
        // Durante la fase de ruido parpadea. En la silenciosa queda fijo.
        if (noisyPhase) return blinkOn;
        return true;
    }

    @Override
    public void tick(double dt, GameState state) {
        switch (this.state) {
            case GOING_TO_DOOR -> tickGoingToDoor(dt, state);
            case AT_DOOR       -> tickAtDoor(dt, state);
            case DONE -> { }
        }
    }

    private void tickGoingToDoor(double dt, GameState state) {
        if (targetDoor.equals(getCurrentNodeId())) {
            state.log(getDisplayName() + " está en la puerta.");
            this.state = State.AT_DOOR;
            this.doorTime = 0;
            this.doorOpenAccum = 0;
            this.noisyPhase = true;
            this.silentDuration = AT_DOOR_SILENT_MIN
                    + rng.nextDouble() * (AT_DOOR_SILENT_MAX - AT_DOOR_SILENT_MIN);
            return;
        }

        if (isObserved(state)) return;

        timeSinceAttempt += dt;
        if (timeSinceAttempt < moveInterval) return;
        timeSinceAttempt = 0;

        if (!passesAiRoll()) {
            moveInterval = rollInterval(MOVE_BASE);
            return;
        }

        List<String> path = state.getMap().shortestPath(getCurrentNodeId(), targetDoor);
        if (path.size() < 2) return;
        String next = path.get(1);

        if (!state.canEnter(getId(), next)) {
            if (nodesMoved < ROUTE_SWITCH_THRESHOLD) {
                targetDoor = targetDoor.equals(LEFT_DOOR) ? RIGHT_DOOR : LEFT_DOOR;
                nodesMoved = 0;
                timeSinceAttempt = 0;
                state.log(getDisplayName() + " cambia de ruta hacia "
                        + (targetDoor.equals(LEFT_DOOR) ? "la puerta izquierda" : "la puerta derecha") + ".");
                return;
            }
            if (moveOrRetreat(next, state)) {
                moveInterval = rollInterval(MOVE_BASE);
            } else {
                timeSinceAttempt = Math.max(0, moveInterval - 1.0);
            }
            return;
        }

        state.moveAnimatronic(this, next);
        nodesMoved++;
        moveInterval = rollInterval(MOVE_BASE);
    }

    private void tickAtDoor(double dt, GameState state) {
        doorTime += dt;
        boolean doorOpen = !state.getDoors().isBlocked(targetDoor);

        // Parpadeo durante la fase de ruido
        if (noisyPhase) {
            blinkTimer += dt;
            if (blinkTimer >= BLINK_PERIOD) {
                blinkTimer = 0;
                blinkOn = !blinkOn;
            }
        } else {
            blinkOn = true;   // en fase silenciosa queda fijo visible
        }

        if (doorOpen) {
        doorOpenAccum += dt;
        if (doorOpenAccum >= AT_DOOR_KILL_THRESHOLD) {
            String officeId = state.getMap().getOfficeNodeId();
            if (officeId != null) state.moveAnimatronic(this, officeId);
            state.triggerGameOver(getDisplayName() + " ha entrado por la puerta "
                    + (targetDoor.equals(LEFT_DOOR) ? "izquierda" : "derecha"));
            this.state = State.DONE;
            return;
        }
    }

        if (noisyPhase) {
            state.getAttention().contribute(AT_DOOR_ATTENTION_RATE);
            if (doorTime >= AT_DOOR_NOISY_TIME) {
                noisyPhase = false;
            }
        }

        double totalDuration = AT_DOOR_NOISY_TIME + silentDuration;
        if (doorTime >= totalDuration) {
            state.log(getDisplayName() + " se retira de la puerta.");
            respawn(state);
        }
    }

    private boolean isObserved(GameState state) {
        Node n = state.getMap().getNode(getCurrentNodeId());
        if (n == null) return false;
        String sector = n.getSectorId();
        return sector != null && sector.equals(state.getActiveSectorId());
    }

    private void respawn(GameState state) {
        state.moveAnimatronic(this, getRespawnNodeId());
        this.state = State.GOING_TO_DOOR;
        this.targetDoor = rng.nextBoolean() ? LEFT_DOOR : RIGHT_DOOR;
        this.doorTime = 0;
        this.doorOpenAccum = 0;
        this.noisyPhase = true;
        this.blinkOn = true;
        this.blinkTimer = 0.0;
        this.timeSinceAttempt = 0;
        this.nodesMoved = 0;
        this.moveInterval = rollInterval(MOVE_BASE);
    }
}
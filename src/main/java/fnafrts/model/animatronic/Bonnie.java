package fnafrts.model.animatronic;

import java.awt.Color;
import java.util.List;
import java.util.Set;

import fnafrts.core.GameState;
import fnafrts.model.graph.Node;

public class Bonnie extends Animatronic {

    private static final double MOVE_BASE = 5.75;
    private static final double OBSERVED_MULT = 1.25;
    private static final double SKIP_INTERVAL_MULT = 1.5;

    private static final double DOOR_WAIT_MIN = 4.0;
    private static final double DOOR_WAIT_MAX = 7.0;
    private static final double DOOR_KILL_DELAY = 2.0;
    private static final double DOOR_CLOSED_LEAVE_TIME = 3.0;

    private static final double H16_DETOUR_CHANCE_AT_1  = 0.10;
    private static final double H16_DETOUR_CHANCE_AT_20 = 0.35;
    private static final double H16_BLOCK_DURATION_AT_1  = 10.0;
    private static final double H16_BLOCK_DURATION_AT_20 = 20.0;
    private static final double H16_STAY_DURATION_AT_1  = 10.0;
    private static final double H16_STAY_DURATION_AT_20 = 3.0;

    private static final String HALLWAY1_ENTRY = "H11";
    private static final String H16 = "H16";
    private static final String RIGHT_DOOR = "PD";
    private static final Set<String> AVOID = Set.of("BP5");
    private static final String[] HALLWAY_SECTORS = { "hallway1", "hallway2" };

    public enum State {
        GOING_TO_HALLWAY1,
        GOING_TO_H16,
        WAITING_AT_H16,
        GOING_TO_RIGHT_DOOR,
        AT_RIGHT_DOOR,
        BLOCKING_DOOR,
        DONE
    }

    private State state = State.GOING_TO_HALLWAY1;
    private double stateTimer = 0.0;
    private double blockTimer = 0.0;

    private double doorWaitTimer = 0.0;
    private double doorWaitThreshold = 0.0;
    private double killTimer = 0.0;

    public Bonnie(String id, String displayName, String symbol, Color color,
              String homeNodeId, String respawnNodeId, java.util.Random rng) {
        super(id, displayName, symbol, color, homeNodeId, respawnNodeId, rng);
        this.moveInterval = rollInterval(MOVE_BASE);
    }

    public State getState() { return state; }

    @Override
    public boolean isForcedVisible() {
        return state == State.BLOCKING_DOOR;
    }

    @Override
    public String getBlockedDoorNodeId() {
        return state == State.BLOCKING_DOOR ? RIGHT_DOOR : null;
    }

    @Override
    public void tick(double dt, GameState state) {
        // El timer de bloqueo corre siempre, aunque Bonnie ya se haya ido.
        if (blockTimer > 0) {
            blockTimer -= dt;
            if (blockTimer <= 0) {
                blockTimer = 0;
                for (String s : HALLWAY_SECTORS) state.setSectorTapped(s, false);
            }
        }

        switch (this.state) {
            case GOING_TO_HALLWAY1   -> tickGoingToHallway1(dt, state);
            case GOING_TO_H16        -> tickGoingToH16(dt, state);
            case WAITING_AT_H16      -> tickWaitingAtH16(dt, state);
            case GOING_TO_RIGHT_DOOR -> tickGoingToRightDoor(dt, state);
            case AT_RIGHT_DOOR       -> tickAtRightDoor(dt, state);
            case BLOCKING_DOOR       -> tickBlockingDoor(dt, state);
            case DONE -> { }
        }
    }

    // ---------- Estados ----------

    private void tickGoingToHallway1(double dt, GameState state) {
        if ("hallway1".equals(currentSector(state))) {
            double chance = aiLerp(H16_DETOUR_CHANCE_AT_1, H16_DETOUR_CHANCE_AT_20);
            if (state.getRandom().nextDouble() < chance) {
                state.log(getDisplayName() + " se desvía hacia H16.");
                this.state = State.GOING_TO_H16;
            } else {
                this.state = State.GOING_TO_RIGHT_DOOR;
            }
            return;
        }
        advanceMove(dt, state, HALLWAY1_ENTRY);
    }

    private void tickGoingToH16(double dt, GameState state) {
        if (H16.equals(getCurrentNodeId())) {
            state.log(getDisplayName() + " se queda en H16.");
            this.state = State.WAITING_AT_H16;
            this.stateTimer = aiLerp(H16_STAY_DURATION_AT_1, H16_STAY_DURATION_AT_20);
            this.blockTimer = aiLerp(H16_BLOCK_DURATION_AT_1, H16_BLOCK_DURATION_AT_20);
            for (String s : HALLWAY_SECTORS) state.setSectorTapped(s, true);
            return;
        }
        advanceMove(dt, state, H16);
    }

    private void tickWaitingAtH16(double dt, GameState state) {
        stateTimer -= dt;
        if (stateTimer <= 0) {
            // No desbloqueamos acá: el blockTimer sigue su curso.
            state.log(getDisplayName() + " sale de H16.");
            this.state = State.GOING_TO_RIGHT_DOOR;
            this.timeSinceAttempt = moveInterval;
        }
    }

    private void tickGoingToRightDoor(double dt, GameState state) {
        if (RIGHT_DOOR.equals(getCurrentNodeId())) {
            state.log(getDisplayName() + " está en la puerta derecha.");
            this.state = State.AT_RIGHT_DOOR;
            this.doorWaitTimer = 0;
            this.doorWaitThreshold = DOOR_WAIT_MIN
                    + state.getRandom().nextDouble() * (DOOR_WAIT_MAX - DOOR_WAIT_MIN);
            return;
        }
        advanceMove(dt, state, RIGHT_DOOR);
    }

    private double closedTime = 0.0;

    private void tickAtRightDoor(double dt, GameState state) {
        boolean doorOpen = !state.getDoors().isBlocked(RIGHT_DOOR);

        if (doorOpen) {
            closedTime = 0;
            doorWaitTimer += dt;
            if (doorWaitTimer >= doorWaitThreshold) {
                state.log(getDisplayName() + " bloquea la puerta derecha.");
                this.state = State.BLOCKING_DOOR;
                this.killTimer = 0;
            }
        } else {
            closedTime += dt;
            if (closedTime >= DOOR_CLOSED_LEAVE_TIME) {
                state.log(getDisplayName() + " se retira de la puerta derecha.");
                respawn(state);
            }
        }
    }

    private void tickBlockingDoor(double dt, GameState state) {
        killTimer += dt;
        if (killTimer >= DOOR_KILL_DELAY) {
            String officeId = state.getMap().getOfficeNodeId();
            if (officeId != null) state.moveAnimatronic(this, officeId);
            state.triggerGameOver(getDisplayName() + " ha entrado por la puerta derecha");
            this.state = State.DONE;
        }
    }

    // ---------- Movimiento con salto ----------

    private void advanceMove(double dt, GameState state, String targetId) {
        timeSinceAttempt += dt;
        if (timeSinceAttempt < moveInterval) return;

        if (!passesAiRoll()) {
            timeSinceAttempt = 0;
            moveInterval = rollInterval(state);
            return;
        }

        boolean moved = tryMoveWithSkip(targetId, state);
        if (moved) {
            timeSinceAttempt = 0;
        } else {
            timeSinceAttempt = Math.max(0, timeSinceAttempt - 1.0);
        }
    }

    private boolean tryMoveWithSkip(String targetId, GameState state) {
        List<String> path = state.getMap().shortestPath(getCurrentNodeId(), targetId, AVOID);
        if (path.size() < 2) return false;

        String next = path.get(1);
        if (!isBlocked(next, state)) {
            state.moveAnimatronic(this, next);
            moveInterval = rollInterval(state);
            return true;
        }

        if (path.size() >= 3) {
            String skip = path.get(2);
            if (!isBlocked(skip, state)) {
                state.moveAnimatronic(this, skip);
                moveInterval = rollInterval(state) * SKIP_INTERVAL_MULT;
                return true;
            }
        }

        // Ambos bloqueados: retroceder
        String prev = getPreviousNodeId();
        if (prev != null && state.canEnter(getId(), prev)) {
            state.moveAnimatronic(this, prev);
            moveInterval = rollInterval(state);
            return true;
        }
        return false;
    }


    private boolean isBlocked(String nodeId, GameState state) {
        if (!state.canEnter(getId(), nodeId)) return true;
        return state.isNodeOccupiedByOther(nodeId, getId());
    }

    // ---------- Helpers ----------

    private double rollInterval(GameState state) {
        double obsMult = isObserved(state) ? OBSERVED_MULT : 1.0;
        return rollInterval(MOVE_BASE) * obsMult;
    }

    private boolean isObserved(GameState state) {
        String sector = currentSector(state);
        return sector != null && sector.equals(state.getActiveSectorId());
    }

    private String currentSector(GameState state) {
        Node n = state.getMap().getNode(getCurrentNodeId());
        return n == null ? null : n.getSectorId();
    }

    private void respawn(GameState state) {
        state.moveAnimatronic(this, getRespawnNodeId());
        this.state = State.GOING_TO_HALLWAY1;
        this.doorWaitTimer = 0;
        this.killTimer = 0;
        this.timeSinceAttempt = 0;
        this.moveInterval = rollInterval(MOVE_BASE);
    }
}
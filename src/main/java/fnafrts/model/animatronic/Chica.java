package fnafrts.model.animatronic;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import fnafrts.core.GameState;
import fnafrts.model.graph.Node;

public class Chica extends Animatronic {

    private static final double MOVE_BASE = 7.5;
    private static final double OBSERVED_MULT = 1.15;
    private static final double PIZZA_SPEED_MULT = 0.8;

    private static final double KITCHEN_MIN = 15.0;
    private static final double KITCHEN_MAX = 25.0;
    private static final double KITCHEN_ATTENTION_PER_SEC = 1.0;

    private static final double PIZZA_COOK_MIN = 13.0;
    private static final double PIZZA_COOK_MAX = 22.0;

    private static final double DOOR_OPEN_BLOCK_TIME = 4.0;
    private static final double DOOR_CLOSED_LEAVE_TIME = 3.0;
    private static final double DOOR_OPEN_GRACE_TIME = 1.0;
    private static final double BLOCK_MIN = 3.0;
    private static final double BLOCK_MAX = 6.5;

    private static final String KITCHEN_SECTOR = "kitchen";
    private static final String KITCHEN_ENTRY  = "K1";
    private static final String LEFT_DOOR      = "PI";

    public enum State {
        GOING_TO_KITCHEN,
        IN_KITCHEN,
        COOKING_PIZZA,
        GOING_TO_OFFICE,
        AT_LEFT_DOOR,
        BLOCKING_DOOR,
        DONE
    }

    private State state = State.GOING_TO_KITCHEN;
    private double stateTimer = 0.0;
    private double speedMultiplier = 1.0;

    private String kitchenTarget = null;

    private double openTime = 0.0;
    private double closedTime = 0.0;
    private double graceTimer = 0.0;
    private boolean graceActive = false;
    private boolean prevDoorOpen = false;

    private double blockTimer = 0.0;
    private double blockThreshold = 0.0;

    public Chica(String id, String displayName, String symbol, Color color,
                 String homeNodeId, String respawnNodeId, Random rng) {
        super(id, displayName, symbol, color, homeNodeId, respawnNodeId, rng);
        this.moveInterval = rollInterval(MOVE_BASE);
    }

    public State getState() { return state; }

    @Override
    public boolean isForcedVisible() {
        return state == State.BLOCKING_DOOR;
    }

    @Override
    public boolean canAcceptPizzaOrder() {
        return state == State.IN_KITCHEN;
    }

    @Override
    public String getBlockedDoorNodeId() {
        return state == State.BLOCKING_DOOR ? LEFT_DOOR : null;
    }

    @Override
    public boolean acceptPizzaOrder(GameState gameState) {
        if (state != State.IN_KITCHEN) return false;
        state = State.COOKING_PIZZA;
        stateTimer = PIZZA_COOK_MIN + gameState.getRandom().nextDouble()
                * (PIZZA_COOK_MAX - PIZZA_COOK_MIN);
        kitchenTarget = null;
        return true;
    }

    @Override
    public void tick(double dt, GameState state) {
        switch (this.state) {
            case GOING_TO_KITCHEN -> tickGoingToKitchen(dt, state);
            case IN_KITCHEN       -> tickInKitchen(dt, state);
            case COOKING_PIZZA    -> tickCookingPizza(dt, state);
            case GOING_TO_OFFICE  -> tickGoingToOffice(dt, state);
            case AT_LEFT_DOOR     -> tickAtLeftDoor(dt, state);
            case BLOCKING_DOOR    -> tickBlockingDoor(dt, state);
            case DONE -> { }
        }
    }

    private void tickGoingToKitchen(double dt, GameState state) {
        if (KITCHEN_SECTOR.equals(currentSector(state))) {
            state.log(getDisplayName() + " ha entrado a la cocina.");
            this.state = State.IN_KITCHEN;
            this.stateTimer = KITCHEN_MIN + state.getRandom().nextDouble()
                    * (KITCHEN_MAX - KITCHEN_MIN);
            this.kitchenTarget = null;
            return;
        }
        advanceMove(dt, state, KITCHEN_ENTRY);
    }

    private void tickInKitchen(double dt, GameState state) {
        state.getAttention().contribute(KITCHEN_ATTENTION_PER_SEC);

        stateTimer -= dt;
        if (stateTimer <= 0) {
            leaveKitchen(state, false);
            return;
        }

        if (kitchenTarget == null || getCurrentNodeId().equals(kitchenTarget)) {
            kitchenTarget = pickRandomKitchenNode(state);
        }
        advanceMove(dt, state, kitchenTarget);
    }

    private void tickCookingPizza(double dt, GameState state) {
        stateTimer -= dt;
        if (stateTimer <= 0) {
            leaveKitchen(state, true);
        }
    }

    private void tickGoingToOffice(double dt, GameState state) {
        if (LEFT_DOOR.equals(getCurrentNodeId())) {
            state.log(getDisplayName() + " está en la puerta izquierda.");
            this.state = State.AT_LEFT_DOOR;
            this.openTime = 0;
            this.closedTime = 0;
            this.prevDoorOpen = false;
            this.graceActive = false;
            this.graceTimer = 0;
            return;
        }
        advanceMove(dt, state, LEFT_DOOR);
    }

    private void tickAtLeftDoor(double dt, GameState state) {
        boolean doorOpen = !state.getDoors().isBlocked(LEFT_DOOR);

        if (doorOpen && !prevDoorOpen && closedTime > 0) {
            graceActive = true;
            graceTimer = 0;
        }
        prevDoorOpen = doorOpen;

        if (graceActive) {
            graceTimer += dt;
            if (graceTimer >= DOOR_OPEN_GRACE_TIME) {
                enterOfficeAndKill(state, "la puerta izquierda");
                return;
            }
            if (!doorOpen) {
                graceActive = false;
            }
            return;
        }

        if (doorOpen) {
            openTime += dt;
            if (openTime >= DOOR_OPEN_BLOCK_TIME) {
                state.log(getDisplayName() + " bloquea la puerta izquierda.");
                this.state = State.BLOCKING_DOOR;
                this.blockTimer = 0;
                this.blockThreshold = BLOCK_MIN + state.getRandom().nextDouble()
                        * (BLOCK_MAX - BLOCK_MIN);
            }
        } else {
            closedTime += dt;
            if (closedTime >= DOOR_CLOSED_LEAVE_TIME) {
                state.log(getDisplayName() + " se retira de la puerta izquierda.");
                respawn(state);
            }
        }
    }

    private void tickBlockingDoor(double dt, GameState state) {
        blockTimer += dt;
        if (blockTimer >= blockThreshold) {
            enterOfficeAndKill(state, "la puerta izquierda");
        }
    }

    private void leaveKitchen(GameState state, boolean afterPizza) {
        state.log(getDisplayName() + " sale de la cocina.");
        this.state = State.GOING_TO_OFFICE;
        this.speedMultiplier = afterPizza ? PIZZA_SPEED_MULT : 1.0;
        this.timeSinceAttempt = moveInterval;
    }

    private void advanceMove(double dt, GameState state, String targetId) {
        timeSinceAttempt += dt;
        if (timeSinceAttempt < moveInterval) return;

        if (!passesAiRoll()) {
            // Falló la tirada: reintenta en el próximo intervalo
            timeSinceAttempt = 0;
            moveInterval = rollInterval(state);
            return;
        }

        if (moveToward(targetId, state)) {
            timeSinceAttempt = 0;
            moveInterval = rollInterval(state);
        } else {
            timeSinceAttempt = Math.max(0, moveInterval - 1.0);
        }
    }

    private boolean moveToward(String targetId, GameState state) {
        List<String> path = state.getMap().shortestPath(getCurrentNodeId(), targetId);
        if (path.size() < 2) return false;
        return moveOrRetreat(path.get(1), state);
    }

    private double rollInterval(GameState state) {
        double obsMult = isObserved(state) ? OBSERVED_MULT : 1.0;
        return rollInterval(MOVE_BASE) * obsMult * speedMultiplier;
    }

    private boolean isObserved(GameState state) {
        String sector = currentSector(state);
        return sector != null && sector.equals(state.getActiveSectorId());
    }

    private String currentSector(GameState state) {
        Node n = state.getMap().getNode(getCurrentNodeId());
        return n == null ? null : n.getSectorId();
    }

    private String pickRandomKitchenNode(GameState state) {
        List<Node> kitchen = state.getMap().getNodesOfSector(KITCHEN_SECTOR);
        if (kitchen.isEmpty()) return getCurrentNodeId();
        return kitchen.get(state.getRandom().nextInt(kitchen.size())).getId();
    }

    private void enterOfficeAndKill(GameState state, String where) {
        String officeId = state.getMap().getOfficeNodeId();
        if (officeId != null) state.moveAnimatronic(this, officeId);
        state.triggerGameOver(getDisplayName() + " ha entrado por " + where);
        this.state = State.DONE;
    }

    private void respawn(GameState state) {
        state.moveAnimatronic(this, getRespawnNodeId());
        this.state = State.GOING_TO_KITCHEN;
        this.stateTimer = 0;
        this.openTime = 0;
        this.closedTime = 0;
        this.graceActive = false;
        this.graceTimer = 0;
        this.blockTimer = 0;
        this.prevDoorOpen = false;
        this.speedMultiplier = 1.0;
        this.timeSinceAttempt = 0;
        this.moveInterval = rollInterval(MOVE_BASE);
    }
}
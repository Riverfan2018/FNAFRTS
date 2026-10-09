package fnafrts.model.animatronic;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import fnafrts.core.GameState;

public class Endo extends Animatronic {

    private static final double EL_MOVE_BASE = 32.0;

    private static final double SHOCK_WINDOW_MIN = 10.0;
    private static final double SHOCK_WINDOW_MAX = 14.0;

    private static final double HURRIED_MOVE_BASE = 5.0;
    private static final double HURRIED_DOOR_KILL_TIME = 2.0;

    private static final double SILENT_DOOR_KILL_TIME = 5.0;

    private static final double OFF_TICK_INTERVAL = 11.0;
    private static final double OFF_WAKE_CHANCE = 0.35;

    private static final String EL2 = "EL2";
    private static final String EL1 = "EL1";
    private static final String EL3 = "EL3";
    private static final String EL4 = "EL4";
    private static final String O4  = "O4";
    private static final String PI  = "PI";

    private static final double LIGHT_KILL_TIME = 0.1;

    public enum State {
        WANDERING,
        SHOCK_WINDOW,
        HURRIED,
        HURRIED_AT_DOOR,
        SILENT_AT_DOOR,
        OFF,
        DONE
    }

    private State state = State.WANDERING;
    private double moveTimer;
    private double stateTimer;
    private double offTimer;
    private double lightKillTimer = 0.0;

    public Endo(String id, String displayName, String symbol, Color color,
                String homeNodeId, Random rng) {
        super(id, displayName, symbol, color, homeNodeId, homeNodeId, rng);
        this.moveTimer = rollInterval(EL_MOVE_BASE);
    }

    public State getState() { return state; }

    @Override
    protected double aiProbability() {
        int lvl = getAiLevel();
        if (lvl <= 0) return 0.0;
        return aiLerp(0.72, 0.98);
    }

    @Override
    public boolean canBeShocked() {
        return state == State.SHOCK_WINDOW;
    }

    @Override
    public boolean acceptShock(GameState state) {
        if (this.state != State.SHOCK_WINDOW) return false;
        this.state = State.WANDERING;
        this.stateTimer = 0;
        this.moveTimer = rollInterval(EL_MOVE_BASE);
        state.moveAnimatronic(this, EL2);
        return true;
    }

    // ---------- Tick ----------

    @Override
    public void tick(double dt, GameState gameState) {
        switch (this.state) {
            case WANDERING       -> tickWandering(dt, gameState);
            case SHOCK_WINDOW    -> tickShockWindow(dt, gameState);
            case HURRIED         -> tickHurried(dt, gameState);
            case HURRIED_AT_DOOR -> tickHurriedAtDoor(dt, gameState);
            case SILENT_AT_DOOR  -> tickSilentAtDoor(dt, gameState);
            case OFF             -> tickOff(dt, gameState);
            case DONE -> { }
        }
    }

    // ---------- WANDERING ----------

    private void tickWandering(double dt, GameState gameState) {
        moveTimer -= dt;
        if (moveTimer > 0) return;
        moveTimer = rollInterval(EL_MOVE_BASE);

        if (!passesAiRoll()) return;

        String current = getCurrentNodeId();
        String next;
        if (EL2.equals(current)) {
            next = rng.nextBoolean() ? EL1 : EL4;
        } else if (EL1.equals(current)) {
            next = EL3;
        } else {
            next = EL2;
        }

        moveOrRetreat(next, gameState);

        String actual = getCurrentNodeId();
        if (EL3.equals(actual) || EL4.equals(actual)) {
            enterShockWindow(gameState);
        }
    }

    private void enterShockWindow(GameState gameState) {
        gameState.log(getDisplayName() + " está expuesto en " + getCurrentNodeId() + ".");
        this.state = State.SHOCK_WINDOW;
        this.stateTimer = SHOCK_WINDOW_MIN + rng.nextDouble() * (SHOCK_WINDOW_MAX - SHOCK_WINDOW_MIN);
    }

    // ---------- SHOCK_WINDOW ----------

    private void tickShockWindow(double dt, GameState gameState) {
        stateTimer -= dt;
        if (stateTimer > 0) return;

        if (EL3.equals(getCurrentNodeId())) {
            gameState.log(getDisplayName() + " escapa por el pasillo.");
            this.state = State.HURRIED;
            this.moveTimer = 0;
        } else {
            gameState.log(getDisplayName() + " desaparece en silencio.");
            gameState.moveAnimatronic(this, PI);
            this.state = State.SILENT_AT_DOOR;
            this.stateTimer = SILENT_DOOR_KILL_TIME;
        }
    }

    // ---------- HURRIED ----------

    private void tickHurried(double dt, GameState gameState) {
        if (PI.equals(getCurrentNodeId())) {
            gameState.log(getDisplayName() + " está en la puerta izquierda.");
            this.state = State.HURRIED_AT_DOOR;
            this.stateTimer = 0;
            return;
        }

        moveTimer -= dt;
        if (moveTimer > 0) return;
        moveTimer = rollInterval(HURRIED_MOVE_BASE);

        List<String> path = gameState.getMap().shortestPath(getCurrentNodeId(), PI);
        if (path.size() < 2) return;

        int idx = Math.min(2, path.size() - 1);
        String target = path.get(idx);

        if (!gameState.canEnter(getId(), target)) {
            target = path.get(1);
            if (!gameState.canEnter(getId(), target)) {
                String prev = getPreviousNodeId();
                if (prev != null && gameState.canEnter(getId(), prev)) {
                    gameState.moveAnimatronic(this, prev);
                }
                return;
            }
        }
        
        gameState.moveAnimatronic(this, target);

        if (PI.equals(target)) {
            gameState.log(getDisplayName() + " está en la puerta izquierda.");
            this.state = State.HURRIED_AT_DOOR;
            this.stateTimer = 0;
        }
    }

    private void tickHurriedAtDoor(double dt, GameState gameState) {
        if (checkLightKill(dt, gameState)) return;

        stateTimer += dt;

        boolean doorOpen = !gameState.getDoors().isBlocked(PI);
        if (!doorOpen) {
            gameState.log(getDisplayName() + " bloqueado, vuelve a O4.");
            gameState.moveAnimatronic(this, O4);
            this.state = State.OFF;
            this.offTimer = 0;
            return;
        }

        if (stateTimer >= HURRIED_DOOR_KILL_TIME) {
            String officeId = gameState.getMap().getOfficeNodeId();
            if (officeId != null) gameState.moveAnimatronic(this, officeId);
            gameState.triggerGameOver(getDisplayName() + " te atrapó en la puerta izquierda");
            this.state = State.DONE;
        }
    }

    // ---------- SILENT_AT_DOOR ----------

    private void tickSilentAtDoor(double dt, GameState gameState) {
        if (checkLightKill(dt, gameState)) return;

        stateTimer -= dt;

        boolean doorOpen = !gameState.getDoors().isBlocked(PI);
        if (!doorOpen) {
            gameState.log(getDisplayName() + " bloqueado, vuelve a EL2.");
            gameState.moveAnimatronic(this, EL2);
            this.state = State.WANDERING;
            this.moveTimer = rollInterval(EL_MOVE_BASE);
            this.stateTimer = 0;
            return;
        }

        if (stateTimer <= 0) {
            String officeId = gameState.getMap().getOfficeNodeId();
            if (officeId != null) gameState.moveAnimatronic(this, officeId);
            gameState.triggerGameOver(getDisplayName() + " te atrapó silenciosamente");
            this.state = State.DONE;
        }
    }

    // ---------- OFF ----------

    private void tickOff(double dt, GameState gameState) {
        offTimer += dt;
        if (offTimer < OFF_TICK_INTERVAL) return;
        offTimer = 0;

        if (rng.nextDouble() < OFF_WAKE_CHANCE) {
            gameState.log(getDisplayName() + " se reactiva.");
            this.state = State.HURRIED;
            this.moveTimer = 0;
        }
    }

    // ---------- Helpers ----------

    private boolean checkLightKill(double dt, GameState gameState) {
        boolean lightOn = gameState.getDoors().isLeftLightOn();
        boolean doorOpen = !gameState.getDoors().isBlocked(PI);

        if (!lightOn || !doorOpen) {
            lightKillTimer = 0.0;
            return false;
        }

        lightKillTimer += dt;
        if (lightKillTimer < LIGHT_KILL_TIME) return false;

        String officeId = gameState.getMap().getOfficeNodeId();
        if (officeId != null) gameState.moveAnimatronic(this, officeId);
        gameState.triggerGameOver(getDisplayName() + " te atrapó al iluminarlo");
        this.state = State.DONE;
        return true;
    }
}
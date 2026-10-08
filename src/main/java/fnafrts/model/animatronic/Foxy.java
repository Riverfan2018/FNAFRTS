package fnafrts.model.animatronic;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import fnafrts.core.GameState;
import fnafrts.model.graph.Node;

public class Foxy extends Animatronic {

    private static final double TIMER_DOWN_RATE = 1.5;
    private static final double UP_RATE_AT_1  = 0.9;
    private static final double UP_RATE_AT_20 = 2.25;
    private static final double TARGET_AT_1  = 36.0;
    private static final double TARGET_AT_20 = 20.0;

    private static final double RUSH_NODE_TIME = 0.5;
    private static final double RUSH_SKIP_TIME = 0.8;

    private static final String[] ENTRANCE_PATH = { "E1", "E2", "E3", "E4", "E5" };
    private static final String RIGHT_DOOR = "PD";

    public enum State {
        ENTRANCE,
        RUSH,
        DONE
    }

    private State state = State.ENTRANCE;
    private double timer = 0.0;
    private double rushTimer = 0.0;
    private int entranceIndex = 0;
    private int blockCount = 0;

    public Foxy(String id, String displayName, String symbol, Color color,
                String homeNodeId, Random rng) {
        super(id, displayName, symbol, color, homeNodeId, homeNodeId, rng);
    }

    public State getState() { return state; }

    @Override
    public void tick(double dt, GameState gameState) {
        switch (this.state) {
            case ENTRANCE -> tickEntrance(dt, gameState);
            case RUSH     -> tickRush(dt, gameState);
            case DONE -> { }
        }
    }

    private void tickEntrance(double dt, GameState gameState) {
        if (isObserved(gameState)) {
            timer -= TIMER_DOWN_RATE * dt;
            if (timer < 0) timer = 0;
        } else {
            timer += aiUpRate() * dt;
        }

        if (timer < aiTimerTarget()) return;
        timer = 0;

        if (entranceIndex >= ENTRANCE_PATH.length - 1) {
            gameState.log(getDisplayName() + " sale disparado hacia la puerta derecha.");
            this.state = State.RUSH;
            this.rushTimer = 0;
            return;
        }

        entranceIndex++;
        String nextNode = ENTRANCE_PATH[entranceIndex];
        gameState.moveAnimatronic(this, nextNode);
    }

    private void tickRush(double dt, GameState gameState) {
        if (RIGHT_DOOR.equals(getCurrentNodeId())) {
            boolean doorOpen = !gameState.getDoors().isBlocked(RIGHT_DOOR);
            if (doorOpen) {
                String officeId = gameState.getMap().getOfficeNodeId();
                if (officeId != null) gameState.moveAnimatronic(this, officeId);
                gameState.triggerGameOver(getDisplayName() + " ha entrado por la puerta derecha");
                this.state = State.DONE;
            } else {
                gameState.log(getDisplayName() + " fue bloqueado en la puerta derecha.");
                gameState.getAttention().raise(5.0);
                doRespawn(gameState);
            }
            return;
        }

        rushTimer += dt;

        List<String> path = gameState.getMap().shortestPath(getCurrentNodeId(), RIGHT_DOOR);
        if (path.size() < 2) return;

        String next = path.get(1);
        boolean blockedNext = gameState.isNodeOccupiedByOther(next, getId());

        double stepTime = blockedNext ? RUSH_SKIP_TIME : RUSH_NODE_TIME;
        if (rushTimer < stepTime) return;
        rushTimer -= stepTime;

        String target;
        if (blockedNext) {
            if (path.size() < 3) return;
            String skip = path.get(2);
            if (gameState.isNodeOccupiedByOther(skip, getId())) return;
            target = skip;
        } else {
            target = next;
        }

        if (!gameState.canEnter(getId(), target)) return;
        gameState.moveAnimatronic(this, target);
    }

    private void doRespawn(GameState gameState) {
        blockCount++;
        String respawnNode;
        if (blockCount == 1) {
            respawnNode = "E2";
            entranceIndex = 1;
        } else {
            respawnNode = "E3";
            entranceIndex = 2;
        }
        gameState.moveAnimatronic(this, respawnNode);
        this.state = State.ENTRANCE;
        this.timer = 0;
        this.rushTimer = 0;
    }

    private boolean isObserved(GameState gameState) {
        Node n = gameState.getMap().getNode(getCurrentNodeId());
        if (n == null) return false;
        String sector = n.getSectorId();
        return sector != null && sector.equals(gameState.getActiveSectorId());
    }

    private double aiUpRate() {
        int lvl = getAiLevel();
        if (lvl <= 0) return 0.0;
        if (lvl >= 20) return UP_RATE_AT_20;
        return UP_RATE_AT_1 + (lvl - 1) * (UP_RATE_AT_20 - UP_RATE_AT_1) / 19.0;
    }

    private double aiTimerTarget() {
        int lvl = getAiLevel();
        if (lvl <= 0) return TARGET_AT_1;
        if (lvl >= 20) return TARGET_AT_20;
        return TARGET_AT_1 + (lvl - 1) * (TARGET_AT_20 - TARGET_AT_1) / 19.0;
    }
}
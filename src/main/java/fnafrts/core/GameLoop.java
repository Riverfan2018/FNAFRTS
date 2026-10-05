package fnafrts.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameLoop {

    private static final long TICK_MS = 100L;
    private static final double TICK_S = TICK_MS / 1000.0;

    private final GameState state;
    private final ScheduledExecutorService exec;
    private final double timeScale;

    public GameLoop(GameState state) {
        this(state, 1.0);
    }

    public GameLoop(GameState state, double timeScale) {
        this.state = state;
        this.timeScale = timeScale;
        this.exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "game-loop");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        exec.scheduleAtFixedRate(this::safeTick, 0, TICK_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        exec.shutdownNow();
    }

    private void safeTick() {
        try {
            state.tick(TICK_S * timeScale);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
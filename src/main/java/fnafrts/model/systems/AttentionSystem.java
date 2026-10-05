package fnafrts.model.systems;

public class AttentionSystem {

    public static final double MAX_LEVEL = 100.0;

    private static final double RATE_ONE_LIGHT  = +0.75;
    private static final double RATE_TWO_LIGHTS = +1.50;
    private static final double RATE_IDLE       = -1.00;

    private double level = 0.0;
    private double framePositiveRate = 0.0;

    public double getLevel() { return level; }
    public double getRatio() { return level / MAX_LEVEL; }

    /** Subida instantánea puntual. Ignora valores negativos. */
    public void raise(double amount) {
        if (amount <= 0) return;
        level = Math.min(MAX_LEVEL, level + amount);
    }

    /**
     * Registra un aporte continuo (por segundo) para el frame actual.
     * Estos aportes sobrescriben el decay base.
     * Llamar SOLO desde el tick de los animatrónicos.
     */
    public void contribute(double ratePerSecond) {
        if (ratePerSecond > 0) framePositiveRate += ratePerSecond;
    }

    public void reset() {
        level = 0.0;
        framePositiveRate = 0.0;
    }

    /**
     * Actualiza la atención.
     * Si hay aportes positivos (luces + contribute), se usa la suma.
     * Si no hay ninguno, se aplica el decay de -1/s.
     */
    public void update(double dt, int lightsOn) {
        double lightRate = 0;
        if (lightsOn >= 2)      lightRate = RATE_TWO_LIGHTS;
        else if (lightsOn == 1) lightRate = RATE_ONE_LIGHT;

        double positive = lightRate + framePositiveRate;
        double rate = positive > 0 ? positive : RATE_IDLE;

        level += rate * dt;
        if (level < 0)         level = 0;
        if (level > MAX_LEVEL) level = MAX_LEVEL;

        framePositiveRate = 0;
    }
}
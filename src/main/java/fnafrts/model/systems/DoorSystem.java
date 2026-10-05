package fnafrts.model.systems;

public class DoorSystem {

    public static final double MAX_HEAT = 40.0;

    private static final double RATE_ONE_CLOSED  = +1.00;
    private static final double RATE_BOTH_CLOSED = +2.25;
    private static final double RATE_NONE_CLOSED = -1.75;
    private static final double RATE_COOLDOWN    = -0.75;

    private final String leftEntryNodeId;
    private final String rightEntryNodeId;

    private DoorState leftState  = DoorState.OPEN;
    private DoorState rightState = DoorState.OPEN;
    private boolean leftLightOn  = false;
    private boolean rightLightOn = false;

    private double heat = 0.0;
    private boolean overheated = false;

    public DoorSystem(String leftEntryNodeId, String rightEntryNodeId) {
        this.leftEntryNodeId = leftEntryNodeId;
        this.rightEntryNodeId = rightEntryNodeId;
    }

    public String getLeftEntryNodeId()  { return leftEntryNodeId; }
    public String getRightEntryNodeId() { return rightEntryNodeId; }

    public DoorState getLeftState()  { return leftState; }
    public DoorState getRightState() { return rightState; }
    public boolean isLeftLightOn()   { return leftLightOn; }
    public boolean isRightLightOn()  { return rightLightOn; }

    public double getHeat() { return heat; }
    public boolean isOverheated() { return overheated; }

    public void toggleLeft() {
        if (overheated) return;
        leftState = (leftState == DoorState.OPEN) ? DoorState.CLOSED : DoorState.OPEN;
    }

    public void toggleRight() {
        if (overheated) return;
        rightState = (rightState == DoorState.OPEN) ? DoorState.CLOSED : DoorState.OPEN;
    }

    public void toggleLeftLight()  { leftLightOn  = !leftLightOn; }
    public void toggleRightLight() { rightLightOn = !rightLightOn; }

    public boolean isBlocked(String entryNodeId) {
        if (entryNodeId.equals(leftEntryNodeId))  return leftState  == DoorState.CLOSED;
        if (entryNodeId.equals(rightEntryNodeId)) return rightState == DoorState.CLOSED;
        throw new IllegalArgumentException("Nodo no es entrada de oficina: " + entryNodeId);
    }

    public void update(double deltaSeconds) {
        if (overheated) {
            heat += RATE_COOLDOWN * deltaSeconds;
            if (heat <= 0.0) {
                heat = 0.0;
                overheated = false;
                leftState = DoorState.OPEN;
                rightState = DoorState.OPEN;
            }
            return;
        }

        int closedCount = 0;
        if (leftState  == DoorState.CLOSED) closedCount++;
        if (rightState == DoorState.CLOSED) closedCount++;

        double rate = switch (closedCount) {
            case 2 -> RATE_BOTH_CLOSED;
            case 1 -> RATE_ONE_CLOSED;
            default -> RATE_NONE_CLOSED;
        };

        heat += rate * deltaSeconds;
        if (heat < 0.0) heat = 0.0;

        if (heat >= MAX_HEAT) {
            heat = MAX_HEAT;
            overheated = true;
            leftState = DoorState.OVERHEATED;
            rightState = DoorState.OVERHEATED;
        }
    }

    /** Sube el calor de golpe. Se usa para eventos puntuales (shock, etc.). */
    public void raiseHeat(double amount) {
        if (amount <= 0) return;
        heat = Math.min(MAX_HEAT, heat + amount);
        if (heat >= MAX_HEAT && !overheated) {
            overheated = true;
            leftState = DoorState.OVERHEATED;
            rightState = DoorState.OVERHEATED;
        }
    }
}
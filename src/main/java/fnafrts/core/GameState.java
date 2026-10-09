package fnafrts.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import fnafrts.model.animatronic.Animatronic;
import fnafrts.model.graph.MapGraph;
import fnafrts.model.graph.Node;
import fnafrts.model.graph.NodeType;
import fnafrts.model.systems.AttentionSystem;
import fnafrts.model.systems.DoorSystem;

public class GameState {

    private static final double SECONDS_PER_HOUR = 45.0;
    private static final int FINAL_HOUR = 6;
    private static final int LOG_CAPACITY = 50;
    private static final long CAMERA_COOLDOWN_MS = 1500L;
    private static final long INPUT_BUFFER_MS = 250L;

    private String bufferedSectorId = null;
    private long bufferedSectorExpiryMs = 0;

    private final MapGraph map;
    private final DoorSystem doors;
    private final AttentionSystem attention = new AttentionSystem();
    private final Map<String, Animatronic> animatronics = new LinkedHashMap<>();
    private final Map<String, Set<String>> nodeOccupancy = new HashMap<>();
    private final List<String> eventLog = new ArrayList<>();

    private final Object lock = new Object();
    private final Random random;
    private final Set<String> blockedCameraSectors = new HashSet<>();

    private volatile String activeSectorId;

    private double elapsedSeconds = 0.0;
    private int hour = 0;
    private boolean victory = false;
    private boolean gameOver = false;
    private long lastSectorChangeMs = 0;
    private String gameOverReason;

    public GameState(MapGraph map) {
        this(map, new Random());
    }

    public GameState(MapGraph map, Random random) {
        this.map = map;
        this.doors = new DoorSystem(map.getLeftEntryNodeId(), map.getRightEntryNodeId());
        this.random = random;
    }

    public String getActiveSectorId() { return activeSectorId; }
    public long getLastSectorChangeMs() { return lastSectorChangeMs; }
    public static long getCameraCooldownMs() { return CAMERA_COOLDOWN_MS; }

    public void setInitialSector(String id) {
        synchronized (lock) { activeSectorId = id; }
    }

    public boolean trySetActiveSector(String id) {
        synchronized (lock) {
            if (id == null || id.equals(activeSectorId)) return false;
            if (blockedCameraSectors.contains(id)) return false;
            long now = System.currentTimeMillis();
            long remaining = CAMERA_COOLDOWN_MS - (now - lastSectorChangeMs);

            if (remaining > 0) {
                if (remaining <= INPUT_BUFFER_MS) {
                    bufferedSectorId = id;
                    bufferedSectorExpiryMs = now + INPUT_BUFFER_MS;
                }
                return false;
            }
            return applySectorChange(id, now);
        }
    }

    private boolean applySectorChange(String id, long now) {
        for (Animatronic a : animatronics.values()) {
            String seen = a.getLastSeenNodeId();
            if (seen != null) {
                Node n = map.getNode(seen);
                if (n != null && id.equals(n.getSectorId())) {
                    a.setLastSeenNodeId(null);
                }
            }
        }
        activeSectorId = id;
        lastSectorChangeMs = now;
        bufferedSectorId = null;
        return true;
    }

    public boolean isCamerasOff() {
        return activeSectorId == null;
    }

    public void turnOffCameras() {
        synchronized (lock) {
            activeSectorId = null;
            lastSectorChangeMs = System.currentTimeMillis();
        }
    }

    /** true si algún animatrónico puede recibir una orden de pizza ahora mismo. */
    public boolean canOrderPizza() {
        synchronized (lock) {
            if (!"kitchen".equals(activeSectorId)) return false;
            for (Animatronic a : animatronics.values()) {
                if (a.canAcceptPizzaOrder()) return true;
            }
            return false;
        }
    }

    public boolean isCameraOnKitchen() {
        synchronized (lock) { return "kitchen".equals(activeSectorId); }
    }

    /** Delega la orden al primer animatrónico que la acepte. Devuelve true si alguno la tomó. */
    public boolean orderPizza() {
        synchronized (lock) {
            if (!"kitchen".equals(activeSectorId)) return false;
            for (Animatronic a : animatronics.values()) {
                if (a.acceptPizzaOrder(this)) {
                    attention.raise(3.0);
                    log("Pizza en el horno.");
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isCameraOnEmployeeLounge() {
        synchronized (lock) { return "employee".equals(activeSectorId); }
    }

    public boolean canShock() {
        synchronized (lock) {
            if (!"employee".equals(activeSectorId)) return false;
            for (Animatronic a : animatronics.values()) {
                if (a.canBeShocked()) return true;
            }
            return false;
        }
    }

    public boolean shockAnimatronic() {
        synchronized (lock) {
            if (!"employee".equals(activeSectorId)) return false;
            for (Animatronic a : animatronics.values()) {
                if (a.acceptShock(this)) {
                    doors.raiseHeat(5.0);
                    attention.raise(5.0);
                    log("Shock aplicado a " + a.getDisplayName() + ".");
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isCameraBlocked(String sectorId) {
        synchronized (lock) { return blockedCameraSectors.contains(sectorId); }
    }

    public void setCameraBlocked(String sectorId, boolean blocked) {
        synchronized (lock) {
            if (blocked) blockedCameraSectors.add(sectorId);
            else         blockedCameraSectors.remove(sectorId);
            // Si el jugador está mirando una cámara que se acaba de bloquear, lo expulsamos
            if (blocked && sectorId.equals(activeSectorId)) {
                activeSectorId = null;
                lastSectorChangeMs = System.currentTimeMillis();
            }
        }
    }

    /** true si el nodo está ocupado por alguien que no sos vos. */
    public boolean isNodeOccupiedByOther(String nodeId, String myId) {
        synchronized (lock) {
            Set<String> occ = nodeOccupancy.get(nodeId);
            if (occ == null || occ.isEmpty()) return false;
            if (occ.size() > 1) return true;
            return !occ.contains(myId);
        }
    }

    private void updateLastSeen() {
        for (Animatronic a : animatronics.values()) {
            if (!a.isActive()) continue;
            if (isAnimatronicVisibleToPlayer(a)) {
                a.setLastSeenNodeId(a.getCurrentNodeId());
                continue;
            }
            String seen = a.getLastSeenNodeId();
            if (seen != null && isNodeVisibleToPlayer(seen)) {
                a.setLastSeenNodeId(null);
            }
        }
    }

    // ---------- Getters ----------

    public MapGraph getMap() { return map; }
    public DoorSystem getDoors() { return doors; }
    public AttentionSystem getAttention() { return attention; }
    public Object getLock() { return lock; }
    public Random getRandom() { return random; }

    public static double getSecondsPerHour() { return SECONDS_PER_HOUR; }
    public int getHour() { return hour; }
    public double getElapsedSeconds() { return elapsedSeconds; }
    public boolean isVictory() { return victory; }
    public boolean isGameOver() { return gameOver; }
    public String getGameOverReason() { return gameOverReason; }
    public boolean isGameFinished() { return gameOver || victory; }

    public Map<String, Animatronic> getAnimatronics() {
        return Collections.unmodifiableMap(animatronics);
    }

    public List<String> getEventLog() {
        synchronized (lock) {
            return new ArrayList<>(eventLog);
        }
    }

    public List<String> getSectorIds() {
        return new ArrayList<>(map.getSectors().keySet());
    }

    public String getSectorDisplayName(String sectorId) {
        return map.getSector(sectorId).getDisplayName();
    }

    public List<AnimatronicView> getAnimatronicViews() {
        synchronized (lock) {
            List<AnimatronicView> views = new ArrayList<>();
            for (Animatronic a : animatronics.values()) {
                if (!a.isActive()) continue;
                views.add(new AnimatronicView(
                    a.getId(), a.getDisplayName(),
                    a.getSymbol(), a.getColor(),
                    a.getCurrentNodeId(),
                    a.getLastSeenNodeId(),
                    a.isForcedVisible()));
            }
            return views;
        }
    }

    public List<AnimatronicView> getAnimatronicsAtNode(String nodeId) {
        List<AnimatronicView> result = new ArrayList<>();
        for (AnimatronicView v : getAnimatronicViews()) {
            if (v.currentNodeId().equals(nodeId)) result.add(v);
        }
        return result;
    }

    public boolean isAnimatronicVisibleToPlayer(String id) {
        synchronized (lock) {
            Animatronic a = animatronics.get(id);
            return a != null && isAnimatronicVisibleToPlayer(a);
        }
    }

    public boolean isNodeVisibleToPlayer(String nodeId) {
        synchronized (lock) {
            Node n = map.getNode(nodeId);
            return n != null && isNodeVisibleToPlayer(n);
        }
    }

    private boolean isAnimatronicVisibleToPlayer(Animatronic a) {
        Node n = map.getNode(a.getCurrentNodeId());
        if (n == null) return false;
        if (isNodeVisibleToPlayer(n)) return true;
        // Nodos ENTRY: visibles también si el animatrónico se fuerza visible
        if (n.getType() == NodeType.ENTRY_LEFT || n.getType() == NodeType.ENTRY_RIGHT) {
            return a.isForcedVisible();
        }
        return false;
    }

    private boolean isNodeVisibleToPlayer(Node n) {
        if (n.getType() == NodeType.OFFICE) return true;
        if (n.getType() == NodeType.ENTRY_LEFT) return doors.isLeftLightOn();
        if (n.getType() == NodeType.ENTRY_RIGHT) return doors.isRightLightOn();
        String active = activeSectorId;
        return active != null && active.equals(n.getSectorId());
    }

    // ---------- Registro ----------

    public void addAnimatronic(Animatronic a) {
        synchronized (lock) {
            if (animatronics.containsKey(a.getId())) {
                throw new IllegalArgumentException("Animatrónico duplicado: " + a.getId());
            }
            animatronics.put(a.getId(), a);

            if (a.isActive()) {
                nodeOccupancy.computeIfAbsent(a.getCurrentNodeId(), k -> new HashSet<>())
                            .add(a.getId());
                log(a.getDisplayName() + " inicializado en " + a.getCurrentNodeId());
            } else {
                log(a.getDisplayName() + " desactivado (AI 0).");
            }
        }
    }

    // ---------- Consultas de ocupación ----------

    /**
     * true si el animatrónico puede pisar el nodo.
     * Un nodo que sea HOME o RESPAWN de algún animatrónico está reservado a ese animatrónico.
     * Si el mismo nodo es home y respawn del mismo animatrónico, sigue funcionando.
     */
    public boolean canEnter(String animatronicId, String nodeId) {
        synchronized (lock) {
            if (map.getNode(nodeId) == null) return false;

            // Exclusividad de home/respawn (solo animatrónicos activos)
            for (Animatronic a : animatronics.values()) {
                if (!a.isActive()) continue;
                if (nodeId.equals(a.getHomeNodeId()) || nodeId.equals(a.getRespawnNodeId())) {
                    if (!a.getId().equals(animatronicId)) return false;
                }
            }

            // Ocupación real
            Set<String> occ = nodeOccupancy.get(nodeId);
            if (occ != null && !occ.isEmpty() && !occ.contains(animatronicId)) {
                return false;
            }

            return true;
        }
    }

    // ---------- Mutaciones controladas ----------

    public void moveAnimatronic(Animatronic a, String newNodeId) {
        String oldNode = a.getCurrentNodeId();
        Set<String> oldSet = nodeOccupancy.get(oldNode);
        if (oldSet != null) {
            oldSet.remove(a.getId());
            if (oldSet.isEmpty()) nodeOccupancy.remove(oldNode);
        }
        a.setPreviousNodeId(oldNode);
        a.setCurrentNodeId(newNodeId);
        nodeOccupancy.computeIfAbsent(newNodeId, k -> new HashSet<>()).add(a.getId());
        log(a.getDisplayName() + ": " + oldNode + " -> " + newNodeId);
    }

    public void triggerGameOver(String reason) {
        if (gameOver) return;
        gameOver = true;
        gameOverReason = reason;
        log("GAME OVER: " + reason);
    }

    public void log(String msg) {
        String entry = String.format("[%02d:%02d] %s",
                hour,
                (int) ((elapsedSeconds % SECONDS_PER_HOUR) / SECONDS_PER_HOUR * 60.0),
                msg);
        eventLog.add(entry);
        if (eventLog.size() > LOG_CAPACITY) {
            eventLog.remove(0);
        }
    }

    // ---------- Tick ----------

    public void tick(double dt) {
        synchronized (lock) {
            if (gameOver || victory) return;

            doors.update(dt);

            elapsedSeconds += dt;
            int newHour = (int) (elapsedSeconds / SECONDS_PER_HOUR);
            if (newHour >= FINAL_HOUR) {
                hour = FINAL_HOUR;
                victory = true;
                log("¡6 AM! Has sobrevivido.");
                return;
            }
            if (newHour != hour) {
                hour = newHour;
                log("Han dado las " + hour + " AM.");
            }

            // Los animatrónicos tickean y contribuyen a la atención
            for (Animatronic a : animatronics.values()) {
                if (!a.isActive()) continue;
                a.tick(dt, this);
                if (gameOver) return;
            }

            // Recalcular puertas bloqueadas por animatrónicos.
            doors.setLeftBlocked(false);
            doors.setRightBlocked(false);
            for (Animatronic a : animatronics.values()) {
                if (!a.isActive()) continue;
                String blocked = a.getBlockedDoorNodeId();
                if (blocked == null) continue;
                if (blocked.equals(doors.getLeftEntryNodeId()))  doors.setLeftBlocked(true);
                if (blocked.equals(doors.getRightEntryNodeId())) doors.setRightBlocked(true);
            }

            // Refrescar "última vez visto" para los animatrónicos visibles en el sector activo.
            updateLastSeen();
            
            // Procesar input en buffer (si quedó algo pendiente)
            if (bufferedSectorId != null) {
                long now = System.currentTimeMillis();
                if (now > bufferedSectorExpiryMs) {
                    bufferedSectorId = null;
                } else {
                    long remaining = CAMERA_COOLDOWN_MS - (now - lastSectorChangeMs);
                    if (remaining <= 0
                            && !blockedCameraSectors.contains(bufferedSectorId)
                            && !bufferedSectorId.equals(activeSectorId)) {
                        applySectorChange(bufferedSectorId, now);
                    }
                }
            }

            // Ahora sí, la atención se actualiza con todo lo acumulado
            int lightsOn = 0;
            if (doors.isLeftLightOn())  lightsOn++;
            if (doors.isRightLightOn()) lightsOn++;
            attention.update(dt, lightsOn);
        }
    }
}
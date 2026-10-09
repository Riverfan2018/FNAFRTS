package fnafrts.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JPanel;

import fnafrts.core.AnimatronicView;
import fnafrts.core.GameState;
import fnafrts.model.graph.Node;
import fnafrts.model.graph.NodeType;
import fnafrts.model.graph.Sector;

public class MapPanel extends JPanel {

    private static final int GAP_PX_X           = 16;
    private static final int GAP_PX_Y           = 25;
    private static final int SECTOR_HEADER_PX   = 5;
    private static final int NORMAL_CELL_RATIO  = 78;
    private static final int OFFICE_CELL_RATIO  = 100;
    private static final int PADDING            = 40;
    private static final float DIM_FACTOR       = 0.35f;
    private static final long  PULSE_PERIOD_MS  = 2200L;

    // Cámaras
    private static final long STATIC_DURATION_MS  = 320L;
    private static final int  STATIC_STEP_PX      = 3;
    private static final int  COOLDOWN_CIRCLE_PX  = 48;
    private static final int  COOLDOWN_MARGIN_PX  = 18;

    /** Sectores cuyo nombre no se dibuja (el nodo o el contexto lo hacen obvio). */
    private static final java.util.Set<String> HIDDEN_SECTOR_NAMES = java.util.Set.of(
            "office",
            "hallway1",
            "hallway2"
    );

    private final GameState state;
    private final Map<String, Rectangle> nodeBounds = new HashMap<>();
    private int cellSize = 60;
    private String hoveredSectorId = null;
    private String lastRenderedActiveSector = null;
    private long staticStartMs = 0;
    private int lastW = -1;
    private int lastH = -1;

    public MapPanel(GameState state, String initialSectorId) {
        this.state = state;
        state.setInitialSector(initialSectorId);
        setBackground(Theme.BG_DEEP);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String sectorId = sectorAt(e.getX(), e.getY());
                if (sectorId != null) {
                    state.trySetActiveSector(sectorId);
                    // La estática se dispara cuando el cambio se aplica,
                    // no cuando se clickea. Ver paintComponent.
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                String sectorId = sectorAt(e.getX(), e.getY());
                int cursor = (sectorId != null)
                        ? Cursor.HAND_CURSOR
                        : Cursor.DEFAULT_CURSOR;
                if (getCursor().getType() != cursor) {
                    setCursor(Cursor.getPredefinedCursor(cursor));
                }
                if (!java.util.Objects.equals(sectorId, hoveredSectorId)) {
                    hoveredSectorId = sectorId;
                    repaint();
                }
            }
        });
    }

    // ---------- Layout ----------

    private void recomputePositionsIfNeeded() {
        if (getWidth() == lastW && getHeight() == lastH && !nodeBounds.isEmpty()) return;
        lastW = getWidth();
        lastH = getHeight();
        recomputePositions();
    }

    private void recomputePositions() {
        nodeBounds.clear();
        var allNodes = state.getMap().getNodes().values();
        if (allNodes.isEmpty()) return;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (Node n : allNodes) {
            int wx = state.getMap().getWorldX(n.getId());
            int wy = state.getMap().getWorldY(n.getId());
            minX = Math.min(minX, wx);
            maxX = Math.max(maxX, wx);
            minY = Math.min(minY, wy);
            maxY = Math.max(maxY, wy);
        }
        int spanX = maxX - minX + 1;
        int spanY = maxY - minY + 1;

        int availW = getWidth()  - 2 * PADDING;
        int availH = getHeight() - 2 * PADDING;
        if (availW <= 0 || availH <= 0) return;

        int cellW = (availW - (spanX - 1) * GAP_PX_X) / spanX;
        int cellH = (availH - (spanY - 1) * GAP_PX_Y) / spanY;
        cellSize = Math.max(12, Math.min(cellW, cellH));

        int pitchX = cellSize + GAP_PX_X;
        int pitchY = cellSize + GAP_PX_Y;

        int totalW = spanX * cellSize + (spanX - 1) * GAP_PX_X;
        int totalH = spanY * cellSize + (spanY - 1) * GAP_PX_Y;
        int originX = (getWidth()  - totalW) / 2;
        int originY = (getHeight() - totalH) / 2;

        for (Node n : allNodes) {
            int wx = state.getMap().getWorldX(n.getId()) - minX;
            int wy = state.getMap().getWorldY(n.getId()) - minY;
            int cx = originX + wx * pitchX + cellSize / 2;
            int cy = originY + wy * pitchY + cellSize / 2;

            int ratio = (n.getType() == NodeType.OFFICE) ? OFFICE_CELL_RATIO : NORMAL_CELL_RATIO;
            int size = Math.max(10, cellSize * ratio / 100);
            nodeBounds.put(n.getId(), new Rectangle(
                    cx - size / 2, cy - size / 2, size, size));
        }
    }

    @Override
    public void doLayout() {
        lastW = -1;
        lastH = -1;
        recomputePositionsIfNeeded();
    }

    // ---------- Render ----------

    private boolean hasRenderedOnce = false;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Detectar cambios de sector para disparar la estática, sin importar
        // si vinieron del click directo o del input buffer.
        String currentActive = state.getActiveSectorId();
        if (!java.util.Objects.equals(currentActive, lastRenderedActiveSector)) {
            if (hasRenderedOnce) {
                staticStartMs = System.currentTimeMillis();
            }
            lastRenderedActiveSector = currentActive;
            hasRenderedOnce = true;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        recomputePositionsIfNeeded();
        float pulse = computePulse();

        paintBackground(g2);
        for (Sector s : state.getMap().getSectors().values()) {
            paintSectorBackground(g2, s, pulse);
        }
        paintLinks(g2);

        for (Node n : state.getMap().getNodes().values()) {
            if (n.getType() == NodeType.OFFICE) continue;
            Rectangle r = nodeBounds.get(n.getId());
            if (r == null) continue;
            boolean lit = n.getSectorId().equals(state.getActiveSectorId());
            paintNode(g2, n, r, lit);
        }
        for (Node n : state.getMap().getNodes().values()) {
            if (n.getType() != NodeType.OFFICE) continue;
            Rectangle r = nodeBounds.get(n.getId());
            if (r == null) continue;
            paintOfficeNode(g2, r, pulse);
        }

        paintAnimatronics(g2);
        paintLastSeen(g2);
        paintStatic(g2);
        paintCooldownRing(g2);
        paintPersistentInterference(g2);

        g2.dispose();
    }


    private void paintBackground(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();

        g2.setPaint(new GradientPaint(0, 0, Theme.BG_DEEP, 0, h, Theme.BG_PANEL));
        g2.fillRect(0, 0, w, h);

        int radius = Math.max(w, h);
        if (radius > 0) {
            g2.setPaint(new RadialGradientPaint(
                    new Point(w / 2, h / 2),
                    radius * 0.75f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 90)}));
            g2.fillRect(0, 0, w, h);
        }
    }

    private float computePulse() {
        long t = System.currentTimeMillis() % PULSE_PERIOD_MS;
        return (float) (0.5 + 0.5 * Math.sin(t / (double) PULSE_PERIOD_MS * 2 * Math.PI));
    }

    private void paintSectorBackground(Graphics2D g2, Sector s, float pulse) {
        Rectangle bounds = getSectorBounds(s);
        if (bounds == null) return;

        int x = bounds.x;
        int y = bounds.y;
        int w = bounds.width;
        int h = bounds.height;

        boolean isOffice  = s.getId().equals(state.getMap().getOfficeSectorId());
        boolean reveal    = state.isGameFinished();
        boolean isActive  = s.getId().equals(state.getActiveSectorId());
        boolean isHovered = s.getId().equals(hoveredSectorId);
        boolean active    = reveal || isActive || isOffice;

        if (isOffice) {
            g2.setColor(Theme.BG_SECTOR_OFFICE);
        } else if (active) {
            g2.setColor(Theme.BG_SECTOR_ACTIVE);
        } else if (isHovered) {
            g2.setColor(Theme.BG_SECTOR_HOVER);
        } else {
            g2.setColor(Theme.BG_SECTOR_DIM);
        }
        g2.fillRoundRect(x, y, w, h, 18, 18);

        if (active) {
            int glowAlpha = (int) (60 + 80 * pulse);
            Color borderColor = isOffice
                    ? Theme.NODE_OFFICE_BORDER
                    : Theme.BORDER_SECTOR_ACTIVE;
            g2.setColor(Theme.withAlpha(borderColor, glowAlpha));
            g2.setStroke(new BasicStroke(4f));
            g2.drawRoundRect(x, y, w, h, 18, 18);

            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(x, y, w, h, 18, 18);
            if (state.isSectorTapped(s.getId())) {
                g2.setColor(new Color(220, 60, 60, 40));
                g2.fillRoundRect(x, y, w, h, 18, 18);
                g2.setColor(new Color(220, 60, 60, 200));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x, y, w, h, 18, 18);
            }
        } else if (isHovered) {
            g2.setColor(Theme.BORDER_SECTOR_HOVER);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, w, h, 18, 18);
        } else {
            g2.setColor(Theme.BORDER_SECTOR_DIM);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y, w, h, 18, 18);
        }

        if (!HIDDEN_SECTOR_NAMES.contains(s.getId())) {
            g2.setFont(Theme.FONT_SECTOR_LABEL);
            g2.setColor(active ? Theme.TEXT_PRIMARY : Theme.TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(s.getDisplayName());
            int tx = x + (w - tw) / 2;
            int ty = y + SECTOR_HEADER_PX + 10;
            g2.drawString(s.getDisplayName(), tx, ty);
        }
    }

    private void paintLinks(Graphics2D g2) {
        float pulse = computePulse();
        boolean reveal = state.isGameFinished();
        String leftEntry  = state.getDoors().getLeftEntryNodeId();
        String rightEntry = state.getDoors().getRightEntryNodeId();
        boolean leftOn  = state.getDoors().isLeftLightOn();
        boolean rightOn = state.getDoors().isRightLightOn();
        String active   = state.getActiveSectorId();

        for (Node n : state.getMap().getNodes().values()) {
            Rectangle r1 = nodeBounds.get(n.getId());
            if (r1 == null) continue;
            Point p1 = centerOf(r1);
            boolean fromActive = reveal
                || n.getSectorId().equals(active)
                || n.getType() == NodeType.OFFICE;

            for (String nid : n.getNeighbors()) {
                Rectangle r2 = nodeBounds.get(nid);
                if (r2 == null) continue;
                if (n.getId().compareTo(nid) >= 0) continue;

                Node other = state.getMap().getNode(nid);
                boolean toActive = reveal
                    || other.getSectorId().equals(active)
                    || other.getType() == NodeType.OFFICE;

                boolean lit = fromActive || toActive;
                Point p2 = centerOf(r2);

                // Barra de estado de puerta entre ENTRY y OFFICE.
                if (isDoorBar(n, other)) {
                    boolean leftBar = n.getType() == NodeType.ENTRY_LEFT
                                || other.getType() == NodeType.ENTRY_LEFT;
                    String doorId = leftBar ? leftEntry : rightEntry;
                    boolean open = !state.getDoors().isBlocked(doorId);
                    Color doorColor = open
                            ? new Color(80, 220, 100)   // verde
                            : new Color(220, 60, 60);   // rojo
                    g2.setColor(doorColor);
                    g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                    continue;
                }

                boolean touchesLeft  = n.getId().equals(leftEntry)  || other.getId().equals(leftEntry);
                boolean touchesRight = n.getId().equals(rightEntry) || other.getId().equals(rightEntry);
                boolean entryLit = (touchesLeft && leftOn) || (touchesRight && rightOn);
                boolean touchesEntry = touchesLeft || touchesRight;

                if (entryLit) {
                    int glowAlpha = (int) (70 + 90 * pulse);
                    g2.setColor(Theme.withAlpha(Theme.ENTRY_GLOW, glowAlpha));
                    g2.setStroke(new BasicStroke(5f));
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);

                    g2.setColor(Theme.LINK_ENTRY);
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);

                } else if (touchesEntry) {
                    g2.setColor(lit ? Theme.LINK_ENTRY : Theme.dim(Theme.LINK_ENTRY, 0.45f));
                    g2.setStroke(new BasicStroke(lit ? 2.0f : 1.4f));
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);

                } else {
                    g2.setColor(lit ? Theme.LINK_ACTIVE : Theme.LINK_DIM);
                    g2.setStroke(new BasicStroke(lit ? 2.2f : 1.2f));
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
    }

    private boolean isDoorBar(Node a, Node b) {
        boolean aEntry  = a.getType() == NodeType.ENTRY_LEFT || a.getType() == NodeType.ENTRY_RIGHT;
        boolean bEntry  = b.getType() == NodeType.ENTRY_LEFT || b.getType() == NodeType.ENTRY_RIGHT;
        boolean aOffice = a.getType() == NodeType.OFFICE;
        boolean bOffice = b.getType() == NodeType.OFFICE;
        return (aEntry && bOffice) || (bEntry && aOffice);
    }

    private void paintNode(Graphics2D g2, Node n, Rectangle r, boolean lit) {
        boolean reveal = state.isGameFinished();
        boolean isLeftEntry  = n.getType() == NodeType.ENTRY_LEFT;
        boolean isRightEntry = n.getType() == NodeType.ENTRY_RIGHT;
        boolean lightOn = (isLeftEntry  && state.getDoors().isLeftLightOn())
                    || (isRightEntry && state.getDoors().isRightLightOn());

        int corner = Math.max(4, r.width / 6);

        if (lightOn) {
            float pulse = computePulse();
            int glowAlpha = (int) (90 + 110 * pulse);
            g2.setColor(Theme.withAlpha(Theme.ENTRY_GLOW, glowAlpha));
            g2.setStroke(new BasicStroke(6f));
            g2.drawRoundRect(r.x - 5, r.y - 5, r.width + 10, r.height + 10, corner + 4, corner + 4);
        }

        Color base = switch (n.getType()) {
            case ENTRY_LEFT, ENTRY_RIGHT -> lightOn ? Theme.NODE_ENTRY_LIT : Theme.NODE_ENTRY;
            default -> Theme.NODE_NORMAL;
        };
        if (!reveal) {
            if (!lit && !lightOn) base = Theme.dim(base, DIM_FACTOR);
            if (!lit && lightOn)  base = Theme.dim(base, 0.85f);
        }

        g2.setColor(Theme.NODE_SHADOW);
        g2.fillRoundRect(r.x + 3, r.y + 3, r.width, r.height, corner, corner);

        g2.setColor(base);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, corner, corner);

        if (lit || lightOn || reveal) {
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height / 3, corner, corner);
        }

        g2.setColor(lightOn ? Theme.ENTRY_GLOW
                : (lit || reveal) ? Theme.NODE_BORDER_ACTIVE
                : Theme.NODE_BORDER_DIM);
        g2.setStroke(new BasicStroke(lightOn ? 2.4f : (lit ? 1.8f : 1f)));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, corner, corner);
    }

    private void paintOfficeNode(Graphics2D g2, Rectangle r, float pulse) {
        int corner = Math.max(6, r.width / 8);

        int glowAlpha = (int) (70 + 70 * pulse);
        g2.setColor(Theme.withAlpha(Theme.NODE_OFFICE_GLOW, glowAlpha));
        g2.setStroke(new BasicStroke(6f));
        g2.drawRoundRect(r.x - 4, r.y - 4, r.width + 8, r.height + 8, corner + 4, corner + 4);

        g2.setColor(Theme.NODE_SHADOW);
        g2.fillRoundRect(r.x + 4, r.y + 4, r.width, r.height, corner, corner);

        g2.setColor(Theme.NODE_OFFICE);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, corner, corner);

        g2.setColor(new Color(255, 255, 255, 40));
        g2.fillRoundRect(r.x + 3, r.y + 3, r.width - 6, r.height / 3, corner, corner);

        g2.setColor(Theme.NODE_OFFICE_BORDER);
        g2.setStroke(new BasicStroke(2.4f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, corner, corner);

        g2.setFont(Theme.FONT_SECTOR_LABEL);
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        String label = "OFICINA";
        int tw = fm.stringWidth(label);
        g2.drawString(label,
                r.x + (r.width - tw) / 2,
                r.y + r.height / 2 + fm.getAscent() / 2 - 2);
    }

    private void paintAnimatronics(Graphics2D g2) {
        boolean reveal = state.isGameFinished();

        for (AnimatronicView a : state.getAnimatronicViews()) {
            Node node = state.getMap().getNode(a.currentNodeId());
            if (node == null) continue;

            boolean visible = reveal || state.isAnimatronicVisibleToPlayer(a.id());
            if (!visible) continue;

            Rectangle r = nodeBounds.get(node.getId());
            if (r == null) continue;

            int badgeSize = Math.max(14, cellSize * NORMAL_CELL_RATIO / 100 / 2);
            int bx = r.x + r.width - badgeSize + 4;
            int by = r.y - 4;

            if (a.forcedVisible()) {
                g2.setColor(Theme.withAlpha(a.color(), 130));
                g2.fillOval(bx - 6, by - 6, badgeSize + 12, badgeSize + 12);
            }

            g2.setColor(Theme.withAlpha(a.color(), 70));
            g2.fillOval(bx - 3, by - 3, badgeSize + 6, badgeSize + 6);

            g2.setColor(a.color());
            g2.fillOval(bx, by, badgeSize, badgeSize);

            g2.setColor(new Color(0, 0, 0, 140));
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(bx, by, badgeSize, badgeSize);

            g2.setFont(Theme.FONT_ANIMATRONIC);
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            String s = a.symbol();
            int tw = fm.stringWidth(s);
            int tx = bx + (badgeSize - tw) / 2;
            int ty = by + (badgeSize + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(s, tx, ty);
        }
    }

    private void paintLastSeen(Graphics2D g2) {
        if (state.isGameFinished()) return;

        for (AnimatronicView a : state.getAnimatronicViews()) {
            String seen = a.lastSeenNodeId();
            if (seen == null) continue;

            Node seenNode = state.getMap().getNode(seen);
            if (seenNode == null) continue;

            // Si el animatrónico está en el mismo nodo y es visible, el badge real
            // ya lo representa. No duplicamos la marca.
            if (seen.equals(a.currentNodeId())
                    && state.isAnimatronicVisibleToPlayer(a.id())) {
                continue;
            }

            Rectangle r = nodeBounds.get(seen);
            if (r == null) continue;

            int badgeSize = Math.max(12, cellSize * NORMAL_CELL_RATIO / 100 / 2);
            int bx = r.x + r.width - badgeSize + 4;
            int by = r.y - 4;

            g2.setColor(Theme.withAlpha(a.color(), 35));
            g2.fillOval(bx, by, badgeSize, badgeSize);

            g2.setColor(Theme.withAlpha(a.color(), 180));
            g2.setStroke(new BasicStroke(
                    1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0f, new float[]{3f, 3f}, 0f));
            g2.drawOval(bx, by, badgeSize, badgeSize);

            g2.setFont(Theme.FONT_ANIMATRONIC);
            g2.setColor(Theme.withAlpha(a.color(), 200));
            FontMetrics fm = g2.getFontMetrics();
            String s = a.symbol();
            int tw = fm.stringWidth(s);
            int tx = bx + (badgeSize - tw) / 2;
            int ty = by + (badgeSize + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(s, tx, ty);
        }
    }

    // ---------- Efectos de cámara ----------

    private void paintStatic(Graphics2D g2) {
        long now = System.currentTimeMillis();
        long elapsed = now - staticStartMs;
        if (elapsed >= STATIC_DURATION_MS) return;

        float progress = elapsed / (float) STATIC_DURATION_MS;
        int alpha = (int) (200 * (1.0f - progress));
        if (alpha <= 0) return;

        int w = getWidth();
        int h = getHeight();
        Random r = new Random(now / 35);

        for (int y = 0; y < h; y += STATIC_STEP_PX) {
            for (int x = 0; x < w; x += STATIC_STEP_PX) {
                int v = r.nextInt(256);
                g2.setColor(new Color(v, v, v, alpha));
                g2.fillRect(x, y, STATIC_STEP_PX, STATIC_STEP_PX);
            }
        }

        int scanY = (int) (progress * h);
        g2.setColor(new Color(255, 255, 255, (int) (90 * (1.0f - progress))));
        g2.fillRect(0, scanY, w, 2);
    }

    private void paintCooldownRing(Graphics2D g2) {
        long now = System.currentTimeMillis();
        long lastChange = state.getLastSectorChangeMs();

        // El anillo debe durar lo que dure el bloqueo más largo activo:
        // cooldown normal (1.5s tras un cambio de cámara) o lockout de reset (3.6s).
        long cooldownEnd = lastChange + GameState.getCameraCooldownMs();
        long lockoutEnd  = state.getCamerasLockoutUntilMs();
        long end = Math.max(cooldownEnd, lockoutEnd);

        long elapsed = now - lastChange;
        long totalDuration = end - lastChange;
        if (totalDuration <= 0 || elapsed >= totalDuration) return;

        float remaining = 1.0f - elapsed / (float) totalDuration;
        if (remaining <= 0f) return;

        int size = COOLDOWN_CIRCLE_PX;
        int margin = COOLDOWN_MARGIN_PX;
        int cx = getWidth() - margin - size / 2;
        int cy = margin + size / 2;

        g2.setColor(new Color(0, 0, 0, 110));
        g2.fillOval(cx - size / 2 + 2, cy - size / 2 + 2, size, size);

        g2.setColor(new Color(20, 20, 30, 220));
        g2.fillOval(cx - size / 2, cy - size / 2, size, size);

        int angle = (int) (360 * remaining);
        long now2 = System.currentTimeMillis();
        boolean inLockout = now2 < state.getCamerasLockoutUntilMs();
        g2.setColor(inLockout ? Theme.HEAT_CRIT : Theme.NODE_OFFICE_BORDER);
        g2.fillArc(cx - size / 2, cy - size / 2, size, size, 90, -angle);

        g2.setColor(Theme.BORDER_SECTOR_DIM);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(cx - size / 2, cy - size / 2, size, size);
    }

    // ---------- Hit testing ----------

    private String sectorAt(int px, int py) {
        recomputePositionsIfNeeded();
        String officeSectorId = state.getMap().getOfficeSectorId();
        List<Sector> sectors = new ArrayList<>(state.getMap().getSectors().values());

        for (int i = sectors.size() - 1; i >= 0; i--) {
            Sector s = sectors.get(i);
            if (s.getId().equals(officeSectorId)) continue;
            Rectangle b = getSectorBounds(s);
            if (b != null && b.contains(px, py)) {
                return s.getId();
            }
        }
        return null;
    }

    private Rectangle getSectorBounds(Sector s) {
        List<Node> nodes = state.getMap().getNodesOfSector(s.getId());
        if (nodes.isEmpty()) return null;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (Node n : nodes) {
            Rectangle r = nodeBounds.get(n.getId());
            if (r == null) return null;
            minX = Math.min(minX, r.x);
            maxX = Math.max(maxX, r.x + r.width);
            minY = Math.min(minY, r.y);
            maxY = Math.max(maxY, r.y + r.height);
        }

        int padX   = Math.max(6, GAP_PX_X / 2 + 4);
        int padY   = Math.max(6, GAP_PX_Y / 2 + 2);
        int header = SECTOR_HEADER_PX;

        return new Rectangle(
                minX - padX,
                minY - padY - header,
                (maxX - minX) + 2 * padX,
                (maxY - minY) + 2 * padY + header);
    }

    private Point centerOf(Rectangle r) {
        return new Point(r.x + r.width / 2, r.y + r.height / 2);
    }

    private void paintPersistentInterference(Graphics2D g2) {
        java.util.Set<String> tapped = state.getTappedSectors();
        if (tapped.isEmpty()) return;

        long t = System.currentTimeMillis();

        for (String sectorId : tapped) {
            Sector s = state.getMap().getSector(sectorId);
            if (s == null) continue;
            Rectangle b = getSectorBounds(s);
            if (b == null) continue;

            // 1) Fondo opaco (tapa todo lo dibujado debajo: nodos, animatrónicos, links)
            g2.setColor(new Color(18, 18, 24));
            g2.fillRoundRect(b.x, b.y, b.width, b.height, 18, 18);

            // 2) Estática densa, con semilla por sector y ventana temporal.
            //    Alpha 255 = opaco total.
            Random r = new Random(t / 80 + sectorId.hashCode());
            for (int y = b.y; y < b.y + b.height; y += STATIC_STEP_PX) {
                for (int x = b.x; x < b.x + b.width; x += STATIC_STEP_PX) {
                    int v = r.nextInt(256);
                    g2.setColor(new Color(v, v, v, 255));
                    g2.fillRect(x, y, STATIC_STEP_PX, STATIC_STEP_PX);
                }
            }

            // 3) Scanline que baja por el sector
            long scanT = t % 1500;
            int scanY = b.y + (int) (scanT / 1500.0 * b.height);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fillRect(b.x, scanY, b.width, 3);

            // 4) Texto centrado, con fuente adaptada al ancho del sector
            int fontSize = Math.max(12, Math.min(22, b.width / 12));
            g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            g2.setColor(new Color(255, 255, 255, 220));
            FontMetrics fm = g2.getFontMetrics();
            String text = "SEÑAL PERDIDA";
            int tw = fm.stringWidth(text);
            g2.drawString(text,
                    b.x + (b.width - tw) / 2,
                    b.y + b.height / 2 + fm.getAscent() / 3);

            // 5) Borde rojo para delimitar el sector interferido
            g2.setColor(new Color(220, 60, 60, 220));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(b.x, b.y, b.width, b.height, 18, 18);
        }
    }
}
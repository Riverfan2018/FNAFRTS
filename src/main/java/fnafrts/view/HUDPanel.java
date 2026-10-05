package fnafrts.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import fnafrts.core.GameState;
import fnafrts.model.systems.AttentionSystem;
import fnafrts.model.systems.DoorSystem;

public class HUDPanel extends JPanel {

    private static final int HEIGHT        = 92;
    private static final int BAR_WIDTH     = 260;
    private static final int BAR_HEIGHT    = 12;
    private static final int RIGHT_MARGIN  = 20;
    private static final int ROW_GAP       = 14;
    private static final int TOP_MARGIN    = 14;

    private final GameState state;

    public HUDPanel(GameState state) {
        this.state = state;
        setPreferredSize(new Dimension(0, HEIGHT));
        setBackground(Theme.BG_PANEL);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SECTOR_DIM));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        paintClock(g2);
        paintAttentionBar(g2);
        paintHeatBar(g2);

        g2.dispose();
    }

    private void paintClock(Graphics2D g2) {
        int hour = state.getHour();
        int displayHour = (hour == 0) ? 12 : hour;

        double secondsPerHour = GameState.getSecondsPerHour();
        double elapsed = state.getElapsedSeconds();
        int minutes = (int) ((elapsed % secondsPerHour) / secondsPerHour * 60.0);
        if (minutes > 59) minutes = 59;

        String text = String.format("%d:%02d", displayHour, minutes);

        g2.setFont(new Font("SansSerif", Font.BOLD, 34));
        g2.setColor(Theme.TEXT_PRIMARY);

        FontMetrics fm = g2.getFontMetrics();
        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, 20, y);
    }
    
    private void paintAttentionBar(Graphics2D g2) {
        AttentionSystem attn = state.getAttention();
        double ratio = Math.min(1.0, attn.getRatio());

        int barX = getWidth() - RIGHT_MARGIN - BAR_WIDTH;
        int barY = TOP_MARGIN;

        Color color;
        if (ratio < 0.4)       color = Theme.ATTENTION_LOW;
        else if (ratio < 0.75) color = Theme.ATTENTION_MED;
        else                    color = Theme.ATTENTION_HIGH;

        paintBar(g2, barX, barY, BAR_WIDTH, BAR_HEIGHT, "ATENCIÓN", ratio, color);
    }

    private void paintHeatBar(Graphics2D g2) {
        DoorSystem doors = state.getDoors();
        double ratio = Math.min(1.0, doors.getHeat() / DoorSystem.MAX_HEAT);

        int barX = getWidth() - RIGHT_MARGIN - BAR_WIDTH;
        int barY = TOP_MARGIN + BAR_HEIGHT + ROW_GAP;

        Color color;
        if (doors.isOverheated()) color = Theme.HEAT_CRIT;
        else if (ratio < 0.4)     color = Theme.HEAT_LOW;
        else if (ratio < 0.7)     color = Theme.HEAT_MEDIUM;
        else if (ratio < 0.9)     color = Theme.HEAT_HIGH;
        else                       color = Theme.HEAT_CRIT;

        paintBar(g2, barX, barY, BAR_WIDTH, BAR_HEIGHT, "CALOR", ratio, color);
    }

    private void paintBar(Graphics2D g2, int x, int y, int w, int h,
                          String label, double ratio, Color fill) {

        g2.setFont(Theme.FONT_SECTOR_LABEL);
        g2.setColor(Theme.TEXT_PRIMARY);
        FontMetrics fm = g2.getFontMetrics();
        int lw = fm.stringWidth(label);
        g2.drawString(label, x - lw - 12, y + h - 1);

        // Fondo
        g2.setColor(Theme.BG_DEEP);
        g2.fillRoundRect(x, y, w, h, 6, 6);

        // Relleno
        int fillW = (int) (w * ratio);
        if (fillW > 0) {
            g2.setColor(fill);
            g2.fillRoundRect(x, y, fillW, h, 6, 6);
        }

        // Borde
        g2.setColor(Theme.BORDER_SECTOR_DIM);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, h, 6, 6);
    }
}
package fnafrts.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import fnafrts.core.GameState;
import fnafrts.model.systems.DoorState;
import fnafrts.model.systems.DoorSystem;

public class DoorWidget extends JPanel {

    private static final int WIDGET_W = 300;
    private static final int WIDGET_H = 165;

    private final GameState state;
    private final boolean isLeft;

    public DoorWidget(GameState state, boolean isLeft) {
        this.state = state;
        this.isLeft = isLeft;

        setOpaque(false);
        setPreferredSize(new Dimension(WIDGET_W, WIDGET_H));
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JLabel title = new JLabel(isLeft ? "PUERTA IZQUIERDA" : "PUERTA DERECHA",
                SwingConstants.CENTER);
        title.setFont(Theme.FONT_SECTOR_LABEL);
        title.setForeground(Theme.TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
        buttons.setOpaque(false);

        JButton doorButton = makeButton(isLeft ? "PUERTA (Q)" : "PUERTA (E)");
        doorButton.addActionListener(e -> {
            if (isLeft) state.getDoors().toggleLeft();
            else        state.getDoors().toggleRight();
        });

        JButton lightButton = makeButton(isLeft ? "LUZ (A)" : "LUZ (D)");
        lightButton.addActionListener(e -> {
            if (isLeft) state.getDoors().toggleLeftLight();
            else        state.getDoors().toggleRightLight();
        });

        buttons.add(doorButton);
        buttons.add(lightButton);
        add(buttons, BorderLayout.SOUTH);
    }

    private JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.FONT_BUTTON);
        b.setFocusPainted(false);
        b.setForeground(Theme.TEXT_PRIMARY);
        Theme.installHover(b, Theme.BG_DEEP, Theme.BORDER_SECTOR_DIM,
                   Theme.BUTTON_HOVER_BG, Theme.BUTTON_HOVER_BORDER);
        return b;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int top = 22;
        int bottom = getHeight() - 40;
        int mid = (top + bottom) / 2;
        int x = 8;
        int w = getWidth() - 16;

        paintDoorState(g2, x, top, w, (mid - top) - 3);
        paintLightState(g2, x, mid + 3, w, bottom - mid - 3);

        g2.dispose();
    }

    private void paintDoorState(Graphics2D g2, int x, int y, int w, int h) {
        DoorSystem doors = state.getDoors();
        DoorState doorState = isLeft ? doors.getLeftState() : doors.getRightState();

        Color border, fill, label;
        String text;
        switch (doorState) {
            case OPEN -> {
                border = Theme.DOOR_OPEN;
                fill   = Theme.withAlpha(Theme.DOOR_OPEN, 35);
                label  = Theme.DOOR_OPEN;
                text   = "ABIERTA";
            }
            case CLOSED -> {
                border = Theme.DOOR_CLOSED;
                fill   = Theme.withAlpha(Theme.DOOR_CLOSED, 45);
                label  = Theme.DOOR_CLOSED;
                text   = "CERRADA";
            }
            case OVERHEATED -> {
                border = Theme.DOOR_OVERHEAT;
                fill   = Theme.withAlpha(Theme.DOOR_OVERHEAT, 60);
                label  = Theme.DOOR_OVERHEAT;
                text   = "SOBRECALENTADA";
            }
            default -> {
                border = Theme.BORDER_SECTOR_DIM;
                fill   = Theme.BG_PANEL;
                label  = Theme.TEXT_MUTED;
                text   = "?";
            }
        }

        g2.setColor(fill);
        g2.fillRoundRect(x, y, w, h, 10, 10);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 10, 10);

        int iconSize = 20;
        int ix = x + 12;
        int iy = y + (h - iconSize) / 2;
        if (doorState == DoorState.CLOSED || doorState == DoorState.OVERHEATED) {
            g2.fillRect(ix + iconSize / 3, iy, iconSize / 3, iconSize);
        } else {
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(ix, iy, iconSize, iconSize);
        }

        g2.setColor(label);
        g2.setFont(Theme.FONT_SECTOR_LABEL);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        g2.drawString(text,
                x + (w - tw) / 2 + 8,
                y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void paintLightState(Graphics2D g2, int x, int y, int w, int h) {
        DoorSystem doors = state.getDoors();
        boolean lit = isLeft ? doors.isLeftLightOn() : doors.isRightLightOn();

        g2.setColor(lit ? Theme.PEEK_LIT : Theme.PEEK_DARK);
        g2.fillRoundRect(x, y, w, h, 10, 10);
        g2.setColor(lit ? Theme.ENTRY_GLOW : Theme.BORDER_SECTOR_DIM);
        g2.setStroke(new BasicStroke(lit ? 1.8f : 1f));
        g2.drawRoundRect(x, y, w, h, 10, 10);

        String text = lit ? "LUZ ENCENDIDA" : "LUZ APAGADA";
        g2.setColor(lit ? Theme.ENTRY_GLOW : Theme.TEXT_MUTED);
        g2.setFont(Theme.FONT_SECTOR_LABEL);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        g2.drawString(text,
                x + (w - tw) / 2,
                y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }
}
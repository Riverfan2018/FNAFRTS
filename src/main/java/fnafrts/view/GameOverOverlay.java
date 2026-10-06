package fnafrts.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import fnafrts.core.GameState;

public class GameOverOverlay extends JPanel {

    private final GameState state;

    public GameOverOverlay(GameState state) {
        this.state = state;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!state.isGameFinished()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Fondo oscuro
        g2.setColor(new Color(0, 0, 0, 175));
        g2.fillRect(0, 0, w, h);

        boolean won = state.isVictory();

        // Título
        String title = won ? "6 AM" : "GAME OVER";
        Color titleColor = won ? new Color(240, 210, 90) : new Color(200, 40, 40);
        g2.setColor(titleColor);
        g2.setFont(new Font("SansSerif", Font.BOLD, 84));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(title);
        g2.drawString(title, (w - tw) / 2, h / 2 - 30);

        // Subtítulo
        String subtitle;
        Color subtitleColor;
        if (won) {
            subtitle = "Sobreviviste hasta el amanecer.";
            subtitleColor = new Color(230, 230, 240);
        } else {
            subtitle = state.getGameOverReason();
            subtitleColor = new Color(230, 160, 160);
        }
        if (subtitle != null) {
            g2.setColor(subtitleColor);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 22));
            fm = g2.getFontMetrics();
            tw = fm.stringWidth(subtitle);
            g2.drawString(subtitle, (w - tw) / 2, h / 2 + 25);
        }

        // Instrucción
        g2.setColor(new Color(180, 180, 200));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        String hint = "Presioná R para reiniciar la noche  ·  ESC para volver al menú";
        fm = g2.getFontMetrics();
        tw = fm.stringWidth(hint);
        g2.drawString(hint, (w - tw) / 2, h / 2 + 100);

        g2.dispose();
    }
}
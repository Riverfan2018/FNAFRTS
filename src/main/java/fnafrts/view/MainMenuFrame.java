package fnafrts.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MainMenuFrame extends JFrame {

    public enum Action { PLAY, QUIT }

    private final Consumer<Action> onAction;

    public MainMenuFrame(Consumer<Action> onAction) {
        super("FNAF_RTS - Menú");
        this.onAction = onAction;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                MainMenuFrame.this.onAction.accept(Action.QUIT);
            }
        });
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        MenuBackground bg = new MenuBackground();
        bg.setLayout(new GridBagLayout());
        add(bg, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(6, 0, 6, 0);
        c.anchor = GridBagConstraints.CENTER;

        JButton playButton = makeButton("JUGAR");
        playButton.addActionListener(e -> {
            dispose();
            onAction.accept(Action.PLAY);
        });
        bg.add(playButton, c);

        c.gridy = 1;
        JButton quitButton = makeButton("SALIR");
        quitButton.addActionListener(e -> {
            dispose();
            onAction.accept(Action.QUIT);
        });
        bg.add(quitButton, c);
    }

    private JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 22));
        b.setFocusPainted(false);
        b.setBackground(Theme.BG_DEEP);
        b.setForeground(Theme.TEXT_PRIMARY);
        b.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SECTOR_ACTIVE, 2));
        b.setPreferredSize(new Dimension(240, 60));
        return b;
    }

    private static class MenuBackground extends JPanel {
        MenuBackground() {
            setBackground(Theme.BG_DEEP);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Título
            g2.setFont(new Font("SansSerif", Font.BOLD, 76));
            g2.setColor(Theme.NODE_OFFICE_BORDER);
            String title = "FNAF_RTS";
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(title);
            g2.drawString(title, (getWidth() - tw) / 2, getHeight() / 3);

            // Subtítulo
            g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g2.setColor(Theme.TEXT_MUTED);
            String subtitle = "Sobrevive hasta las 6 AM";
            fm = g2.getFontMetrics();
            tw = fm.stringWidth(subtitle);
            g2.drawString(subtitle, (getWidth() - tw) / 2, getHeight() / 3 + 44);

            g2.dispose();
        }
    }

    public static void launch(Consumer<Action> onAction) {
        SwingUtilities.invokeLater(() -> new MainMenuFrame(onAction).setVisible(true));
    }
}
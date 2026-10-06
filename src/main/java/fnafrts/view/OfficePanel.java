package fnafrts.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import fnafrts.core.GameState;

public class OfficePanel extends JPanel {

    private final GameState state;
    private final JButton pizzaButton;
    private final JButton shockButton;
    private final JButton camerasOffButton;

    public OfficePanel(GameState state) {
        this.state = state;
        setPreferredSize(new Dimension(0, 170));
        setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_SECTOR_DIM));

        JPanel leftSide = new JPanel();
        leftSide.setLayout(new BoxLayout(leftSide, BoxLayout.X_AXIS));
        leftSide.setOpaque(false);
        leftSide.add(new DoorWidget(state, true));

        pizzaButton     = makeActionButton("PIZZA (W)");
        shockButton     = makeActionButton("SHOCK (S)");
        camerasOffButton = makeActionButton("CÁMARAS (C)");

        pizzaButton.addActionListener(e -> {
            if (state.isGameFinished()) return;
            state.orderPizza();
        });
        shockButton.addActionListener(e -> {
            if (state.isGameFinished()) return;
            state.shockAnimatronic();
        });
        camerasOffButton.addActionListener(e -> {
            if (state.isGameFinished()) return;
            state.turnOffCameras();
        });

        leftSide.add(wrapSlot(pizzaButton));
        leftSide.add(Box.createHorizontalStrut(6));
        leftSide.add(wrapSlot(shockButton));
        leftSide.add(Box.createHorizontalStrut(6));
        leftSide.add(wrapSlot(camerasOffButton));

        add(leftSide, BorderLayout.WEST);

        JLabel title = new JLabel("OFICINA", SwingConstants.CENTER);
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MUTED);
        add(title, BorderLayout.CENTER);

        add(new DoorWidget(state, false), BorderLayout.EAST);

        Timer poll = new Timer(200, e -> {
            boolean finished = state.isGameFinished();

            boolean inKitchen = state.isCameraOnKitchen();
            pizzaButton.setVisible(inKitchen);
            pizzaButton.setEnabled(inKitchen && !finished && state.canOrderPizza());

            boolean inEmployee = state.isCameraOnEmployeeLounge();
            shockButton.setVisible(inEmployee);
            shockButton.setEnabled(inEmployee && !finished && state.canShock());

            boolean hasActiveCamera = state.getActiveSectorId() != null;
            camerasOffButton.setVisible(hasActiveCamera);
            camerasOffButton.setEnabled(hasActiveCamera && !finished);
        });
        poll.start();
    }

    private JButton makeActionButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.FONT_BUTTON);
        b.setFocusPainted(false);
        b.setForeground(Theme.TEXT_PRIMARY);
        b.setPreferredSize(new Dimension(130, 80));
        b.setVisible(false);
        b.setEnabled(false);

        Theme.installHover(b, Theme.BG_DEEP, Theme.BORDER_SECTOR_DIM,
                        Theme.BUTTON_HOVER_BG, Theme.BUTTON_HOVER_BORDER);

        return b;
    }

    private JPanel wrapSlot(JButton button) {
        JPanel slot = new JPanel(new GridBagLayout());
        slot.setOpaque(false);
        slot.setPreferredSize(new Dimension(140, 165));
        slot.setMinimumSize(new Dimension(140, 165));
        slot.setMaximumSize(new Dimension(140, 165));
        slot.add(button);
        return slot;
    }
}
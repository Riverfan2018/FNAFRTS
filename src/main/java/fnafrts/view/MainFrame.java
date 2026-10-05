package fnafrts.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import fnafrts.core.GameState;

public class MainFrame extends JFrame {

    private final GameState state;
    private final MapPanel mapPanel;
    private final HUDPanel hudPanel;
    private final OfficePanel officePanel;
    private final GameOverOverlay overlay;
    private final Consumer<Boolean> onExit;

    public MainFrame(GameState state, Consumer<Boolean> onExit) {
        super("FNAF_RTS");
        this.state = state;
        this.onExit = onExit;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                MainFrame.this.onExit.accept(false);
            }
        });

        List<String> sectorIds = state.getSectorIds();
        String officeSectorId = state.getMap().getOfficeSectorId();
        String initialSector = sectorIds.stream()
                .filter(id -> !id.equals(officeSectorId))
                .findFirst()
                .orElse(sectorIds.get(0));

        hudPanel    = new HUDPanel(state);
        mapPanel    = new MapPanel(state, initialSector);
        officePanel = new OfficePanel(state);

        add(hudPanel,    BorderLayout.NORTH);
        add(mapPanel,    BorderLayout.CENTER);
        add(officePanel, BorderLayout.SOUTH);

        overlay = new GameOverOverlay(state);
        overlay.setVisible(false);
        setGlassPane(overlay);

        installShortcuts();

        Timer renderTimer = new Timer(33, e -> {
            boolean finished = state.isGameFinished();
            if (finished && !overlay.isVisible()) {
                overlay.setVisible(true);
            }
            mapPanel.repaint();
            hudPanel.repaint();
            officePanel.repaint();
            if (finished) overlay.repaint();
        });
        renderTimer.start();
    }

    private void installShortcuts() {
        JComponent root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        bind(im, am, KeyEvent.VK_Q, "toggleLeftDoor", () -> {
            if (state.isGameFinished()) return;
            state.getDoors().toggleLeft();
        });
        bind(im, am, KeyEvent.VK_E, "toggleRightDoor", () -> {
            if (state.isGameFinished()) return;
            state.getDoors().toggleRight();
        });
        bind(im, am, KeyEvent.VK_A, "toggleLeftLight", () -> {
            if (state.isGameFinished()) return;
            state.getDoors().toggleLeftLight();
        });
        bind(im, am, KeyEvent.VK_D, "toggleRightLight", () -> {
            if (state.isGameFinished()) return;
            state.getDoors().toggleRightLight();
        });
        bind(im, am, KeyEvent.VK_W, "orderPizza", () -> {
            if (state.isGameFinished()) return;
            state.orderPizza();
        });
        bind(im, am, KeyEvent.VK_C, "camerasOff", () -> {
            if (state.isGameFinished()) return;
            state.turnOffCameras();
        });
        bind(im, am, KeyEvent.VK_S, "shock", () -> {
            if (state.isGameFinished()) return;
            state.shockAnimatronic();
        });

        bind(im, am, KeyEvent.VK_R, "restart", () -> {
            if (!state.isGameFinished()) return;
            dispose();
            onExit.accept(true);
        });
        bind(im, am, KeyEvent.VK_ESCAPE, "quit", () -> {
            if (!state.isGameFinished()) return;
            dispose();
            onExit.accept(true);   // volver al menú
        });
    }

    private void bind(InputMap im, ActionMap am, int keyCode, String name, Runnable action) {
        im.put(KeyStroke.getKeyStroke(keyCode, 0), name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    public static void launch(GameState state, Consumer<Boolean> onExit) {
        SwingUtilities.invokeLater(() -> new MainFrame(state, onExit).setVisible(true));
    }
}
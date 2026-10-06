package fnafrts;

import java.awt.Color;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import javax.swing.SwingUtilities;

import fnafrts.core.GameLoop;
import fnafrts.core.GameState;
import fnafrts.model.animatronic.Bonnie;
import fnafrts.model.animatronic.Chica;
import fnafrts.model.animatronic.Endo;
import fnafrts.model.animatronic.Foxy;
import fnafrts.model.animatronic.Freddy;
import fnafrts.model.animatronic.GoldenFreddy;
import fnafrts.model.graph.MapGraph;
import fnafrts.util.MapLoader;
import fnafrts.view.MainFrame;
import fnafrts.view.MainMenuFrame;

public class Main {
    public static void main(String[] args) {
        while (true) {
            MainMenuFrame.Action action = showMenu();
            if (action == MainMenuFrame.Action.QUIT) return;

            boolean backToMenu = false;
            while (!backToMenu) {
                try {
                    MainFrame.ExitReason reason = runGame();
                    switch (reason) {
                        case RESTART -> { /* vuelve a jugar sin pasar por el menú */ }
                        case MENU -> backToMenu = true;
                        case QUIT -> { return; }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    backToMenu = true;
                }
            }
        }
    }

    private static MainMenuFrame.Action showMenu() {
        CompletableFuture<MainMenuFrame.Action> result = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> MainMenuFrame.launch(result::complete));
        try {
            return result.get();
        } catch (Exception e) {
            return MainMenuFrame.Action.QUIT;
        }
    }

    private static MainFrame.ExitReason runGame() throws Exception {
        MapGraph map = MapLoader.loadFromResource("/maps/prototype.txt");

        Random rng = new Random();
        GameState state = new GameState(map, rng);

        Chica chica = new Chica("chica", "Chica", "Ch", new Color(230, 200, 60), "S1", "S1", rng);
        Bonnie bonnie = new Bonnie("bonnie", "Bonnie", "Bo", new Color(140, 80, 220), "S3", "S3", rng);
        Freddy freddy = new Freddy("freddy", "Freddy", "Fr", new Color(139, 90, 43), "S2", "S2", rng);
        Foxy foxy = new Foxy("foxy", "Foxy", "Fo", new Color(180, 40, 40), "E1", rng);
        GoldenFreddy goldenFreddy = new GoldenFreddy("golden", "Golden Freddy", "GF",
                new Color(240, 200, 60), "BS4", rng);
        Endo endo = new Endo("endo", "Endo", "En", new Color(180, 180, 190), "EL2", rng);

        state.addAnimatronic(chica);
        state.addAnimatronic(bonnie);
        state.addAnimatronic(freddy);
        state.addAnimatronic(foxy);
        state.addAnimatronic(goldenFreddy);
        state.addAnimatronic(endo);

        GameLoop loop = new GameLoop(state, 1.0);
        loop.start();

        CompletableFuture<MainFrame.ExitReason> result = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> MainFrame.launch(state, result::complete));

        MainFrame.ExitReason reason = result.get();
        loop.stop();
        return reason;
    }

}
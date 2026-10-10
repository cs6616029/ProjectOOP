package project.game;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Vampire Survivors");

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new GameStartScreen((characterType, mapResource) -> {
                VampireSurvivorsGame game = new VampireSurvivorsGame(characterType, mapResource);
                frame.setContentPane(game);
                frame.revalidate();
                frame.repaint();
                game.startGameThread();
                game.requestFocusInWindow();
            }));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
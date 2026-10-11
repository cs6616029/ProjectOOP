package project.game;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ISEKAI SURVIVORS");

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            showStartScreen(frame);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void showStartScreen(JFrame frame) {
        frame.setContentPane(new GameStartScreen((characterType, mapResource) -> {
            ISEKAISurvivors game = new ISEKAISurvivors(
                    characterType, mapResource, () -> showStartScreen(frame));
            frame.setContentPane(game);
            frame.revalidate();
            frame.repaint();
            game.startGameThread();
            game.requestFocusInWindow();
        }));
        frame.revalidate();
        frame.repaint();
    }
}

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowUI);
    }

    private static void createAndShowUI() {
        JFrame frame = new JFrame("Zombie Apocalypse Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        GamePanel gamePanel = new GamePanel();
        ClockIndicator clockIndicator = new ClockIndicator(gamePanel);

        int worldSize = gamePanel.getPreferredSize().width;

        // Layer the clock indicator on top of the game world, in the corner
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(worldSize, worldSize));
        gamePanel.setBounds(0, 0, worldSize, worldSize);
        clockIndicator.setBounds(worldSize - 86, 14, 72, 72);
        layeredPane.add(gamePanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(clockIndicator, JLayeredPane.PALETTE_LAYER);

        // Left sidebar: live stats up top, Play/Reset/Close controls below
        JLabel infoLabel = new JLabel();
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 17));
        infoLabel.setVerticalAlignment(SwingConstants.TOP);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        updateInfoLabel(infoLabel, gamePanel);

        JButton playButton = new JButton("Play");
        JButton resetButton = new JButton("Reset");
        JButton closeButton = new JButton("Close");

        Dimension buttonSize = new Dimension(150, 38);
        for (JButton b : new JButton[]{playButton, resetButton, closeButton}) {
            b.setMaximumSize(buttonSize);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        JPanel buttonBox = new JPanel();
        buttonBox.setLayout(new BoxLayout(buttonBox, BoxLayout.Y_AXIS));
        buttonBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        buttonBox.setBackground(Color.WHITE);
        buttonBox.add(playButton);
        buttonBox.add(Box.createVerticalStrut(8));
        buttonBox.add(resetButton);
        buttonBox.add(Box.createVerticalStrut(8));
        buttonBox.add(closeButton);

        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonWrapper.setBackground(Color.WHITE);
        buttonWrapper.add(buttonBox);

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        sidebar.setPreferredSize(new Dimension(240, worldSize));
        sidebar.add(infoLabel, BorderLayout.NORTH);
        sidebar.add(buttonWrapper, BorderLayout.CENTER);

        frame.setLayout(new BorderLayout());
        frame.add(sidebar, BorderLayout.WEST);
        frame.add(layeredPane, BorderLayout.CENTER);

        // Slower timer: each entity decides its next grid move
        Timer logicTimer = new Timer(500, e -> {
            gamePanel.step();
            updateInfoLabel(infoLabel, gamePanel);
        });

        // Fast timer: smooth walking animation, day/night cycle, clock widget
        Timer renderTimer = new Timer(16, e -> {
            gamePanel.tick();
            clockIndicator.repaint();
        });

        final boolean[] running = {false};
        playButton.addActionListener(e -> {
            if (running[0]) {
                logicTimer.stop();
                renderTimer.stop();
                playButton.setText("Play");
                gamePanel.setPaused(true);
            } else {
                logicTimer.start();
                renderTimer.start();
                playButton.setText("Pause");
                gamePanel.setPaused(false);
            }
            running[0] = !running[0];
        });

    resetButton.addActionListener(e -> {
    logicTimer.stop();
    renderTimer.stop();
    running[0] = false;
    playButton.setText("Play");
    gamePanel.setPaused(false);
    gamePanel.resetEntities();
    updateInfoLabel(infoLabel, gamePanel);
    });

        closeButton.addActionListener(e -> frame.dispose());

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void updateInfoLabel(JLabel label, GamePanel gamePanel) {
        label.setText("<html>Information on<br>number of humans,<br>zombies, etc.<br><br>"
                + "Humans: " + gamePanel.getHumanCount() + "<br>"
                + "Zombies: " + gamePanel.getZombieCount() + "<br>"
                + "Cures: " + gamePanel.getCureCount() + "</html>");
    }
}
import java.awt.*;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowUI);
    }

    private static void createAndShowUI() {
        JFrame frame = new JFrame("Zombie Apocalypse Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        GamePanel gamePanel;
        try {
            gamePanel = new GamePanel();
        } catch (WorldSetupException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Startup Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
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
         // Fast timer: smooth walking animation, day/night cycle, clock widget
        int[] fpsHolder = {60}; // cap the fps at 60
        
        JLabel fpsLabel = new JLabel("FPS: " + fpsHolder[0], SwingConstants.CENTER);
        fpsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSlider fpsSlider = new JSlider(10, 144, fpsHolder[0]); 
        fpsSlider.setMaximumSize(new Dimension(180, 45));
        fpsSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
        fpsSlider.setBackground(Color.WHITE);
        Timer renderTimer = new Timer(1000 / fpsHolder[0], e -> {
            gamePanel.tick();
            clockIndicator.repaint();
        });
        //add an event for Slider
        fpsSlider.addChangeListener(e -> {
            fpsHolder[0] = fpsSlider.getValue();
            fpsLabel.setText("FPS: " + fpsHolder[0]);
            if (fpsHolder[0] > 0) {
                renderTimer.setDelay(1000 / fpsHolder[0]); 
            }
        });
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
        buttonBox.add(fpsLabel);
        buttonBox.add(Box.createVerticalStrut(4));
        buttonBox.add(fpsSlider);
        buttonBox.add(Box.createVerticalStrut(8));

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

        final boolean[] running = {false};
        final Timer[] logicTimerHolder = new Timer[1];

        // Slower timer: each entity decides its next grid move
        Timer logicTimer = new Timer(500, e -> {
            gamePanel.step();
            updateInfoLabel(infoLabel, gamePanel);
            if (gamePanel.isGameOver()) {
                logicTimerHolder[0].stop();
                renderTimer.stop();
                running[0] = false;
                playButton.setText("Play");
            }
        });

        logicTimerHolder[0] = logicTimer;

        playButton.addActionListener(e -> {
            if (gamePanel.isGameOver()) return;
            if (running[0]) {
                logicTimer.stop();
                renderTimer.stop();
                playButton.setText("Play");
            } else {
                logicTimer.start();
                renderTimer.start();
                playButton.setText("Pause");
            }
            running[0] = !running[0];
        });

        resetButton.addActionListener(e -> {
            logicTimer.stop();
            renderTimer.stop();
            running[0] = false;
            playButton.setText("Play");
            try {
                gamePanel.resetEntities();
            } catch (WorldSetupException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Reset Failed", JOptionPane.ERROR_MESSAGE);
            }
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

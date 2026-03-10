package ui;

import Storage.StorageManager;
import model.Workspace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Kairo - A Smart Task and Scheduling System
 * Main application entry point with modern, elegant UI
 * 
 * Features a sophisticated feminine neutral palette with
 * blacks, warm greys, and refined tan accents
 */
public class KairoApp {

    private static final String APP_NAME = "Kairo";
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 850;
    private static final Path DATA_FILE = Paths.get(System.getProperty("user.home"), ".kairo", "workspace.json");

    public static void main(String[] args) {
        // Set system look and feel for better native integration
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Custom UI defaults for a refined, elegant look
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("Button.margin", new Insets(10, 20, 10, 20));
            
            // Refined tooltip styling
            UIManager.put("ToolTip.background", new Color(45, 42, 38));
            UIManager.put("ToolTip.foreground", new Color(250, 249, 247));
            UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(8, 12, 8, 12));
            
            // Scrollbar styling
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            
        } catch (Exception e) {
            // Continue with default look and feel
        }

        // Run on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // Initialize storage
            StorageManager storage = new StorageManager(DATA_FILE);
            
            // Load or create workspace
            Workspace workspace = storage.load();

            // Create main window
            JFrame window = new JFrame(APP_NAME);
            window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            window.setMinimumSize(new Dimension(1000, 700));
            window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            window.setLocationRelativeTo(null);
            
            // Set window background for seamless appearance
            window.getContentPane().setBackground(new Color(250, 249, 247));

            // Add main UI panel
            MainPanel panel = new MainPanel(workspace);
            window.setContentPane(panel);

            // Save workspace on close
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    storage.save(workspace);
                    window.dispose();
                    System.exit(0);
                }
            });

            window.setVisible(true);
        });
    }
}

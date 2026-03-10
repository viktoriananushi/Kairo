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
 * Main application entry point with modern, Notion-inspired UI
 */
public class KairoApp {

    private static final String APP_NAME = "Kairo";
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final Path DATA_FILE = Paths.get(System.getProperty("user.home"), ".kairo", "workspace.json");

    public static void main(String[] args) {
        // Set system look and feel for better native integration
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Set custom UI defaults for a cleaner look
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("Button.margin", new Insets(8, 16, 8, 16));
            
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
            window.setMinimumSize(new Dimension(900, 600));
            window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            window.setLocationRelativeTo(null);

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

package com.spa;

import com.spa.db.DatabaseManager;
import com.spa.gui.LoginFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // Nimbus gives a cleaner look than the default Swing Metal L&F.
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Fall back to default look and feel.
        }

        // Force the DB connection + schema creation on startup so any
        // connectivity/permission problems surface immediately with a clear error.
        try {
            DatabaseManager.getConnection();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Could not initialize the local database:\n" + e.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}

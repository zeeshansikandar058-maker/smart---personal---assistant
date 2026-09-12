package com.spa.util;

import javax.swing.*;
import java.awt.*;

public final class NotificationUtil {

    private static TrayIcon trayIcon;

    private NotificationUtil() {
    }

    private static void ensureTray() {
        if (trayIcon != null || !SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage(new byte[0]); // 0-byte placeholder icon
            // Use a simple generated icon instead of a missing resource file.
            image = Toolkit.getDefaultToolkit().createImage(generateIconBytes());
            trayIcon = new TrayIcon(image, "Smart Personal Assistant");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        } catch (Exception e) {
            trayIcon = null;
        }
    }

    public static void showNotification(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            ensureTray();
            if (trayIcon != null) {
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            } else {
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    /** Minimal 16x16 solid-color PNG built at runtime so we don't need a bundled icon asset. */
    private static byte[] generateIconBytes() {
        try {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(30, 120, 200));
            g.fillOval(0, 0, 16, 16);
            g.dispose();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
}

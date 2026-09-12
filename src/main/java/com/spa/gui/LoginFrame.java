package com.spa.gui;

import com.spa.auth.AuthService;
import com.spa.auth.User;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    public LoginFrame() {
        super("Smart Personal Assistant — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 260);
        setLocationRelativeTo(null);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Smart Personal Assistant", SwingConstants.CENTER);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(heading, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0; panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; panel.add(usernameField, gbc);

        gbc.gridy = 2; gbc.gridx = 0; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; panel.add(passwordField, gbc);

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Create Account");

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        JLabel hint = new JLabel("<html><i>First account created automatically becomes Admin.</i></html>", SwingConstants.CENTER);
        gbc.gridy = 4;
        panel.add(hint, gbc);

        loginBtn.addActionListener(e -> attemptLogin());
        registerBtn.addActionListener(e -> attemptRegister());
        passwordField.addActionListener(e -> attemptLogin());

        add(panel);
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        Optional<User> user = authService.login(username, password);
        if (user.isPresent()) {
            dispose();
            SwingUtilities.invokeLater(() -> new MainDashboardFrame(user.get()).setVisible(true));
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void attemptRegister() {
        JTextField cityField = new JTextField("Sahiwal");
        JTextField countryField = new JTextField("Pakistan");
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("City (for prayer times):"));
        form.add(cityField);
        form.add(new JLabel("Country:"));
        form.add(countryField);

        int result = JOptionPane.showConfirmDialog(this, form, "Complete Registration",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        Optional<String> error = authService.register(username, password, cityField.getText().trim(), countryField.getText().trim());
        if (error.isPresent()) {
            JOptionPane.showMessageDialog(this, error.get(), "Registration Failed", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Account created! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}

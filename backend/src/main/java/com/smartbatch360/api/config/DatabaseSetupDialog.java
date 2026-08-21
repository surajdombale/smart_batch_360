package com.smartbatch360.api.config;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

/**
 * First-run (or reconnect-after-failure) prompt: Database URL, then
 * Username, then Password - in that order, as requested. Swing rather than
 * JavaFX: the backend is a headless-by-default server process with no other
 * GUI dependency, and Swing ships in the JDK with no extra packaging cost.
 *
 * Only ever invoked from DatabaseBootstrap, and only when a display is
 * actually available (see GraphicsEnvironment.isHeadless() there) - tests,
 * CI and `mvn verify` never reach this class.
 */
final class DatabaseSetupDialog {

    private DatabaseSetupDialog() {
    }

    static Optional<DatabaseConfig> prompt(String previousError) {
        JTextField urlField = new JTextField("localhost:3306");
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(360, previousError != null ? 260 : 220));

        if (previousError != null) {
            JLabel errorLabel = new JLabel(wrap(previousError));
            errorLabel.setForeground(new Color(0xB00020));
            panel.add(errorLabel);
            panel.add(Box.createVerticalStrut(8));
        }

        panel.add(new JLabel("Database URL (host:port):"));
        panel.add(urlField);
        panel.add(Box.createVerticalStrut(6));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(Box.createVerticalStrut(6));
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel(wrap("The \"smartbatch360\" database is created automatically - "
                + "this user just needs permission to create databases (a MySQL admin/root user works).")));

        int result = JOptionPane.showConfirmDialog(null, panel, "SmartBatch360 - Database Connection",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }
        if (urlField.getText().isBlank() || usernameField.getText().isBlank()) {
            return prompt("Database URL and Username are required.");
        }

        return Optional.of(DatabaseConfig.of(urlField.getText(), usernameField.getText(),
                new String(passwordField.getPassword())));
    }

    private static String wrap(String text) {
        return "<html><body style='width: 300px'>" + text.replace("&", "&amp;").replace("<", "&lt;") + "</body></html>";
    }
}

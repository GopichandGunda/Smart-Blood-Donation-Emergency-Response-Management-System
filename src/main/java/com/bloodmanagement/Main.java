package com.bloodmanagement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public final class Main {
    private static final Color BRAND_RED = new Color(166, 35, 52);

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::showWelcomeWindow);
    }

    private static void showWelcomeWindow() {
        JFrame frame = new JFrame("Emergency Blood Request & Donor Matching System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(720, 420));
        frame.setSize(820, 480);
        frame.setLocationRelativeTo(null);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(8, 32, 8, 32);

        JLabel title = new JLabel("Emergency Blood Request", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        title.setForeground(BRAND_RED);
        constraints.gridy = 0;
        content.add(title, constraints);

        JLabel subtitle = new JLabel("& Donor Matching System", SwingConstants.CENTER);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        subtitle.setForeground(new Color(45, 55, 65));
        constraints.gridy = 1;
        content.add(subtitle, constraints);

        JLabel description = new JLabel(
                "<html><div style='text-align:center'>"
                        + "A Java desktop application for coordinating emergency blood requests,<br>"
                        + "donor information, and blood-bank operations."
                        + "</div></html>",
                SwingConstants.CENTER);
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        description.setForeground(new Color(85, 95, 105));
        constraints.gridy = 2;
        constraints.insets = new Insets(18, 32, 8, 32);
        content.add(description, constraints);

        JLabel phase = new JLabel("Phase 1 starter is ready", SwingConstants.CENTER);
        phase.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        phase.setForeground(new Color(105, 115, 125));
        constraints.gridy = 3;
        constraints.insets = new Insets(22, 32, 8, 32);
        content.add(phase, constraints);

        frame.add(content, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}

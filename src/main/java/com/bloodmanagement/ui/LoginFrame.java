package com.bloodmanagement.ui;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.service.AppServices;
import com.bloodmanagement.service.DonorService;
import com.bloodmanagement.util.DatabaseConnection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public final class LoginFrame extends JFrame {
    private static final Color RED = new Color(171, 38, 54);
    private static final Color NAVY = new Color(29, 43, 59);
    private static final Color LIGHT = new Color(246, 248, 250);
    private final AppServices services = new AppServices();
    private final JTextField username = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JButton signIn = new JButton("Sign in");

    public LoginFrame() {
        super("Emergency Blood Management | Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 700));
        setSize(1280, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(NAVY);
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.fill = GridBagConstraints.HORIZONTAL;
        left.insets = new Insets(10, 42, 10, 42);
        JLabel mark = new JLabel("LIFE  /  LINK", SwingConstants.CENTER);
        mark.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        mark.setForeground(new Color(255, 190, 195));
        left.gridy = 0;
        brand.add(mark, left);
        JLabel headline = new JLabel(
                "<html><div style='text-align:center'>Every moment<br>matters.</div></html>",
                SwingConstants.CENTER);
        headline.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
        headline.setForeground(Color.WHITE);
        left.gridy = 1;
        left.insets = new Insets(28, 32, 10, 32);
        brand.add(headline, left);
        JLabel statement = new JLabel(
                "<html><div style='text-align:center'>Coordinate emergency blood requests,<br>"
                        + "donor support, and blood-bank inventory<br>from one secure desktop workspace.</div></html>",
                SwingConstants.CENTER);
        statement.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        statement.setForeground(new Color(209, 218, 228));
        left.gridy = 2;
        left.insets = new Insets(12, 30, 10, 30);
        brand.add(statement, left);
        JLabel badge = new JLabel("JAVA DESKTOP  •  SECURE  •  CONNECTED", SwingConstants.CENTER);
        badge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        badge.setForeground(new Color(171, 186, 199));
        left.gridy = 3;
        left.insets = new Insets(48, 20, 20, 20);
        brand.add(badge, left);
        add(brand, BorderLayout.WEST);
        brand.setPreferredSize(new Dimension(440, 0));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 12, 8, 12);
        JLabel eyebrow = new JLabel("WELCOME BACK");
        eyebrow.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        eyebrow.setForeground(RED);
        c.gridy = 0;
        form.add(eyebrow, c);
        JLabel title = new JLabel("Sign in to your account");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 29));
        title.setForeground(NAVY);
        c.gridy = 1;
        form.add(title, c);
        JLabel subtitle = new JLabel("Use your registered username and password.");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        subtitle.setForeground(new Color(104, 115, 127));
        c.gridy = 2;
        c.insets = new Insets(3, 12, 24, 12);
        form.add(subtitle, c);
        c.gridwidth = 1;
        c.gridy = 3;
        c.insets = new Insets(8, 12, 8, 12);
        c.gridx = 0;
        form.add(label("Username"), c);
        c.gridy = 4;
        form.add(username, c);
        username.setPreferredSize(new Dimension(340, 40));
        styleInput(username);
        c.gridy = 5;
        form.add(label("Password"), c);
        c.gridy = 6;
        form.add(password, c);
        password.setPreferredSize(new Dimension(340, 40));
        styleInput(password);
        c.gridy = 7;
        c.insets = new Insets(20, 12, 8, 12);
        signIn.setBackground(RED);
        signIn.setForeground(Color.WHITE);
        signIn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        signIn.setFocusPainted(false);
        signIn.setPreferredSize(new Dimension(340, 44));
        form.add(signIn, c);
        JButton register = new JButton("New donor? Create an account");
        register.setForeground(RED);
        register.setBackground(Color.WHITE);
        register.setBorderPainted(false);
        register.setFocusPainted(false);
        register.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        c.gridy = 8;
        c.insets = new Insets(10, 12, 8, 12);
        form.add(register, c);
        JLabel note = new JLabel("<html><div style='text-align:center'>Authorized hospital and blood-bank personnel only.<br>"
                + "Transfusion compatibility must be confirmed by qualified staff.</div></html>",
                SwingConstants.CENTER);
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        note.setForeground(new Color(127, 137, 147));
        c.gridy = 9;
        c.insets = new Insets(28, 12, 8, 12);
        form.add(note, c);
        add(form, BorderLayout.CENTER);
        form.setBorder(BorderFactory.createEmptyBorder(35, 45, 30, 45));

        signIn.addActionListener(event -> signIn());
        password.addActionListener(event -> signIn());
        register.addActionListener(event -> registerDonor());
    }

    private void signIn() {
        signIn.setEnabled(false);
        char[] enteredPassword = password.getPassword();
        try {
            var user = services.authentication.authenticate(username.getText(), enteredPassword);
            new DashboardFrame(user, services).setVisible(true);
            dispose();
        } catch (RuntimeException exception) {
            showError(exception);
        } finally {
            java.util.Arrays.fill(enteredPassword, '\0');
            password.setText("");
            signIn.setEnabled(true);
        }
    }

    private void registerDonor() {
        JTextField fullName = new JTextField(20);
        JTextField age = new JTextField(20);
        JTextField gender = new JTextField(20);
        JComboBox<BloodGroup> bloodGroup = new JComboBox<>(BloodGroup.values());
        JTextField phone = new JTextField(20);
        JTextField email = new JTextField(20);
        JTextField address = new JTextField(20);
        JTextField city = new JTextField(20);
        JTextField dateOfBirth = new JTextField(LocalDate.now().minusYears(25).toString(), 20);
        JTextField account = new JTextField(20);
        JPasswordField newPassword = new JPasswordField(20);
        JPanel panel = form(
                "Full name", fullName, "Age", age, "Gender", gender, "Blood group", bloodGroup,
                "Phone", phone, "Email", email, "Address", address, "City", city,
                "Date of birth (YYYY-MM-DD)", dateOfBirth, "Username", account, "Password (12+ characters)", newPassword);
        int result = JOptionPane.showConfirmDialog(this, panel, "Create donor account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        char[] enteredPassword = newPassword.getPassword();
        try {
            var donor = DonorService.newDonor(fullName.getText(), Integer.parseInt(age.getText().trim()),
                    gender.getText().trim(), (BloodGroup) bloodGroup.getSelectedItem(), phone.getText(),
                    email.getText(), address.getText(), city.getText(), LocalDate.parse(dateOfBirth.getText().trim()));
            long id = services.donorService.register(donor, account.getText(), enteredPassword, null);
            JOptionPane.showMessageDialog(this, "Donor account created successfully. Your donor ID is " + id + ".",
                    "Registration complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError(exception);
        } finally {
            java.util.Arrays.fill(enteredPassword, '\0');
            newPassword.setText("");
        }
    }

    private JPanel form(Object... fields) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 8, 4, 8);
        for (int index = 0; index < fields.length; index += 2) {
            c.gridx = 0;
            panel.add(new JLabel(fields[index].toString()), c);
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add((java.awt.Component) fields[index + 1], c);
            c.gridy++;
        }
        return panel;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        label.setForeground(NAVY);
        return label;
    }

    private void styleInput(JTextField input) {
        input.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 224, 230)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    private void showError(RuntimeException exception) {
        String message = exception.getMessage() == null ? "An unexpected error occurred." : exception.getMessage();
        if (exception.getCause() instanceof java.sql.SQLException) {
            message += "\n\nCheck that MySQL is running, the database schema is installed, and the BLOOD_DB_* "
                    + "environment variables are correct.";
        }
        JOptionPane.showMessageDialog(this, message, "Unable to continue", JOptionPane.ERROR_MESSAGE);
    }
}

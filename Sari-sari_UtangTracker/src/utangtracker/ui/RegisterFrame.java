package utangtracker.ui;

import utangtracker.service.AppException;
import utangtracker.service.AuthService;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RegisterFrame extends JFrame {
    private final AuthService auth = new AuthService();
    private final JTextField firstName = Ui.field("Enter your first name");
    private final JTextField lastName = Ui.field("Enter your last name");
    private final JTextField contact = Ui.field("09XXXXXXXXX");
    private final JTextField username = Ui.field("Choose a username");
    private final JPasswordField password = Ui.password("Create a password");
    private final JPasswordField confirmPassword = Ui.password("Re-enter your password");
    private final JPasswordField pin = Ui.password("4-digit PIN");
    private final JPasswordField confirmPin = Ui.password("Re-enter your 4-digit PIN");

    public RegisterFrame() {
        super("Utang Tracker System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Ui.applyResponsiveSize(this, 820, 640, new Dimension(560, 480));

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.add(LoginFrame.topBar("UTANG TRACKER SYSTEM"), BorderLayout.NORTH);
        root.add(form(), BorderLayout.CENTER);
        JScrollPane scroller = new JScrollPane(root);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        scroller.getHorizontalScrollBar().setUnitIncrement(16);
        setContentPane(scroller);
    }

    private JPanel form() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        JPanel card = Ui.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 8, 8, 8);

        JLabel title = new JLabel("Customer Account Registration", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Ui.TEXT);
        title.setIcon(Ui.storeLogoIcon(36));
        title.setIconTextGap(10);
        title.setHorizontalTextPosition(SwingConstants.RIGHT);
        card.add(title, g);

        g.gridwidth = 1;
        g.gridy++;
        g.insets = new Insets(12, 8, 4, 8);
        card.add(Ui.caption("First name"), g);
        g.gridx = 1;
        card.add(Ui.caption("LastName"), g);

        g.gridy++;
        g.gridx = 0;
        g.insets = new Insets(0, 8, 8, 8);
        firstName.setPreferredSize(new Dimension(200, 40));
        card.add(firstName, g);
        g.gridx = 1;
        lastName.setPreferredSize(new Dimension(200, 40));
        card.add(lastName, g);

        addWide(card, g, "Contact Number", contact);
        addWide(card, g, "Username", username);
        addPasswordWide(card, g, "Password", password);
        addPasswordWide(card, g, "Confirm Password", confirmPassword);
        addPasswordWide(card, g, "4-digit Recovery PIN", pin);
        addPasswordWide(card, g, "Confirm Recovery PIN", confirmPin);

        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;
        g.insets = new Insets(16, 8, 8, 8);
        JButton register = Ui.primary("Register");
        register.setPreferredSize(new Dimension(280, 42));
        register.addActionListener(e -> save());
        card.add(register, g);
        getRootPane().setDefaultButton(register);

        g.gridy++;
        JLabel back = new JLabel("Back to Login", SwingConstants.CENTER);
        back.setFont(Ui.BODY);
        back.setForeground(new Color(0x2E, 0x7D, 0x32));
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
        card.add(back, g);
        wrap.add(card);
        return wrap;
    }

    private void addPasswordWide(JPanel card, GridBagConstraints g, String caption, JPasswordField field) {
        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;
        g.insets = new Insets(8, 8, 4, 8);
        card.add(Ui.caption(caption), g);
        g.gridy++;
        g.insets = new Insets(0, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        field.setPreferredSize(new Dimension(280, 40));

        JPanel pwRow = new JPanel(new BorderLayout(6, 0));
        pwRow.setOpaque(false);
        pwRow.add(field, BorderLayout.CENTER);
        JCheckBox showPw = new JCheckBox("Show");
        showPw.setOpaque(false);
        showPw.setForeground(Ui.MUTED);
        showPw.setFont(Ui.BODY);
        showPw.setFocusPainted(false);
        showPw.addActionListener(e -> {
            if (showPw.isSelected()) {
                field.setEchoChar((char) 0);
            } else {
                field.setEchoChar('•');
            }
        });
        pwRow.add(showPw, BorderLayout.EAST);
        card.add(pwRow, g);
    }

    private void addWide(JPanel card, GridBagConstraints g, String caption, JTextField field) {
        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;
        g.insets = new Insets(8, 8, 4, 8);
        card.add(Ui.caption(caption), g);
        g.gridy++;
        g.insets = new Insets(0, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        field.setPreferredSize(new Dimension(280, 40));
        card.add(field, g);
    }

    private void save() {
        try {
            String pinText = new String(pin.getPassword());
            if (!pinText.equals(new String(confirmPin.getPassword()))) {
                throw new AppException("Recovery PIN and Confirm Recovery PIN do not match.");
            }
            utangtracker.model.User created = auth.registerCustomer(firstName.getText(), lastName.getText(), username.getText(),
                    new String(password.getPassword()), new String(confirmPassword.getPassword()), contact.getText(), pinText);
            JOptionPane.showMessageDialog(this,
                    "Account created successfully!\n\nYour ID is CUST-" + created.getId() + ".\n\n" +
                    "Please present your valid ID to the store owner/admin for utang approval.\n" +
                    "You will be able to sign in after approval.",
                    "Registration Complete – Valid ID Required",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new LoginFrame().setVisible(true);
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Register", JOptionPane.WARNING_MESSAGE);
        }
    }
}

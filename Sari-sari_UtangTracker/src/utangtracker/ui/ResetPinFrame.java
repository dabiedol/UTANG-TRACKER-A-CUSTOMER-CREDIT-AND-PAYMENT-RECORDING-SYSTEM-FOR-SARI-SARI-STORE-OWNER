package utangtracker.ui;

import utangtracker.service.AppException;
import utangtracker.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.AbstractDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

/**
 * Forgot password / Reset PIN screen.
 *
 * Laid out like MoneyTrackerSystem's login/register screens: full-window
 * background, a title block, then one centered rounded card with a colored
 * header strip, "label on the left / pill field on the right" rows and a row of
 * flat colored buttons along the bottom.
 *
 * The customer proves who they are with the 4-digit recovery PIN they chose at
 * registration and then picks a new password (AuthService.resetPassword).
 * Staff and owner accounts cannot use PIN recovery - the owner resets those.
 */
public class ResetPinFrame extends JFrame {
    private static final Color CARD_BG = new Color(0xEA, 0xF3, 0xEC);
    private static final Color HEADER_BG = new Color(0x1F, 0x8A, 0x4C);
    private static final Color BLUE = new Color(0x1E, 0x6F, 0xD9);

    private final AuthService auth = new AuthService();
    private final Form form = new Form();

    public ResetPinFrame() {
        super("Utang Tracker System - Reset PIN");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Ui.applyResponsiveSize(this, 900, 700, new Dimension(640, 560));

        form.back.addActionListener(e -> backToLogin());
        form.clear.addActionListener(e -> form.clearFields());
        form.reset.addActionListener(e -> doReset());
        getRootPane().setDefaultButton(form.reset);

        JScrollPane scroller = new JScrollPane(form);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        scroller.getHorizontalScrollBar().setUnitIncrement(16);
        setContentPane(scroller);
    }

    // ------------------------------------------------------------ actions

    private void doReset() {
        try {
            auth.resetPassword(form.username.getText(),
                    new String(form.pin.getPassword()),
                    new String(form.newPassword.getPassword()),
                    new String(form.confirm.getPassword()));
            JOptionPane.showMessageDialog(this,
                    "Password updated. Sign in with your new password.",
                    "Reset PIN", JOptionPane.INFORMATION_MESSAGE);
            backToLogin();
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Reset PIN", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void backToLogin() {
        dispose();
        new LoginFrame().setVisible(true);
    }

    // ------------------------------------------------------------ UI

    /** The whole screen as a panel, so GuiShots can render it without opening a window. */
    static JPanel previewPanel() {
        return new Form();
    }

    private static final class Form extends BackgroundPanel {
        final JTextField username = new RoundedTextField("Your username");
        final JPasswordField pin = new RoundedPasswordField("4-digit PIN");
        final JPasswordField newPassword = new RoundedPasswordField("New password");
        final JPasswordField confirm = new RoundedPasswordField("Confirm new password");
        final JButton back = Ui.colored("BACK TO LOGIN", Ui.GREEN_DARK);
        final JButton clear = Ui.colored("CLEAR", BLUE);
        final JButton reset = Ui.colored("RESET PASSWORD", Ui.GREEN);

        Form() {
            super(new GridBagLayout());
            limitToDigits(pin, 4);

            JPanel column = new JPanel();
            column.setOpaque(false);
            column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));
            JPanel brand = brand();
            JPanel card = card();
            brand.setAlignmentX(LEFT_ALIGNMENT);
            card.setAlignmentX(LEFT_ALIGNMENT);
            column.add(brand);
            column.add(javax.swing.Box.createVerticalStrut(12));
            column.add(card);
            add(column, new GridBagConstraints());
        }

        void clearFields() {
            username.setText("");
            pin.setText("");
            newPassword.setText("");
            confirm.setText("");
            username.requestFocusInWindow();
        }

        /** "₱ UTANG TRACKER" + tagline on a dark translucent pill so it reads on the photo. */
        private JPanel brand() {
            RoundedPanel pill = new RoundedPanel(new GridBagLayout(), 26);
            pill.setBackground(new Color(0x0E, 0x3D, 0x24, 200));
            pill.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = 0;
            g.anchor = GridBagConstraints.WEST;

            JPanel title = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 0));
            title.setOpaque(false);
            JLabel a = new JLabel("₱  UTANG");
            a.setFont(new Font("Verdana", Font.BOLD, 32));
            a.setForeground(Color.WHITE);
            JLabel b = new JLabel("TRACKER");
            b.setFont(new Font("Verdana", Font.BOLD, 32));
            b.setForeground(new Color(0x7B, 0xE0, 0x9A));
            title.add(a);
            title.add(b);
            pill.add(title, g);

            g.gridy++;
            JLabel tag = new JLabel("Forgot your password?  ●  Verify with your PIN  ●  Get back in");
            tag.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 12));
            tag.setForeground(Color.WHITE);
            tag.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 0));
            pill.add(tag, g);
            return pill;
        }

        private JPanel card() {
            RoundedPanel card = new RoundedPanel(new BorderLayout(), 30);
            card.setBackground(CARD_BG);
            card.setPreferredSize(new Dimension(700, 450));

            // header strip (rounded on top, flat below)
            RoundedPanel header = new RoundedPanel(new BorderLayout(), 30);
            header.setSquareBottom(true);
            header.setBackground(HEADER_BG);
            header.setBorder(BorderFactory.createEmptyBorder(7, 24, 7, 24));
            JLabel h = new JLabel("Reset PIN  ●  Recover your account");
            h.setFont(new Font("SansSerif", Font.BOLD, 13));
            h.setForeground(Color.WHITE);
            header.add(h, BorderLayout.WEST);
            card.add(header, BorderLayout.NORTH);

            // rows: label on the left, pill field on the right
            JPanel rows = new JPanel(new GridBagLayout());
            rows.setOpaque(false);
            rows.setBorder(BorderFactory.createEmptyBorder(14, 28, 6, 28));
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = 0;
            g.gridwidth = 3;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.weightx = 1;
            g.insets = new Insets(0, 0, 14, 0);
            JLabel intro = new JLabel("<html>Enter your username and the 4-digit recovery PIN you chose when you registered, "
                    + "then pick a new password.</html>");
            intro.setFont(Ui.SMALL);
            intro.setForeground(Ui.MUTED);
            rows.add(intro, g);

            row(rows, 1, "Username:", username, null);
            row(rows, 2, "Recovery PIN:", pin, showToggle(pin));
            row(rows, 3, "New Password:", newPassword, showToggle(newPassword));
            row(rows, 4, "Confirm Password:", confirm, showToggle(confirm));

            g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = 5;
            g.gridwidth = 3;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.insets = new Insets(2, 0, 0, 0);
            JLabel rule = new JLabel("<html>Password: 8–12 characters with a capital letter, a number and a special character.<br>"
                    + "Five wrong PINs lock PIN reset for 15 minutes. Staff and owner accounts: ask the owner.</html>");
            rule.setFont(Ui.SMALL);
            rule.setForeground(Ui.MUTED);
            rows.add(rule, g);

            g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = 6;
            g.weighty = 1;
            rows.add(new JLabel(), g);
            card.add(rows, BorderLayout.CENTER);

            // button row, like REGISTER / LOGIN / FORGOT PASSWORD
            JPanel buttons = new JPanel(new GridLayout(1, 3, 18, 0));
            buttons.setOpaque(false);
            buttons.setBorder(BorderFactory.createEmptyBorder(8, 24, 24, 24));
            for (JButton button : new JButton[]{back, clear, reset}) {
                button.setPreferredSize(new Dimension(0, 40));
                buttons.add(button);
            }
            card.add(buttons, BorderLayout.SOUTH);
            return card;
        }

        private static void row(JPanel rows, int y, String text, JTextField field, JCheckBox toggle) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridy = y;
            g.insets = new Insets(0, 0, 14, 0);
            g.anchor = GridBagConstraints.WEST;

            JLabel label = new JLabel(text);
            label.setFont(new Font("SansSerif", Font.BOLD, 13));
            label.setForeground(Ui.TEXT);
            g.gridx = 0;
            g.weightx = 0;
            g.insets = new Insets(0, 0, 14, 16);
            label.setPreferredSize(new Dimension(150, 38));
            rows.add(label, g);

            g.gridx = 1;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.insets = new Insets(0, 0, 14, 0);
            field.setPreferredSize(new Dimension(300, 38));
            rows.add(field, g);

            g.gridx = 2;
            g.weightx = 0;
            g.fill = GridBagConstraints.NONE;
            g.insets = new Insets(0, 10, 14, 0);
            if (toggle != null) {
                toggle.setPreferredSize(new Dimension(64, 38));
                rows.add(toggle, g);
            } else {
                JLabel gap = new JLabel();
                gap.setPreferredSize(new Dimension(64, 38));
                rows.add(gap, g);
            }
        }

        private static JCheckBox showToggle(final JPasswordField field) {
            final JCheckBox show = new JCheckBox("Show", false);
            show.setOpaque(false);
            show.setFont(Ui.SMALL);
            show.setForeground(Ui.MUTED);
            show.setFocusPainted(false);
            show.setHorizontalAlignment(SwingConstants.LEFT);
            show.addActionListener(e -> field.setEchoChar(show.isSelected() ? (char) 0 : '•'));
            return show;
        }

        private static void limitToDigits(JPasswordField field, final int max) {
            ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
                    replace(fb, offset, 0, text, attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attr) throws BadLocationException {
                    String incoming = text == null ? "" : text.replaceAll("\\D", "");
                    int room = max - (fb.getDocument().getLength() - length);
                    if (room <= 0 && !incoming.isEmpty()) {
                        return;
                    }
                    if (incoming.length() > room) {
                        incoming = incoming.substring(0, Math.max(room, 0));
                    }
                    super.replace(fb, offset, length, incoming, attr);
                }
            });
        }
    }
}

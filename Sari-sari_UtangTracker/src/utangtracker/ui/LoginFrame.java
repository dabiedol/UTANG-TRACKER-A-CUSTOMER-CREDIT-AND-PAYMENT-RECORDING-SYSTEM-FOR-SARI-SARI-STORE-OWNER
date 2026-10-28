package utangtracker.ui;

import utangtracker.Session;
import utangtracker.model.Role;
import utangtracker.model.User;
import utangtracker.service.AppException;
import utangtracker.service.AuthService;

import javax.swing.ImageIcon;
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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginFrame extends JFrame {
    private final AuthService auth = new AuthService();
    private final JTextField username = Ui.field("Enter your username");
    private final JPasswordField password = Ui.password("Enter your password");

    public LoginFrame() {
        super("Utang Tracker System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Ui.applyResponsiveSize(this, 760, 560, new Dimension(520, 420));

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.add(topBar("UTANG TRACKER SYSTEM"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        JLabel logoLabel = new JLabel(Ui.storeLogoIcon(120));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 0, 8, 0));
        center.add(logoLabel, BorderLayout.NORTH);
        center.add(form(), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        JScrollPane scroller = new JScrollPane(root);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        scroller.getHorizontalScrollBar().setUnitIncrement(16);
        setContentPane(scroller);
    }

    static JPanel topBar(String title) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x2E, 0x7D, 0x32));
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 22));
        label.setForeground(Color.WHITE);
        bar.add(label);
        return bar;
    }

    private JPanel form() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        JPanel card = Ui.card();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(480, 430));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 8, 6, 8);

        JLabel title = new JLabel("Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(Ui.TEXT);
        card.add(title, g);

        g.gridy++;
        g.insets = new Insets(16, 8, 4, 8);
        card.add(Ui.caption("Username"), g);
        g.gridy++;
        g.insets = new Insets(0, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        username.setPreferredSize(new Dimension(280, 40));
        card.add(username, g);

        g.gridy++;
        g.insets = new Insets(8, 8, 4, 8);
        card.add(Ui.caption("Password"), g);
        g.gridy++;
        g.insets = new Insets(0, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        password.setPreferredSize(new Dimension(280, 40));

        // Password row with show/hide toggle
        JPanel pwRow = new JPanel(new BorderLayout(6, 0));
        pwRow.setOpaque(false);
        pwRow.add(password, BorderLayout.CENTER);
        JCheckBox showPw = new JCheckBox("Show");
        showPw.setOpaque(false);
        showPw.setForeground(Ui.MUTED);
        showPw.setFont(Ui.BODY);
        showPw.setFocusPainted(false);
        showPw.addActionListener(e -> {
            if (showPw.isSelected()) {
                password.setEchoChar((char) 0);
            } else {
                password.setEchoChar('•');
            }
        });
        pwRow.add(showPw, BorderLayout.EAST);
        card.add(pwRow, g);

        g.gridy++;
        g.insets = new Insets(16, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        JButton login = Ui.primary("Login");
        login.setPreferredSize(new Dimension(280, 42));
        login.addActionListener(e -> doLogin());
        card.add(login, g);
        getRootPane().setDefaultButton(login);

        g.gridy++;
        JLabel forgot = new JLabel("Forgot password?", SwingConstants.CENTER);
        forgot.setFont(Ui.BODY);
        forgot.setForeground(new Color(0x2E, 0x7D, 0x32));
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                resetPassword();
            }
        });
        card.add(forgot, g);

        g.gridy++;
        JLabel link = new JLabel("Don't have an account? Register", SwingConstants.CENTER);
        link.setFont(Ui.BODY);
        link.setForeground(new Color(0x2E, 0x7D, 0x32));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
                new RegisterFrame().setVisible(true);
            }
        });
        card.add(link, g);
        wrap.add(card);
        return wrap;
    }

    private void resetPassword() {
        // MoneyTracker-style Reset PIN screen (see ResetPinFrame)
        dispose();
        new ResetPinFrame().setVisible(true);
    }

    private void doLogin() {
        try {
            User user = auth.login(username.getText(), new String(password.getPassword()));
            Session.set(user);
            dispose();
            openHome(user);
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login", JOptionPane.WARNING_MESSAGE);
        }
    }

    public static void openHome(User user) {
        JFrame frame;
        if (user.getRole() == Role.CUSTOMER) {
            frame = new CustomerFrame();
        } else if (user.getRole() == Role.STAFF) {
            frame = new StaffFrame();
        } else {
            frame = new OwnerFrame();
        }
        frame.setVisible(true);
    }

    public static void logout(JFrame current) {
        Session.clear();
        current.dispose();
        new LoginFrame().setVisible(true);
    }
}

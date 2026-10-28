package utangtracker.ui;

import utangtracker.model.Role;
import utangtracker.model.User;
import utangtracker.Session;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;

public class GuiShots {
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "shots");
        out.mkdirs();
        save(login(), 760, 560, new File(out, "01-login.png"));
        save(register(), 820, 640, new File(out, "02-register.png"));
        save(customer(), 980, 680, new File(out, "03-customer.png"));
        save(staff(), 1040, 720, new File(out, "04-staff.png"));
        save(owner(), 1100, 720, new File(out, "05-owner.png"));
        save(ResetPinFrame.previewPanel(), 900, 700, new File(out, "06-reset-pin.png"));
    }

    private static void save(JPanel panel, int w, int h, File file) throws Exception {
        panel.setSize(w, h);
        panel.setPreferredSize(new Dimension(w, h));
        panel.addNotify();
        panel.validate();
        layoutDeep(panel);
        panel.validate();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        panel.printAll(g);
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    private static void layoutDeep(java.awt.Component component) {
        if (component instanceof java.awt.Container) {
            java.awt.Container container = (java.awt.Container) component;
            container.doLayout();
            for (java.awt.Component child : container.getComponents()) {
                layoutDeep(child);
            }
        }
    }

    private static JPanel login() {
        JPanel root = page();
        root.add(LoginFrame.topBar("UTANG TRACKER"), BorderLayout.NORTH);
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        JPanel card = Ui.card();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(460, 390));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 8, 6, 8);
        JLabel title = new JLabel("Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        card.add(title, g);
        g.gridy++;
        card.add(Ui.caption("Username"), g);
        g.gridy++;
        card.add(sized(Ui.field("Enter your username"), 400, 40), g);
        g.gridy++;
        card.add(Ui.caption("Password"), g);
        g.gridy++;
        JPasswordField password = Ui.password("Enter your password");
        card.add(sized(password, 400, 40), g);
        g.gridy++;
        JButton login = Ui.primary("Login");
        card.add(sized(login, 400, 42), g);
        g.gridy++;
        JLabel link = new JLabel("Don't have an account? Register", SwingConstants.CENTER);
        link.setForeground(new Color(0x2E, 0x7D, 0x32));
        card.add(link, g);
        g.gridy++;
        JLabel demo = new JLabel("<html><center>owner / owner123<br>staff / staff123<br>maria / maria123</center></html>", SwingConstants.CENTER);
        demo.setForeground(Ui.MUTED);
        card.add(demo, g);
        wrap.add(card);
        root.add(wrap, BorderLayout.CENTER);
        return root;
    }

    private static JPanel register() {
        JPanel root = page();
        root.add(LoginFrame.topBar("UTANG TRACKER"), BorderLayout.NORTH);
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        JPanel card = Ui.card();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(640, 500));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 8, 8, 8);
        JLabel title = new JLabel("Customer Account Registration", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        card.add(title, g);
        g.gridwidth = 1;
        g.gridy++;
        card.add(Ui.caption("First name"), g);
        g.gridx = 1;
        card.add(Ui.caption("LastName"), g);
        g.gridy++;
        g.gridx = 0;
        card.add(sized(Ui.field("Enter your first name"), 280, 40), g);
        g.gridx = 1;
        card.add(sized(Ui.field("Enter your last name"), 280, 40), g);
        addWide(card, g, "Contact Number", "09XXXXXXXXX");
        addWide(card, g, "Username", "Choose a username");
        addWide(card, g, "Password", "Create a password");
        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;
        card.add(sized(Ui.primary("Register"), 580, 42), g);
        g.gridy++;
        JLabel back = new JLabel("Back to Login", SwingConstants.CENTER);
        back.setForeground(new Color(0x2E, 0x7D, 0x32));
        card.add(back, g);
        wrap.add(card);
        root.add(wrap, BorderLayout.CENTER);
        return root;
    }

    private static void addWide(JPanel card, GridBagConstraints g, String caption, String placeholder) {
        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;
        card.add(Ui.caption(caption), g);
        g.gridy++;
        card.add(sized(Ui.field(placeholder), 580, 40), g);
    }

    private static JPanel customer() {
        Session.set(new User(1, "Maria", "Santos", "maria", "09171234567", Role.CUSTOMER, true));
        JPanel root = page();
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x2E, 0x7D, 0x32));
        bar.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 18, 12, 16));
        JLabel brand = new JLabel("Utang Tracker  |  Customer Portal");
        brand.setFont(new Font("SansSerif", Font.BOLD, 16));
        brand.setForeground(Color.WHITE);
        bar.add(brand, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        JLabel who = new JLabel("Maria Santos");
        who.setForeground(Color.WHITE);
        right.add(who);
        right.add(sized(Ui.dark("Logout"), 100, 32));
        bar.add(right, BorderLayout.EAST);
        root.add(bar, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 24, 28));
        JPanel balanceCard = Ui.card();
        balanceCard.setLayout(new BorderLayout());
        JLabel caption = new JLabel("TOTAL UNPAID BALANCE", SwingConstants.CENTER);
        caption.setForeground(Ui.MUTED);
        JLabel balance = new JLabel("PHP 45.00", SwingConstants.CENTER);
        balance.setFont(new Font("SansSerif", Font.BOLD, 42));
        balance.setForeground(new Color(0x2E, 0x7D, 0x32));
        balanceCard.add(caption, BorderLayout.NORTH);
        balanceCard.add(balance, BorderLayout.CENTER);
        body.add(balanceCard, BorderLayout.NORTH);
        JPanel history = Ui.card();
        history.setLayout(new BorderLayout(0, 12));
        history.add(Ui.heading("Transaction History"), BorderLayout.NORTH);
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Date Borrowed", "Item Description", "Amount", "Note", "Status"}, 0);
        model.addRow(new Object[] {"2026-10-04 14:30:00", "1 pack noodles", "PHP 15.00", "—", "Unpaid"});
        model.addRow(new Object[] {"2026-10-05 09:15:00", "1 canned sardines", "PHP 30.00", "—", "Unpaid"});
        JTable table = new JTable(model);
        Ui.styleTable(table);
        history.add(Ui.scroll(table), BorderLayout.CENTER);
        body.add(history, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private static JPanel staff() {
        JPanel root = page();
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x2E, 0x7D, 0x32));
        bar.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 18, 12, 16));
        JLabel brand = new JLabel("Utang Tracker  |  Staff POS");
        brand.setFont(new Font("SansSerif", Font.BOLD, 16));
        brand.setForeground(Color.WHITE);
        bar.add(brand, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        JLabel who = new JLabel("Ana Reyes");
        who.setForeground(Color.WHITE);
        right.add(who);
        right.add(sized(Ui.dark("Logout"), 100, 32));
        bar.add(right, BorderLayout.EAST);
        root.add(bar, BorderLayout.NORTH);
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setOpaque(false);
        page.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel top = new JPanel(new GridLayout(1, 2, 16, 0));
        top.setOpaque(false);
        JPanel find = Ui.card();
        find.setLayout(new BorderLayout(0, 10));
        find.add(Ui.heading("Find Customer"), BorderLayout.NORTH);
        JPanel names = new JPanel(new GridLayout(0, 1, 0, 6));
        names.setBackground(Color.WHITE);
        names.add(new JLabel("Maria Santos    PHP 45.00"));
        names.add(new JLabel("Juan Dela Cruz    PHP 30.00"));
        names.add(new JLabel("Liza Garcia    PHP 25.00"));
        find.add(sized(Ui.field("Search by name or account ID..."), 360, 40), BorderLayout.CENTER);
        find.add(names, BorderLayout.SOUTH);
        JPanel form = Ui.card();
        form.setLayout(new GridLayout(0, 1, 0, 8));
        form.add(Ui.heading("New Transaction"));
        form.add(new JLabel("Maria Santos   ·   PHP 45.00"));
        form.add(Ui.caption("Item Description"));
        form.add(Ui.field("e.g. 1kg Rice, 2 Canned Sardines"));
        form.add(Ui.caption("Amount (PHP)"));
        form.add(Ui.field("0.00"));
        form.add(sized(Ui.primary("Add to Utang"), 180, 40));
        top.add(find);
        top.add(form);
        page.add(top, BorderLayout.CENTER);
        JPanel recent = Ui.card();
        recent.setLayout(new BorderLayout(0, 8));
        recent.add(Ui.heading("Recent Transactions"), BorderLayout.NORTH);
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Date & Time", "Item Description", "Amount", "Status"}, 0);
        model.addRow(new Object[] {"2026-10-05 09:15:00", "1 canned sardines", "PHP 30.00", "Unpaid"});
        model.addRow(new Object[] {"2026-10-04 14:30:00", "1 pack noodles", "PHP 15.00", "Unpaid"});
        JTable table = new JTable(model);
        Ui.styleTable(table);
        recent.add(new JScrollPane(table), BorderLayout.CENTER);
        recent.setPreferredSize(new Dimension(0, 180));
        page.add(recent, BorderLayout.SOUTH);
        root.add(page, BorderLayout.CENTER);
        return root;
    }

    private static JPanel owner() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(0xF4, 0xF6, 0xF5));
        JPanel side = new JPanel(new GridLayout(0, 1));
        side.setBackground(Color.WHITE);
        side.setPreferredSize(new Dimension(230, 0));
        JLabel brand = new JLabel("UTANG TRACKER");
        brand.setFont(new Font("SansSerif", Font.BOLD, 16));
        brand.setForeground(new Color(0x2E, 0x7D, 0x32));
        side.add(brand);
        side.add(new JLabel("Dashboard"));
        JLabel accounts = new JLabel("Customer Accounts");
        accounts.setOpaque(true);
        accounts.setBackground(new Color(0xE8, 0xF5, 0xE9));
        accounts.setForeground(new Color(0x2E, 0x7D, 0x32));
        side.add(accounts);
        side.add(new JLabel("Transaction Logs"));
        side.add(new JLabel("Logout"));
        root.add(side, BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setOpaque(false);
        main.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JLabel title = new JLabel("Owner Command Center");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        main.add(title, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(stat("Total Outstanding Utang", "PHP 70.00"));
        stats.add(stat("Active Customers", "2"));
        stats.add(stat("Transactions Today", "1"));
        center.add(stats, BorderLayout.NORTH);
        JPanel tableCard = Ui.card();
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.add(Ui.heading("Customer Accounts"), BorderLayout.NORTH);
        DefaultTableModel model = new DefaultTableModel(
                new Object[] {"Account ID", "First Name", "Last Name", "Contact Number", "Current Balance", "Action"}, 0);
        model.addRow(new Object[] {"CUST-1", "Maria", "Santos", "09171234567", "PHP 45.00", "View"});
        model.addRow(new Object[] {"CUST-2", "Juan", "Dela Cruz", "09181234567", "PHP 30.00", "View"});
        model.addRow(new Object[] {"CUST-3", "Liza", "Garcia", "09201234567", "PHP 25.00", "View"});
        JTable table = new JTable(model);
        Ui.styleTable(table);
        tableCard.add(Ui.scroll(table), BorderLayout.CENTER);
        center.add(tableCard, BorderLayout.CENTER);
        main.add(center, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
        return root;
    }

    private static JPanel stat(String caption, String value) {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout());
        JLabel label = new JLabel(caption);
        label.setForeground(Ui.MUTED);
        JLabel amount = new JLabel(value);
        amount.setFont(new Font("SansSerif", Font.BOLD, 26));
        amount.setForeground(new Color(0x2E, 0x7D, 0x32));
        card.add(label, BorderLayout.NORTH);
        card.add(amount, BorderLayout.CENTER);
        return card;
    }

    private static JPanel page() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(0xF7, 0xF8, 0xF7));
        return root;
    }

    private static JTextField sized(JTextField field, int w, int h) {
        field.setPreferredSize(new Dimension(w, h));
        return field;
    }

    private static JButton sized(JButton button, int w, int h) {
        button.setPreferredSize(new Dimension(w, h));
        button.setSize(w, h);
        return button;
    }
}

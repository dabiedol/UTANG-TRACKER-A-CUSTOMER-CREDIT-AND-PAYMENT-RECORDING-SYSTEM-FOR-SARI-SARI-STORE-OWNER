package utangtracker.ui;

import utangtracker.Session;
import utangtracker.model.CustomerBalance;
import utangtracker.model.LedgerEntry;
import utangtracker.model.User;
import utangtracker.service.AppException;
import utangtracker.service.AuthService;
import utangtracker.service.UtangService;
import utangtracker.util.AppTime;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public class StaffFrame extends JFrame {
    private final UtangService utang = new UtangService();
    private final AuthService auth = new AuthService();
    private final JTextField search = Ui.field("Search by name or account ID...");
    private final DefaultListModel<CustomerBalance> customers = new DefaultListModel<CustomerBalance>();
    private final JList<CustomerBalance> list = new JList<CustomerBalance>(customers);
    private final JTextField item = Ui.field("e.g. Rice, Canned Sardines");
    private final JTextField quantity = Ui.field("1");
    private final JTextField unitPrice = Ui.field("0.00");
    private final JTextField amount = Ui.field("0.00");
    private final JTextField note = Ui.field("Optional note");
    private final JLabel selected = new JLabel("No customer selected");
    private final DefaultTableModel history = new DefaultTableModel(
            new Object[] {"Date & Time", "Item Description", "Qty", "Unit Price", "Amount", "Staff", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private List<CustomerBalance> loaded = new ArrayList<CustomerBalance>();

    public StaffFrame() {
        super("Utang Tracker System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Ui.applyResponsiveSize(this, 1040, 720, new Dimension(760, 520));

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.add(Ui.appHeader("Utang Tracker System  |  Staff POS",
                Session.get().getFirstName() + " " + Session.get().getLastName(), this), BorderLayout.NORTH);
        root.add(body(), BorderLayout.CENTER);
        JScrollPane scroller = new JScrollPane(root);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        scroller.getHorizontalScrollBar().setUnitIncrement(16);
        setContentPane(scroller);
        reloadCustomers();
    }

    private JPanel header() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x2E, 0x7D, 0x32));
        bar.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 18, 12, 16));
        JLabel brand = new JLabel("Utang Tracker System  |  Staff POS");
        brand.setFont(new Font("SansSerif", Font.BOLD, 16));
        brand.setForeground(Color.WHITE);
        brand.setIcon(Ui.storeLogoIcon(28));
        brand.setIconTextGap(8);
        bar.add(brand, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        JLabel who = new JLabel(Session.get().getFirstName() + " " + Session.get().getLastName());
        who.setForeground(Color.WHITE);
        who.setFont(Ui.BODY);
        JButton out = Ui.dark("Logout");
        out.setPreferredSize(new Dimension(100, 32));
        out.addActionListener(e -> LoginFrame.logout(this));
        JButton create = Ui.dark("Create Customer");
        create.setPreferredSize(new Dimension(140, 32));
        create.addActionListener(e -> createCustomer());
        right.add(create);
        right.add(who);
        right.add(out);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel body() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setOpaque(false);
        page.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 0.42;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(0, 0, 0, 8);
        top.add(findCard(), g);
        g.gridx = 1;
        g.weightx = 0.58;
        g.insets = new Insets(0, 8, 0, 0);
        top.add(transactionCard(), g);
        page.add(top, BorderLayout.CENTER);
        page.add(recentCard(), BorderLayout.SOUTH);
        return page;
    }

    private JPanel findCard() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 10));
        card.add(Ui.heading("Find Customer"), BorderLayout.NORTH);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        JPanel stack = new JPanel(new BorderLayout(0, 8));
        stack.setOpaque(false);
        stack.add(search, BorderLayout.NORTH);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(48);
        list.setCellRenderer(new CustomerRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelected();
            }
        });
        stack.add(Ui.scroll(list), BorderLayout.CENTER);
        card.add(stack, BorderLayout.CENTER);
        return card;
    }

    private JPanel transactionCard() {
        JPanel card = Ui.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.insets = new Insets(4, 0, 4, 0);
        card.add(Ui.heading("New Transaction"), g);
        g.gridy++;
        selected.setFont(Ui.BODY);
        selected.setForeground(Ui.MUTED);
        card.add(selected, g);
        g.gridy++;
        card.add(Ui.caption("Item Description"), g);
        g.gridy++;
        item.setPreferredSize(new Dimension(240, 40));
        card.add(item, g);
        g.gridy++;
        card.add(Ui.caption("Quantity"), g);
        g.gridy++;
        quantity.setPreferredSize(new Dimension(240, 40));
        card.add(quantity, g);
        g.gridy++;
        card.add(Ui.caption("Unit Price (PHP)"), g);
        g.gridy++;
        unitPrice.setPreferredSize(new Dimension(240, 40));
        card.add(unitPrice, g);
        // Auto-calculate total
        javax.swing.event.DocumentListener calc = new javax.swing.event.DocumentListener() {
            public void insertUpdate(DocumentEvent e) { recalc(); }
            public void removeUpdate(DocumentEvent e) { recalc(); }
            public void changedUpdate(DocumentEvent e) { recalc(); }
        };
        quantity.getDocument().addDocumentListener(calc);
        unitPrice.getDocument().addDocumentListener(calc);
        g.gridy++;
        card.add(Ui.caption("Total Amount (auto)"), g);
        g.gridy++;
        amount.setPreferredSize(new Dimension(240, 40));
        amount.setEditable(false);
        amount.setBackground(new Color(0xF0, 0xF0, 0xF0));
        card.add(amount, g);
        g.gridy++;
        card.add(Ui.caption("Note (optional)"), g);
        g.gridy++;
        note.setPreferredSize(new Dimension(240, 40));
        card.add(note, g);
        g.gridy++;
        g.insets = new Insets(16, 0, 4, 0);
        JButton add = Ui.primary("Add to Utang");
        add.setPreferredSize(new Dimension(140, 40));
        add.addActionListener(e -> addUtang());
        JButton pay = Ui.dark("Pay Utang");
        pay.setPreferredSize(new Dimension(130, 40));
        pay.addActionListener(e -> payUtang());
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(add);
        actions.add(pay);
        card.add(actions, g);
        return card;
    }

    private JPanel recentCard() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 8));
        card.setPreferredSize(new Dimension(0, 150));
        card.add(Ui.heading("Recent Transactions"), BorderLayout.NORTH);
        JTable table = new JTable(history);
        Ui.styleTable(table);
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        return card;
    }

    private void reloadCustomers() {
        try {
            loaded = utang.searchCustomers("");
            filter();
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Staff POS", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void filter() {
        String needle = search.getText().trim().toLowerCase();
        CustomerBalance previous = list.getSelectedValue();
        customers.clear();
        for (CustomerBalance row : loaded) {
            String blob = (row.getCustomer().getFirstName() + " " + row.getCustomer().getLastName()
                    + " " + row.getCustomer().getId() + " " + row.getCustomer().getContact()).toLowerCase();
            if (needle.isEmpty() || blob.contains(needle)) {
                customers.addElement(row);
            }
        }
        if (previous != null) {
            for (int i = 0; i < customers.size(); i++) {
                if (customers.get(i).getCustomer().getId() == previous.getCustomer().getId()) {
                    list.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private void showSelected() {
        CustomerBalance row = list.getSelectedValue();
        history.setRowCount(0);
        if (row == null) {
            selected.setText("No customer selected");
            return;
        }
        selected.setText("CUST-" + row.getCustomer().getId() + "   "
                + row.getCustomer().getFirstName() + " " + row.getCustomer().getLastName()
                + "   ·   " + AppTime.peso(row.getBalance()));
        try {
            List<LedgerEntry> rows = utang.historyFor(row.getCustomer().getId());
            for (LedgerEntry entry : rows) {
                history.addRow(new Object[] {
                        entry.getCreatedAt(),
                        entry.getItemDescription(),
                        String.format("%.2f", entry.getQuantity()),
                        AppTime.peso(entry.getUnitPrice()),
                        AppTime.peso(entry.getAmount()),
                        (entry.getStaffFirstName() == null ? "System" : entry.getStaffFirstName() + " " + entry.getStaffLastName()),
                        entry.isUtang() ? "Unpaid" : "Paid"
                });
            }
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Staff POS", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void recalc() {
        try {
            double q = Double.parseDouble(quantity.getText().trim());
            double u = Double.parseDouble(unitPrice.getText().trim());
            if (q > 0 && u > 0) {
                amount.setText(String.format("%.2f", q * u));
            } else {
                amount.setText("0.00");
            }
        } catch (NumberFormatException ex) {
            amount.setText("0.00");
        }
    }

    private void createCustomer() {
        JTextField first = Ui.field("First name");
        JTextField last = Ui.field("Last name");
        JTextField contact = Ui.field("09XXXXXXXXX");
        JTextField user = Ui.field("Username");
        JPasswordField pass = Ui.password("Password (8-12, capital, number, special)");
        JPasswordField pin = Ui.password("4-digit Recovery PIN");

        JPanel form = new JPanel(new java.awt.GridLayout(0, 1, 0, 6));
        form.add(new JLabel("First name"));
        form.add(first);
        form.add(new JLabel("Last name"));
        form.add(last);
        form.add(new JLabel("Contact Number"));
        form.add(contact);
        form.add(new JLabel("Username"));
        form.add(user);
        form.add(new JLabel("Password"));
        form.add(pass);
        form.add(new JLabel("4-digit Recovery PIN"));
        form.add(pin);

        int ok = JOptionPane.showConfirmDialog(this, form, "Create Customer Account", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }

        int idConfirm = JOptionPane.showConfirmDialog(this,
                "Has the customer presented a valid ID for utang approval?",
                "Valid ID Confirmation", JOptionPane.YES_NO_OPTION);
        if (idConfirm != JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, "Account creation cancelled. Valid ID is required.");
            return;
        }

        try {
            utangtracker.model.User created = auth.registerCustomer(
                    first.getText(), last.getText(), user.getText(),
                    new String(pass.getPassword()), new String(pass.getPassword()),
                    contact.getText(), new String(pin.getPassword()));
            JOptionPane.showMessageDialog(this,
                    "Customer account created.\nID: CUST-" + created.getId() + "\nUsername: " + created.getUsername(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            reloadCustomers();
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Create Customer", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void addUtang() {
        CustomerBalance row = list.getSelectedValue();
        if (row == null) {
            JOptionPane.showMessageDialog(this, "Select a customer first.");
            return;
        }
        try {
            double q = Double.parseDouble(quantity.getText().trim());
            double u = Double.parseDouble(unitPrice.getText().trim());
            LedgerEntry entry = utang.addUtang(row.getCustomer().getId(), item.getText(), q, u, cleanNote());
            item.setText("");
            note.setText("");
            quantity.setText("1");
            unitPrice.setText("");
            amount.setText("0.00");
            reloadCustomers();
            showReceipt(entry, row.getCustomer());
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Add to Utang", JOptionPane.WARNING_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid quantity and unit price.", "Add to Utang", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Optional note: blank is fine; trimmed and capped at the 255-char column limit. */
    private String cleanNote() {
        String n = note.getText() == null ? "" : note.getText().trim();
        return n.length() > 255 ? n.substring(0, 255) : n;
    }

    private void showReceipt(LedgerEntry entry, User customer) {
        String receipt = "UTANG TRACKER SYSTEM - RECEIPT\n"
                + "----------------------------\n"
                + "Date: " + entry.getCreatedAt() + "\n"
                + "Customer: " + customer.getFirstName() + " " + customer.getLastName() + " (CUST-" + customer.getId() + ")\n"
                + "Item: " + entry.getItemDescription() + "\n"
                + "Quantity: " + String.format("%.2f", entry.getQuantity()) + "\n"
                + "Unit Price: " + AppTime.peso(entry.getUnitPrice()) + "\n"
                + "Total Amount: " + AppTime.peso(entry.getAmount()) + "\n"
                + (entry.getNote() == null || entry.getNote().isEmpty() ? "" : "Note: " + entry.getNote() + "\n")
                + "Status: Unpaid (Utang)\n"
                + "----------------------------\n"
                + "Thank you!";
        JOptionPane.showMessageDialog(this, receipt, "Receipt", JOptionPane.INFORMATION_MESSAGE);
    }

    private void payUtang() {
        CustomerBalance row = list.getSelectedValue();
        if (row == null) {
            JOptionPane.showMessageDialog(this, "Select a customer first.");
            return;
        }
        String typed = JOptionPane.showInputDialog(this, "Payment amount for "
                + row.getCustomer().getFirstName() + " " + row.getCustomer().getLastName()
                + " (" + AppTime.peso(row.getBalance()) + ")");
        if (typed == null || typed.trim().isEmpty()) {
            return;
        }
        try {
            utang.acceptPayment(row.getCustomer().getId(), Double.parseDouble(typed.trim()), cleanNote());
            note.setText("");
            reloadCustomers();
            JOptionPane.showMessageDialog(this, "Payment recorded.");
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Pay Utang", JOptionPane.WARNING_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount.", "Pay Utang", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static class CustomerRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            CustomerBalance row = (CustomerBalance) value;
            label.setText("CUST-" + row.getCustomer().getId() + "   "
                    + row.getCustomer().getFirstName() + " " + row.getCustomer().getLastName()
                    + "    " + AppTime.peso(row.getBalance()));
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 10, 4, 10));
            if (!selected) {
                label.setBackground(Color.WHITE);
            }
            return label;
        }
    }
}

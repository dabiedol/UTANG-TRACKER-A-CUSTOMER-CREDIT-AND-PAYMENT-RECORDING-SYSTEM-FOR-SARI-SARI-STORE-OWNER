package utangtracker.ui;

import utangtracker.Session;
import utangtracker.model.CustomerBalance;
import utangtracker.model.KpiSnapshot;
import utangtracker.model.LedgerEntry;
import utangtracker.model.User;
import utangtracker.service.AppException;
import utangtracker.service.AuthService;
import utangtracker.service.UtangService;
import utangtracker.util.AppTime;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class OwnerFrame extends JFrame {
    private final UtangService utang = new UtangService();
    private final AuthService auth = new AuthService();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel totalUtang = stat();
    private final JLabel activeCustomers = stat();
    private final JLabel today = stat();
    private final JLabel dashTotal = stat();
    private final JLabel dashCustomers = stat();
    private final JLabel dashToday = stat();
    private final JTextField ownerCustomerSearch = Ui.field("Search by name or account ID...");
    private final DefaultListModel<CustomerBalance> ownerCustomers = new DefaultListModel<CustomerBalance>();
    private final JList<CustomerBalance> ownerList = new JList<CustomerBalance>(ownerCustomers);
    private final JTextField ownerItem = Ui.field("e.g. Rice, Canned Sardines");
    private final JTextField ownerQty = Ui.field("1");
    private final JTextField ownerUnit = Ui.field("0.00");
    private final JTextField ownerAmount = Ui.field("0.00");
    private final JTextField payAmount = Ui.field("0.00");
    private final DefaultTableModel accounts = new DefaultTableModel(
            new Object[] {"Account ID", "First Name", "Last Name", "Username", "Contact Number", "Current Balance", "Status", "Action"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel dashLogs = new DefaultTableModel(
            new Object[] {"Created Date&Time", "Customer", "Item Description", "Amount", "Staff", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel logs = new DefaultTableModel(
            new Object[] {"Created Date&Time", "Account ID", "First Name", "Last Name", "Staff", "Item Description", "Amount", "Activity", "Note"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel paidLogs = new DefaultTableModel(
            new Object[] {"Date Paid", "Account ID", "First Name", "Last Name", "Staff", "Amount Paid", "Note"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTextField accountSearch = Ui.field("Search Customer by ID or Name...");
    private final DefaultTableModel dailyRows = new DefaultTableModel(
            new Object[] {"Date & Time", "Customer", "Item", "Qty", "Unit", "Amount", "Staff", "Type"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final DefaultTableModel reportRows = new DefaultTableModel(
            new Object[] {"Account ID", "First Name", "Last Name", "Current Balance"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final DefaultTableModel auditRows = new DefaultTableModel(
            new Object[] {"When", "Who", "Action", "Target", "Detail"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JLabel weekCollections = stat();
    private final JLabel dailyUtangTotal = stat();
    private final JLabel dailyPaymentTotal = stat();
    private List<CustomerBalance> accountRows;
    private final DefaultListModel<User> staffModel = new DefaultListModel<User>();
    private final JList<User> staffList = new JList<User>(staffModel);
    private final DefaultTableModel staffActivity = new DefaultTableModel(
            new Object[] {"Date & Time", "Customer", "Item", "Qty", "Amount", "Type"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JLabel staffSummary = new JLabel("Select a staff member");
    private final JTextField logsDate = Ui.field("YYYY-MM-DD");
    private final JTextField paidDate = Ui.field("YYYY-MM-DD");
    private final JTextField reportsDate = Ui.field("YYYY-MM-DD");
    private final JTextField auditDate = Ui.field("YYYY-MM-DD");
    private List<LedgerEntry> allLedgerCache = new ArrayList<LedgerEntry>();
    private List<String[]> allAuditCache = new ArrayList<String[]>();
    private JLabel dashboardNav;
    private JLabel accountsNav;
    private JLabel logsNav;
    private JLabel paidNav;
    private JLabel pendingNav;
    private final DefaultTableModel pendingRows = new DefaultTableModel(
            new Object[] {"Account ID", "First Name", "Last Name", "Contact", "Username", "Action"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };

    public OwnerFrame() {
        super("Utang Tracker System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Ui.applyResponsiveSize(this, 1100, 720, new Dimension(800, 520));

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.add(sidebar(), BorderLayout.WEST);
        content.setOpaque(false);
        content.add(dashboard(), "dashboard");
        content.add(accountsPage(), "accounts");
        content.add(logsPage(), "logs");
        content.add(paidPage(), "paid");
        content.add(reportsPage(), "reports");
        content.add(auditPage(), "audit");
        content.add(staffPage(), "staff");
        content.add(pendingPage(), "pending");
        root.add(content, BorderLayout.CENTER);
        JScrollPane scroller = new JScrollPane(root);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        scroller.getHorizontalScrollBar().setUnitIncrement(16);
        setContentPane(scroller);
        showPage("dashboard");
        reload();
    }

    private JPanel sidebar() {
        JPanel side = new JPanel();
        side.setLayout(new javax.swing.BoxLayout(side, javax.swing.BoxLayout.Y_AXIS));
        side.setBackground(Color.WHITE);
        side.setPreferredSize(new Dimension(220, 0));
        side.setMinimumSize(new Dimension(160, 0));
        side.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, Ui.LINE));

        JLabel brand = new JLabel("<html>UTANG TRACKER<br>SYSTEM</html>");
        brand.setFont(new Font("SansSerif", Font.BOLD, 15));
        brand.setForeground(new Color(0x1B, 0x6B, 0x32));
        brand.setIcon(Ui.storeLogoIcon(36));
        brand.setIconTextGap(8);
        brand.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 14, 12, 10));
        brand.setAlignmentX(LEFT_ALIGNMENT);
        side.add(brand);

        dashboardNav = nav("Dashboard", "dashboard");
        accountsNav = nav("Customer Accounts", "accounts");
        logsNav = nav("Activity Logs", "logs");
        paidNav = nav("Paid Utang", "paid");
        JLabel reportsNav = nav("Reports", "reports");
        JLabel auditNav = nav("Audit Log", "audit");
        JLabel staffNav = nav("Staff Management", "staff");
        pendingNav = nav("Pending Approvals", "pending");
        side.add(dashboardNav);
        side.add(accountsNav);
        side.add(logsNav);
        side.add(paidNav);
        side.add(reportsNav);
        side.add(auditNav);
        side.add(staffNav);
        side.add(pendingNav);
        side.add(javax.swing.Box.createVerticalGlue());

        side.add(javax.swing.Box.createVerticalStrut(16));
        return side;
    }

    private JLabel nav(String text, final String page) {
        final JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setForeground(Ui.TEXT);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(9, 18, 9, 18));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (!"logout".equals(page)) {
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showPage(page);
                }
            });
        }
        return label;
    }

    private void showPage(String page) {
        cards.show(content, page);
        paintNav(dashboardNav, "dashboard".equals(page));
        paintNav(accountsNav, "accounts".equals(page));
        paintNav(logsNav, "logs".equals(page));
        paintNav(paidNav, "paid".equals(page));
        paintNav(pendingNav, "pending".equals(page));
    }

    private void paintNav(JLabel label, boolean active) {
        label.setBackground(active ? new Color(0xE8, 0xF5, 0xE9) : Color.WHITE);
        label.setForeground(active ? new Color(0x2E, 0x7D, 0x32) : Ui.TEXT);
    }

    private JPanel dashboard() {
        JPanel page = page("Owner Counter");
        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 12, 0));
        stats.add(Ui.statCard("Total Outstanding Utang", dashTotal, "wallet"));
        stats.add(Ui.statCard("Active Customers", dashCustomers, "people"));
        stats.add(Ui.statCard("Transactions Today", dashToday, "calendar"));

        JPanel top = new JPanel(new GridLayout(1, 2, 12, 0));
        top.setOpaque(false);
        JPanel find = Ui.card();
        find.setLayout(new BorderLayout(0, 10));
        find.add(Ui.heading("Find Customer"), BorderLayout.NORTH);
        ownerCustomerSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterOwnerCustomers(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterOwnerCustomers(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterOwnerCustomers(); }
        });
        JPanel stack = new JPanel(new BorderLayout(0, 8));
        stack.setOpaque(false);
        stack.add(ownerCustomerSearch, BorderLayout.NORTH);
        ownerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ownerList.setFixedCellHeight(48);
        ownerList.setCellRenderer(new OwnerCustomerRenderer());
        stack.add(Ui.scroll(ownerList), BorderLayout.CENTER);
        find.add(stack, BorderLayout.CENTER);
        JPanel form = Ui.card();
        form.setLayout(new GridLayout(0, 1, 0, 6));
        form.add(Ui.heading("Add Utang"));
        form.add(Ui.caption("Item Description"));
        form.add(ownerItem);
        form.add(Ui.caption("Quantity"));
        form.add(ownerQty);
        form.add(Ui.caption("Unit Price (PHP)"));
        form.add(ownerUnit);
        form.add(Ui.caption("Total Amount (auto)"));
        ownerAmount.setEditable(false);
        ownerAmount.setBackground(new Color(0xF0, 0xF0, 0xF0));
        form.add(ownerAmount);
        // Auto-calc for owner form
        javax.swing.event.DocumentListener ownerCalc = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { ownerRecalc(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { ownerRecalc(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { ownerRecalc(); }
        };
        ownerQty.getDocument().addDocumentListener(ownerCalc);
        ownerUnit.getDocument().addDocumentListener(ownerCalc);
        JButton add = Ui.primary("Add to Utang");
        add.addActionListener(e -> ownerAddUtang());
        JButton pay = Ui.dark("Pay Utang");
        pay.addActionListener(e -> recordPayment());
        JButton resetPin = Ui.ghost("Reset User PIN");
        resetPin.addActionListener(e -> resetUserPin());
        form.add(add);
        form.add(pay);
        form.add(resetPin);
        top.add(find);
        top.add(form);

        JPanel tableCard = Ui.card();
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.add(Ui.heading("Recent Transactions"), BorderLayout.NORTH);
        JTable table = new JTable(dashLogs);
        Ui.styleTable(table);
        Ui.enableLongCellPopup(table);
        tableCard.add(Ui.scroll(table), BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout(0, 12));
        wrap.setOpaque(false);
        wrap.add(stats, BorderLayout.NORTH);
        wrap.add(top, BorderLayout.CENTER);
        wrap.add(tableCard, BorderLayout.SOUTH);
        tableCard.setPreferredSize(new Dimension(0, 160));
        page.add(wrap, BorderLayout.CENTER);
        return page;
    }

    private JPanel accountsPage() {
        JPanel page = page("Owner Command Center");
        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 14, 0));
        stats.add(Ui.statCard("Total Outstanding Utang", totalUtang, "wallet"));
        stats.add(Ui.statCard("Active Customers", activeCustomers, "people"));
        stats.add(Ui.statCard("Transactions Today", today, "calendar"));

        JPanel wrap = new JPanel(new BorderLayout(0, 12));
        wrap.setOpaque(false);
        wrap.add(stats, BorderLayout.NORTH);
        JPanel tableCard = Ui.card();
        tableCard.setLayout(new BorderLayout(0, 10));
        accountSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { reload(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { reload(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { reload(); }
        });
        JTable table = new JTable(accounts);
        Ui.styleTable(table);
        Ui.enableLongCellPopup(table);
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        JButton updateBtn = Ui.dark("Update Customer");
        updateBtn.addActionListener(e -> updateSelectedCustomer(table));
        JButton deleteBtn = Ui.danger("Delete Customer");
        deleteBtn.addActionListener(e -> deleteSelectedCustomer(table));
        actions.add(updateBtn);
        actions.add(deleteBtn);
        // Heading, action buttons and search all sit ABOVE the table so they are always
        // visible, even on a short window (they used to be squeezed out at the bottom).
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(Ui.heading("Customer Accounts (Active + Pending Approvals)"), BorderLayout.NORTH);
        top.add(actions, BorderLayout.CENTER);
        top.add(accountSearch, BorderLayout.SOUTH);
        tableCard.add(top, BorderLayout.NORTH);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0) return;
                int modelRow = table.convertRowIndexToModel(row);
                String action = String.valueOf(accounts.getValueAt(modelRow, 7));
                if (col == 7 && "Approve".equals(action)) {
                    String idStr = String.valueOf(accounts.getValueAt(modelRow, 0));
                    int id = Integer.parseInt(idStr.replace("CUST-", ""));
                    int confirm = JOptionPane.showConfirmDialog(OwnerFrame.this,
                            "Approve this customer for utang?\nThey presented their valid ID?",
                            "Approve Customer", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            auth.approveCustomer(id);
                            JOptionPane.showMessageDialog(OwnerFrame.this, "Customer approved. They can now sign in.");
                            reload();
                        } catch (AppException ex) {
                            JOptionPane.showMessageDialog(OwnerFrame.this, ex.getMessage(), "Approve", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } else if (col == 7 && "View".equals(action)) {
                    String idStr = String.valueOf(accounts.getValueAt(modelRow, 0));
                    int id = Integer.parseInt(idStr.replace("CUST-", ""));
                    openAccountById(id);
                }
            }
        });
        tableCard.add(Ui.scroll(table), BorderLayout.CENTER);
        wrap.add(tableCard, BorderLayout.CENTER);
        page.add(wrap, BorderLayout.CENTER);
        return page;
    }

    private JPanel logsPage() {
        JPanel page = page("Activity Logs");
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(dateBar(logsDate, "Activity Logs"), BorderLayout.NORTH);
        JTable table = new JTable(logs);
        Ui.styleTable(table);
        Ui.enableLongCellPopup(table);
        body.add(Ui.scroll(table), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel paidPage() {
        JPanel page = page("Paid Utang");
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(dateBar(paidDate, "Paid Utang"), BorderLayout.NORTH);
        JTable table = new JTable(paidLogs);
        Ui.styleTable(table);
        Ui.enableLongCellPopup(table);
        body.add(Ui.scroll(table), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel reportsPage() {
        JPanel page = page("Reports");
        JPanel top = new JPanel(new GridLayout(1, 3, 12, 0));
        top.setOpaque(false);
        top.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 12, 0));
        top.add(Ui.statCard("Collections this week", weekCollections, "wallet"));
        top.add(Ui.statCard("Utang Added Today", dailyUtangTotal, "calendar"));
        top.add(Ui.statCard("Payments Today", dailyPaymentTotal, "wallet"));
        JPanel wrap = new JPanel(new BorderLayout(0, 12));
        wrap.setOpaque(false);
        wrap.add(top, BorderLayout.NORTH);
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 8));
        JPanel head = new JPanel(new BorderLayout(8, 0));
        head.setOpaque(false);
        head.add(Ui.heading("Daily Report"), BorderLayout.WEST);
        head.add(dateBar(reportsDate, "Reports"), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);
        JTable dailyTable = new JTable(dailyRows);
        Ui.styleTable(dailyTable);
        card.add(Ui.scroll(dailyTable), BorderLayout.CENTER);
        wrap.add(card, BorderLayout.CENTER);
        page.add(wrap, BorderLayout.CENTER);
        return page;
    }

    private JPanel auditPage() {
        JPanel page = page("Audit Log");
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(dateBar(auditDate, "Audit Log"), BorderLayout.NORTH);
        JTable table = new JTable(auditRows);
        Ui.styleTable(table);
        Ui.enableLongCellPopup(table);
        body.add(Ui.scroll(table), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel dateBar(final JTextField field, final String which) {
        JPanel bar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);
        JLabel lbl = new JLabel("Date:");
        lbl.setFont(Ui.BODY);
        bar.add(lbl);
        field.setPreferredSize(new Dimension(220, 40));
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyDateFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyDateFilters(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyDateFilters(); }
        });
        bar.add(field);
        JButton today = Ui.ghost("Today");
        today.setPreferredSize(new Dimension(70, 32));
        today.addActionListener(e -> field.setText(AppTime.todayPrefix()));
        JButton clear = Ui.ghost("All");
        clear.setPreferredSize(new Dimension(70, 40));
        clear.addActionListener(e -> field.setText(""));
        bar.add(today);
        bar.add(clear);
        return bar;
    }

    private JPanel staffPage() {
        JPanel page = page("Staff Management");
        JPanel split = new JPanel(new BorderLayout(12, 0));
        split.setOpaque(false);

        // Left: staff list
        JPanel left = Ui.card();
        left.setLayout(new BorderLayout(0, 8));
        left.setPreferredSize(new Dimension(280, 0));
        left.add(Ui.heading("Staff Members"), BorderLayout.NORTH);
        staffList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        staffList.setFixedCellHeight(52);
        staffList.setCellRenderer(new StaffRenderer());
        staffList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showStaffActivity();
            }
        });
        left.add(Ui.scroll(staffList), BorderLayout.CENTER);
        JPanel staffButtons = new JPanel(new GridLayout(0, 1, 0, 6));
        staffButtons.setOpaque(false);
        JButton resetStaffPw = Ui.dark("Change Staff Password");
        resetStaffPw.addActionListener(e -> changeStaffPassword());
        staffButtons.add(resetStaffPw);
        JButton newStaff = Ui.primary("Create Staff");
        newStaff.addActionListener(e -> createStaff());
        staffButtons.add(newStaff);
        left.add(staffButtons, BorderLayout.SOUTH);
        split.add(left, BorderLayout.WEST);

        // Right: activity details
        JPanel right = Ui.card();
        right.setLayout(new BorderLayout(0, 8));
        right.add(Ui.heading("Staff Activity"), BorderLayout.NORTH);
        staffSummary.setFont(Ui.BODY);
        staffSummary.setForeground(Ui.MUTED);
        staffSummary.setBorder(new javax.swing.border.EmptyBorder(4, 4, 8, 4));
        right.add(staffSummary, BorderLayout.SOUTH);
        JTable table = new JTable(staffActivity);
        Ui.styleTable(table);
        Ui.enableLongCellPopup(table);
        right.add(Ui.scroll(table), BorderLayout.CENTER);
        split.add(right, BorderLayout.CENTER);

        page.add(split, BorderLayout.CENTER);
        return page;
    }

    private void showStaffActivity() {
        User selected = staffList.getSelectedValue();
        staffActivity.setRowCount(0);
        if (selected == null) {
            staffSummary.setText("Select a staff member");
            return;
        }
        int count = 0;
        double total = 0;
        for (LedgerEntry entry : allLedgerCache) {
            if (entry.getStaffId() != null && entry.getStaffId() == selected.getId()) {
                staffActivity.addRow(new Object[] {
                        entry.getCreatedAt(),
                        entry.getCustomerFirstName() + " " + entry.getCustomerLastName(),
                        entry.getItemDescription(),
                        String.format("%.2f", entry.getQuantity()),
                        AppTime.peso(entry.getAmount()),
                        entry.isUtang() ? "Utang" : "Payment"
                });
                if (entry.isUtang()) {
                    count++;
                    total += entry.getAmount();
                }
            }
        }
        staffSummary.setText(selected.getFirstName() + " " + selected.getLastName()
                + "   •   " + count + " utang entered   •   Total: " + AppTime.peso(total));
    }

    private static class StaffRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            User u = (User) value;
            label.setText(u.getFirstName() + " " + u.getLastName() + "   (" + u.getUsername() + ")");
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10));
            if (!selected) {
                label.setBackground(Color.WHITE);
            }
            return label;
        }
    }

    private JPanel pendingPage() {
        JPanel page = page("Pending Customer Approvals");
        JTable table = new JTable(pendingRows);
        Ui.styleTable(table);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 5) {
                    String idStr = (String) pendingRows.getValueAt(row, 0);
                    int id = Integer.parseInt(idStr.replace("CUST-", ""));
                    int confirm = JOptionPane.showConfirmDialog(OwnerFrame.this,
                            "Approve this customer for utang?\nThey presented their valid ID?",
                            "Approve Customer", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            auth.approveCustomer(id);
                            JOptionPane.showMessageDialog(OwnerFrame.this, "Customer approved. They can now sign in.");
                            reload();
                        } catch (AppException ex) {
                            JOptionPane.showMessageDialog(OwnerFrame.this, ex.getMessage(), "Approve", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            }
        });
        page.add(Ui.scroll(table), BorderLayout.CENTER);
        return page;
    }

    private JPanel page(String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(Ui.TEXT);
        heading.setIcon(Ui.storeLogoIcon(32));
        heading.setIconTextGap(10);
        JPanel headRow = new JPanel(new BorderLayout());
        headRow.setOpaque(false);
        headRow.add(heading, BorderLayout.WEST);
        // Always-visible Log out, so it can't be scrolled/cropped out of view.
        JButton logout = Ui.danger("Log out");
        logout.addActionListener(e -> LoginFrame.logout(OwnerFrame.this));
        JPanel logoutWrap = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        logoutWrap.setOpaque(false);
        logoutWrap.add(logout);
        headRow.add(logoutWrap, BorderLayout.EAST);
        panel.add(headRow, BorderLayout.NORTH);
        return panel;
    }

    private JPanel statCard(String caption, JLabel value) {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 8));
        JLabel label = new JLabel(caption);
        label.setFont(Ui.SMALL);
        label.setForeground(Ui.MUTED);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JLabel stat() {
        JLabel label = new JLabel("—");
        label.setFont(new Font("SansSerif", Font.BOLD, 26));
        label.setForeground(new Color(0x2E, 0x7D, 0x32));
        return label;
    }

    private void reload() {
        try {
            KpiSnapshot snap = utang.kpis();
            totalUtang.setText(AppTime.peso(snap.getTotalUnpaid()));
            activeCustomers.setText(String.valueOf(snap.getCustomersWithUtang()));
            today.setText(String.valueOf(snap.getTransactionsToday()));
            dashTotal.setText(AppTime.peso(snap.getTotalUnpaid()));
            dashCustomers.setText(String.valueOf(snap.getCustomersWithUtang()));
            dashToday.setText(String.valueOf(snap.getTransactionsToday()));
            accountRows = utang.searchCustomers(accountSearch.getText());
            filterOwnerCustomers();
            accounts.setRowCount(0);

            // Active customers
            for (CustomerBalance row : accountRows) {
                accounts.addRow(new Object[] {
                        "CUST-" + row.getCustomer().getId(),
                        row.getCustomer().getFirstName(),
                        row.getCustomer().getLastName(),
                        row.getCustomer().getUsername(),
                        row.getCustomer().getContact(),
                        AppTime.peso(row.getBalance()),
                        "Active",
                        "View"
                });
            }

            // Pending customers (for approval)
            List<User> pendingList = auth.pendingCustomers();
            for (User p : pendingList) {
                String needle = accountSearch.getText().trim().toLowerCase();
                String hay = (p.getFirstName() + " " + p.getLastName() + " " + p.getUsername() + " " + p.getContact()).toLowerCase();
                if (needle.isEmpty() || hay.contains(needle)) {
                    accounts.addRow(new Object[] {
                            "CUST-" + p.getId(),
                            p.getFirstName(),
                            p.getLastName(),
                            p.getUsername(),
                            p.getContact(),
                            "—",
                            "Pending",
                            "Approve"
                    });
                }
            }

            weekCollections.setText(AppTime.peso(utang.weekCollections()));
            reportRows.setRowCount(0);
            for (CustomerBalance row : utang.highestBalances()) {
                reportRows.addRow(new Object[] {
                        "CUST-" + row.getCustomer().getId(),
                        row.getCustomer().getFirstName(),
                        row.getCustomer().getLastName(),
                        AppTime.peso(row.getBalance())
                });
            }
            dailyRows.setRowCount(0);
            allLedgerCache = utang.allHistory("", false);
            applyDateFilters();
            auditRows.setRowCount(0);
            allAuditCache = utang.auditLog();
            applyDateFilters();
            pendingRows.setRowCount(0);
            for (User pending : auth.pendingCustomers()) {
                pendingRows.addRow(new Object[] {
                        "CUST-" + pending.getId(),
                        pending.getFirstName(),
                        pending.getLastName(),
                        pending.getContact(),
                        pending.getUsername(),
                        "Approve"
                });
            }
            staffModel.clear();
            try {
                for (User s : utang.staff()) {
                    staffModel.addElement(s);
                }
            } catch (AppException ignored) {
                // Staff list may fail independently
            }
            dashLogs.setRowCount(0);
            paidLogs.setRowCount(0);
            logs.setRowCount(0);
            applyDateFilters();
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Owner", JOptionPane.WARNING_MESSAGE);
        }
    }

    private JPanel payCard() {
        JPanel card = Ui.card();
        card.setLayout(new GridLayout(2, 1, 8, 8));
        card.add(Ui.heading("Pay Utang"));
        JPanel row = new JPanel(new GridLayout(1, 3, 8, 0));
        row.setOpaque(false);
        row.add(new JLabel("Uses the customer selected on Owner Counter"));
        row.add(payAmount);
        JButton pay = Ui.primary("Record Payment");
        pay.addActionListener(e -> recordPayment());
        row.add(pay);
        card.add(row);
        return card;
    }

    private void resetUserPin() {
        JTextField contact = Ui.field("09XXXXXXXXX");
        JPasswordField newPin = Ui.password("4-digit PIN");
        JPasswordField confirmPin = Ui.password("Re-enter the 4-digit PIN");
        limitToDigits(newPin, 4);
        limitToDigits(confirmPin, 4);
        for (JTextField f : new JTextField[] {contact, newPin, confirmPin}) {
            f.setPreferredSize(new Dimension(320, 40));
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new javax.swing.border.EmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 0, 8, 0);
        form.add(new JLabel("<html>Set the new recovery PIN the customer chose.</html>"), g);
        String[] labels = {"Customer contact number", "New PIN", "Confirm new PIN"};
        JTextField[] fields = {contact, newPin, confirmPin};
        for (int i = 0; i < labels.length; i++) {
            g.gridy++;
            g.insets = new Insets(4, 0, 2, 0);
            form.add(Ui.caption(labels[i]), g);
            g.gridy++;
            g.insets = new Insets(0, 0, 4, 0);
            form.add(fields[i], g);
        }

        int ok = JOptionPane.showConfirmDialog(this, form, "Reset User PIN",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            User user = auth.resetPinByContact(contact.getText().trim(),
                    new String(newPin.getPassword()),
                    new String(confirmPin.getPassword()));
            JOptionPane.showMessageDialog(this,
                    "PIN reset successfully!\n\n"
                    + "Customer: " + user.getFirstName() + " " + user.getLastName() + "\n"
                    + "Contact: " + user.getContact() + "\n\n"
                    + "They can now log in using their contact number and the new PIN.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Reset PIN", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Keeps a field to digits only, at most {@code max} long. */
    private static void limitToDigits(JPasswordField field, final int max) {
        ((javax.swing.text.AbstractDocument) field.getDocument()).setDocumentFilter(new javax.swing.text.DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, javax.swing.text.AttributeSet attr)
                    throws javax.swing.text.BadLocationException {
                replace(fb, offset, 0, text, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, javax.swing.text.AttributeSet attr)
                    throws javax.swing.text.BadLocationException {
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

    private void changeStaffPassword() {
        User staff = staffList.getSelectedValue();
        if (staff == null) {
            JOptionPane.showMessageDialog(this, "Select a staff member first.");
            return;
        }
        JPasswordField next = Ui.password("New password");
        JPasswordField confirm = Ui.password("Confirm new password");
        next.setPreferredSize(new Dimension(320, 40));
        confirm.setPreferredSize(new Dimension(320, 40));
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new javax.swing.border.EmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 0, 8, 0);
        form.add(new JLabel("<html>Set a new password for <b>" + staff.getFirstName() + " " + staff.getLastName()
                + "</b> (" + staff.getUsername() + ")</html>"), g);
        g.gridy++;
        g.insets = new Insets(4, 0, 2, 0);
        form.add(Ui.caption("New password"), g);
        g.gridy++;
        g.insets = new Insets(0, 0, 8, 0);
        form.add(next, g);
        g.gridy++;
        g.insets = new Insets(4, 0, 2, 0);
        form.add(Ui.caption("Confirm password"), g);
        g.gridy++;
        g.insets = new Insets(0, 0, 4, 0);
        form.add(confirm, g);
        g.gridy++;
        javax.swing.JCheckBox show = new javax.swing.JCheckBox("Show passwords");
        show.setOpaque(false);
        show.setFont(Ui.SMALL);
        show.addActionListener(e -> {
            char c = show.isSelected() ? (char) 0 : '\u2022';
            next.setEchoChar(c);
            confirm.setEchoChar(c);
        });
        form.add(show, g);
        g.gridy++;
        JLabel rule = new JLabel("8\u201312 characters with a capital letter, a number and a special character.");
        rule.setFont(Ui.SMALL);
        rule.setForeground(Ui.MUTED);
        form.add(rule, g);

        int ok = JOptionPane.showConfirmDialog(this, form, "Change Staff Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            auth.resetStaffPassword(staff.getId(), new String(next.getPassword()), new String(confirm.getPassword()));
            JOptionPane.showMessageDialog(this, "Password changed. Tell " + staff.getFirstName() + " to sign in with the new password.");
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Change Staff Password", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void createStaff() {
        JTextField first = Ui.field("First name");
        JTextField last = Ui.field("Last name");
        JTextField contact = Ui.field("09XXXXXXXXX (optional)");
        JTextField user = Ui.field("Username");
        JPasswordField pass = Ui.password("Password (8-12 chars, capital, number, special)");

        first.setPreferredSize(new Dimension(320, 40));
        last.setPreferredSize(new Dimension(320, 40));
        contact.setPreferredSize(new Dimension(320, 40));
        user.setPreferredSize(new Dimension(320, 40));
        pass.setPreferredSize(new Dimension(320, 40));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new javax.swing.border.EmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 0, 2, 0);
        g.anchor = GridBagConstraints.WEST;

        form.add(Ui.caption("First name"), g);
        g.gridy++;
        g.insets = new Insets(0, 0, 8, 0);
        form.add(first, g);

        g.gridy++;
        g.insets = new Insets(4, 0, 2, 0);
        form.add(Ui.caption("Last name"), g);
        g.gridy++;
        g.insets = new Insets(0, 0, 8, 0);
        form.add(last, g);

        g.gridy++;
        g.insets = new Insets(4, 0, 2, 0);
        form.add(Ui.caption("Contact Number (optional)"), g);
        g.gridy++;
        g.insets = new Insets(0, 0, 8, 0);
        form.add(contact, g);

        g.gridy++;
        g.insets = new Insets(4, 0, 2, 0);
        form.add(Ui.caption("Username"), g);
        g.gridy++;
        g.insets = new Insets(0, 0, 8, 0);
        form.add(user, g);

        g.gridy++;
        g.insets = new Insets(4, 0, 2, 0);
        form.add(Ui.caption("Password"), g);
        g.gridy++;
        g.insets = new Insets(0, 0, 4, 0);
        JPanel pwRow = new JPanel(new BorderLayout(6, 0));
        pwRow.setOpaque(false);
        pwRow.add(pass, BorderLayout.CENTER);
        JCheckBox show = new JCheckBox("Show");
        show.setOpaque(false);
        show.setFont(Ui.BODY);
        show.addActionListener(e -> pass.setEchoChar(show.isSelected() ? (char) 0 : '•'));
        pwRow.add(show, BorderLayout.EAST);
        form.add(pwRow, g);

        int ok = JOptionPane.showConfirmDialog(this, form, "Create Staff Account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            User created = auth.createStaff(first.getText(), last.getText(), user.getText(),
                    new String(pass.getPassword()), contact.getText());
            JOptionPane.showMessageDialog(this,
                    "Staff account created.\nUsername: " + created.getUsername(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Create Staff", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteSelectedCustomer(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a customer first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        String idStr = String.valueOf(accounts.getValueAt(modelRow, 0));
        int id = Integer.parseInt(idStr.replace("CUST-", ""));
        String name = accounts.getValueAt(modelRow, 1) + " " + accounts.getValueAt(modelRow, 2);

        // Step 1: yes / no
        int first = JOptionPane.showConfirmDialog(this,
                "Delete customer " + name + " (" + idStr + ")?\n\n"
                + "Their account and ALL of their transaction history will be permanently removed.\n"
                + "This cannot be undone.",
                "Delete Customer", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (first != JOptionPane.YES_OPTION) {
            return;
        }
        // Step 2: type DELETE to confirm
        String typed = JOptionPane.showInputDialog(this,
                "To confirm, type DELETE below:", "Confirm Delete", JOptionPane.WARNING_MESSAGE);
        if (typed == null || !"DELETE".equals(typed.trim())) {
            if (typed != null) {
                JOptionPane.showMessageDialog(this, "Not deleted. You must type DELETE exactly.");
            }
            return;
        }
        try {
            auth.deleteCustomer(id);
            JOptionPane.showMessageDialog(this, name + " (" + idStr + ") was deleted.");
            reload();
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Delete Customer", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateSelectedCustomer(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a customer first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        String status = String.valueOf(accounts.getValueAt(modelRow, 6));
        if ("Pending".equals(status)) {
            JOptionPane.showMessageDialog(this, "Approve the customer first before updating details.");
            return;
        }
        String idStr = String.valueOf(accounts.getValueAt(modelRow, 0));
        int id = Integer.parseInt(idStr.replace("CUST-", ""));
        String currentFirst = String.valueOf(accounts.getValueAt(modelRow, 1));
        String currentLast = String.valueOf(accounts.getValueAt(modelRow, 2));
        String currentUsername = String.valueOf(accounts.getValueAt(modelRow, 3));
        String currentContact = String.valueOf(accounts.getValueAt(modelRow, 4));

        JTextField first = Ui.field("");
        JTextField last = Ui.field("");
        JTextField username = Ui.field("");
        JTextField contact = Ui.field("");
        first.setText(currentFirst);
        last.setText(currentLast);
        username.setText(currentUsername);
        contact.setText(currentContact);
        first.setPreferredSize(new Dimension(320, 40));
        last.setPreferredSize(new Dimension(320, 40));
        username.setPreferredSize(new Dimension(320, 40));
        contact.setPreferredSize(new Dimension(320, 40));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new javax.swing.border.EmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 0, 2, 0);
        g.anchor = GridBagConstraints.WEST;

        form.add(Ui.caption("First name"), g);
        g.gridy++;
        g.insets = new Insets(0, 0, 8, 0);
        form.add(first, g);

        g.gridy++;
        g.insets = new Insets(4, 0, 2, 0);
        form.add(Ui.caption("Last name"), g);
        g.gridy++;
        g.insets = new Insets(0, 0, 8, 0);
        form.add(last, g);

        g.gridy++;
        g.insets = new Insets(4, 0, 2, 0);
        form.add(Ui.caption("Username"), g);
        g.gridy++;
        g.insets = new Insets(0, 0, 8, 0);
        form.add(username, g);

        g.gridy++;
        g.insets = new Insets(4, 0, 2, 0);
        form.add(Ui.caption("Contact Number"), g);
        g.gridy++;
        g.insets = new Insets(0, 0, 4, 0);
        form.add(contact, g);

        int ok = JOptionPane.showConfirmDialog(this, form, "Update Customer " + idStr, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            auth.updateCustomerInfo(id, first.getText(), last.getText(), username.getText(), contact.getText());
            JOptionPane.showMessageDialog(this, "Customer information updated.");
            reload();
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Update", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void ownerRecalc() {
        try {
            double q = Double.parseDouble(ownerQty.getText().trim());
            double u = Double.parseDouble(ownerUnit.getText().trim());
            if (q > 0 && u > 0) {
                ownerAmount.setText(String.format("%.2f", q * u));
            } else {
                ownerAmount.setText("0.00");
            }
        } catch (NumberFormatException ex) {
            ownerAmount.setText("0.00");
        }
    }

    private void ownerAddUtang() {
        try {
            CustomerBalance selected = ownerList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Select a customer first.");
                return;
            }
            double q = Double.parseDouble(ownerQty.getText().trim());
            double u = Double.parseDouble(ownerUnit.getText().trim());
            LedgerEntry entry = utang.addUtang(selected.getCustomer().getId(), ownerItem.getText(),
                    q, u, "");
            ownerItem.setText("");
            ownerQty.setText("1");
            ownerUnit.setText("");
            ownerAmount.setText("0.00");
            reload();
            User cust = selected.getCustomer();
            String receipt = "UTANG TRACKER SYSTEM - RECEIPT\n"
                    + "----------------------------\n"
                    + "Date: " + entry.getCreatedAt() + "\n"
                    + "Customer: " + cust.getFirstName() + " " + cust.getLastName() + " (CUST-" + cust.getId() + ")\n"
                    + "Item: " + entry.getItemDescription() + "\n"
                    + "Quantity: " + String.format("%.2f", entry.getQuantity()) + "\n"
                    + "Unit Price: " + AppTime.peso(entry.getUnitPrice()) + "\n"
                    + "Total Amount: " + AppTime.peso(entry.getAmount()) + "\n"
                    + "Status: Unpaid (Utang)\n"
                    + "----------------------------\n"
                    + "Thank you!";
            JOptionPane.showMessageDialog(this, receipt, "Receipt", JOptionPane.INFORMATION_MESSAGE);
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Add to Utang", JOptionPane.WARNING_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid quantity and unit price.", "Add to Utang", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void recordPayment() {
        CustomerBalance selected = ownerList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a customer first.");
            return;
        }
        String typed = payAmount.getText().trim();
        if (typed.isEmpty()) {
            typed = JOptionPane.showInputDialog(this, "Payment amount for "
                    + selected.getCustomer().getFirstName() + " "
                    + selected.getCustomer().getLastName());
        }
        if (typed == null || typed.trim().isEmpty()) {
            return;
        }
        try {
            utang.acceptPayment(selected.getCustomer().getId(),
                    Double.parseDouble(typed.trim()), "Owner payment");
            payAmount.setText("");
            reload();
            JOptionPane.showMessageDialog(this, "Payment recorded.");
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Pay Utang", JOptionPane.WARNING_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount.", "Pay Utang", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openAccountById(int customerId) {
        CustomerBalance found = null;
        if (accountRows != null) {
            for (CustomerBalance row : accountRows) {
                if (row.getCustomer().getId() == customerId) {
                    found = row;
                    break;
                }
            }
        }
        if (found == null) {
            JOptionPane.showMessageDialog(this, "Customer not found.");
            return;
        }
        DefaultTableModel history = new DefaultTableModel(new Object[] {"Created Date&Time", "Item Description", "Amount", "Note", "Status"}, 0);
        try {
            List<LedgerEntry> rows = utang.historyFor(found.getCustomer().getId());
            for (LedgerEntry entry : rows) {
                history.addRow(new Object[] {
                        entry.getCreatedAt(),
                        entry.getItemDescription(),
                        AppTime.peso(entry.getAmount()),
                        entry.getNote() == null || entry.getNote().isEmpty() ? "—" : entry.getNote(),
                        entry.isUtang() ? "Unpaid" : "Paid"
                });
            }
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "View", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JTable table = new JTable(history);
        Ui.styleTable(table);
        Ui.enableLongCellPopup(table);
        table.setPreferredScrollableViewportSize(new Dimension(520, 160));
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.add(new JLabel("CUST-" + found.getCustomer().getId() + "  "
                + found.getCustomer().getFirstName() + " " + found.getCustomer().getLastName()
                + "    " + AppTime.peso(found.getBalance())), BorderLayout.NORTH);
        form.add(new JScrollPane(table), BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, form, "CUST-" + found.getCustomer().getId(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void openAccount(int modelRow) {
        if (accountRows == null || modelRow < 0 || modelRow >= accountRows.size()) {
            return;
        }
        CustomerBalance row = accountRows.get(modelRow);
        DefaultTableModel history = new DefaultTableModel(new Object[] {"Created Date&Time", "Item Description", "Amount", "Note", "Status"}, 0);
        try {
            List<LedgerEntry> rows = utang.historyFor(row.getCustomer().getId());
            for (LedgerEntry entry : rows) {
                history.addRow(new Object[] {
                        entry.getCreatedAt(),
                        entry.getItemDescription(),
                        AppTime.peso(entry.getAmount()),
                        entry.getNote() == null || entry.getNote().isEmpty() ? "—" : entry.getNote(),
                        entry.isUtang() ? "Unpaid" : "Paid"
                });
            }
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "View", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JTable table = new JTable(history);
        Ui.styleTable(table);
        Ui.enableLongCellPopup(table);
        table.setPreferredScrollableViewportSize(new Dimension(520, 160));
        JTextField payment = Ui.field("0.00");
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.add(new JLabel("CUST-" + row.getCustomer().getId() + "  "
                + row.getCustomer().getFirstName() + " " + row.getCustomer().getLastName()
                + "    " + AppTime.peso(row.getBalance())), BorderLayout.NORTH);
        form.add(new JScrollPane(table), BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, form, "CUST-" + row.getCustomer().getId(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void applyDateFilters() {
        String logsNeedle = logsDate.getText().trim();
        String paidNeedle = paidDate.getText().trim();
        String reportsNeedle = reportsDate.getText().trim();
        String auditNeedle = auditDate.getText().trim();

        logs.setRowCount(0);
        paidLogs.setRowCount(0);
        dailyRows.setRowCount(0);
        auditRows.setRowCount(0);
        dashLogs.setRowCount(0);

        double todayUtang = 0;
        double todayPay = 0;
        String today = AppTime.todayPrefix();

        for (LedgerEntry row : allLedgerCache) {
            String when = row.getCreatedAt();
            boolean matchLogs = logsNeedle.isEmpty() || when.startsWith(logsNeedle);
            boolean matchPaid = paidNeedle.isEmpty() || when.startsWith(paidNeedle);
            boolean matchReports = reportsNeedle.isEmpty() || when.startsWith(reportsNeedle);

            if (matchLogs) {
                logs.addRow(new Object[] {
                        when,
                        "CUST-" + row.getCustomerId(),
                        row.getCustomerFirstName(),
                        row.getCustomerLastName(),
                        (row.getStaffFirstName() == null ? "System" : row.getStaffFirstName() + " " + row.getStaffLastName()),
                        row.getItemDescription(),
                        AppTime.peso(row.getAmount()),
                        row.isUtang() ? "Added utang" : "Payment",
                        row.getNote() == null || row.getNote().isEmpty() ? "—" : row.getNote()
                });
            }
            if (!row.isUtang() && matchPaid) {
                paidLogs.addRow(new Object[] {
                        when,
                        "CUST-" + row.getCustomerId(),
                        row.getCustomerFirstName(),
                        row.getCustomerLastName(),
                        row.getStaffFirstName() == null ? "System" : row.getStaffFirstName() + " " + row.getStaffLastName(),
                        AppTime.peso(row.getAmount()),
                        row.getNote() == null || row.getNote().isEmpty() ? "—" : row.getNote()
                });
            }
            if (matchReports) {
                dailyRows.addRow(new Object[] {
                        when,
                        row.getCustomerFirstName() + " " + row.getCustomerLastName(),
                        row.getItemDescription(),
                        String.format("%.2f", row.getQuantity()),
                        AppTime.peso(row.getUnitPrice()),
                        AppTime.peso(row.getAmount()),
                        (row.getStaffFirstName() == null ? "System" : row.getStaffFirstName() + " " + row.getStaffLastName()),
                        row.getType()
                });
            }
            // Dashboard recent still shows today's activity
            if (when.startsWith(today)) {
                dashLogs.addRow(new Object[] {
                        when,
                        row.getCustomerFirstName() + " " + row.getCustomerLastName(),
                        row.getItemDescription(),
                        AppTime.peso(row.getAmount()),
                        (row.getStaffFirstName() == null ? "System" : row.getStaffFirstName() + " " + row.getStaffLastName()),
                        row.isUtang() ? "Added utang" : "Payment"
                });
                if (row.isUtang()) {
                    todayUtang += row.getAmount();
                } else {
                    todayPay += row.getAmount();
                }
            }
        }
        dailyUtangTotal.setText(AppTime.peso(todayUtang));
        dailyPaymentTotal.setText(AppTime.peso(todayPay));

        for (String[] row : allAuditCache) {
            String when = row.length > 0 ? row[0] : "";
            if (auditNeedle.isEmpty() || when.startsWith(auditNeedle)) {
                auditRows.addRow(row);
            }
        }
    }

    private void filterOwnerCustomers() {
        if (accountRows == null) {
            return;
        }
        String needle = ownerCustomerSearch.getText().trim().toLowerCase();
        CustomerBalance previous = ownerList.getSelectedValue();
        ownerCustomers.clear();
        for (CustomerBalance row : accountRows) {
            String blob = (row.getCustomer().getFirstName() + " " + row.getCustomer().getLastName()
                    + " " + row.getCustomer().getId() + " " + row.getCustomer().getContact()).toLowerCase();
            if (needle.isEmpty() || blob.contains(needle)) {
                ownerCustomers.addElement(row);
            }
        }
        if (previous != null) {
            for (int i = 0; i < ownerCustomers.size(); i++) {
                if (ownerCustomers.get(i).getCustomer().getId() == previous.getCustomer().getId()) {
                    ownerList.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private static class OwnerCustomerRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
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

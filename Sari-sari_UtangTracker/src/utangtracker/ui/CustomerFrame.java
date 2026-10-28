package utangtracker.ui;

import utangtracker.Session;
import utangtracker.model.LedgerEntry;
import utangtracker.service.AppException;
import utangtracker.service.UtangService;
import utangtracker.util.AppTime;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

public class CustomerFrame extends JFrame {
    private final UtangService utang = new UtangService();
    private final JLabel balance = new JLabel("PHP 0.00", SwingConstants.CENTER);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"Date Borrowed", "Item Description", "Amount", "Note", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public CustomerFrame() {
        super("Utang Tracker");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Ui.applyResponsiveSize(this, 980, 680, new Dimension(720, 480));

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.add(Ui.appHeader("Utang Tracker System  |  Customer Portal",
                Session.get().getFirstName() + " " + Session.get().getLastName()
                        + "   ·   ID CUST-" + Session.get().getId(), this), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 24, 28));
        body.add(balanceCard(), BorderLayout.NORTH);
        body.add(historyCard(), BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        JScrollPane scroller = new JScrollPane(root);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        scroller.getHorizontalScrollBar().setUnitIncrement(16);
        setContentPane(scroller);
        reload();
    }

    private JPanel header() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x2E, 0x7D, 0x32));
        bar.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 18, 12, 16));
        JLabel brand = new JLabel("Utang Tracker System  |  Customer Portal");
        brand.setFont(new Font("SansSerif", Font.BOLD, 16));
        brand.setForeground(Color.WHITE);
        brand.setIcon(Ui.storeLogoIcon(28));
        brand.setIconTextGap(8);
        bar.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        JLabel who = new JLabel(Session.get().getFirstName() + " " + Session.get().getLastName()
                + "   ·   ID CUST-" + Session.get().getId());
        who.setForeground(Color.WHITE);
        who.setFont(Ui.BODY);
        JButton out = Ui.dark("Logout");
        out.setPreferredSize(new Dimension(100, 32));
        out.addActionListener(e -> LoginFrame.logout(this));
        right.add(who);
        right.add(out);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel balanceCard() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 8));
        JLabel caption = new JLabel("TOTAL UNPAID BALANCE", SwingConstants.CENTER);
        caption.setFont(new Font("SansSerif", Font.BOLD, 13));
        caption.setForeground(Ui.MUTED);
        caption.setIcon(Ui.circleIcon("wallet", 28, Ui.GREEN, Ui.GREEN_SOFT));
        caption.setFont(new Font("SansSerif", Font.BOLD, 13));
        caption.setForeground(Ui.MUTED);
        balance.setFont(new Font("SansSerif", Font.BOLD, 42));
        balance.setForeground(new Color(0x2E, 0x7D, 0x32));
        card.add(caption, BorderLayout.NORTH);
        card.add(balance, BorderLayout.CENTER);
        return card;
    }

    private JPanel historyCard() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(Ui.heading("Transaction History"), BorderLayout.NORTH);
        JTable table = new JTable(model);
        Ui.styleTable(table);
        Ui.enableLongCellPopup(table);
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        return card;
    }

    private void reload() {
        try {
            balance.setText(AppTime.peso(utang.myBalance()));
            model.setRowCount(0);
            List<LedgerEntry> rows = utang.myHistory();
            for (LedgerEntry row : rows) {
                model.addRow(new Object[] {
                        row.getCreatedAt(),
                        row.getItemDescription(),
                        AppTime.peso(row.getAmount()),
                        row.getNote() == null || row.getNote().isEmpty() ? "—" : row.getNote(),
                        row.isUtang() ? "Unpaid" : "Paid"
                });
            }
        } catch (AppException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Customer Portal", JOptionPane.WARNING_MESSAGE);
        }
    }
}

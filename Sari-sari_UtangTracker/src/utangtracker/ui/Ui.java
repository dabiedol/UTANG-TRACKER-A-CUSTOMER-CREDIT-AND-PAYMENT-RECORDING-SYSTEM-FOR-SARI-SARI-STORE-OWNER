package utangtracker.ui;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Rectangle;
import javax.swing.JFrame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class Ui {
    public static final Color GREEN = new Color(0x1F, 0x8A, 0x4C);
    public static final Color GREEN_DARK = new Color(0x14, 0x5C, 0x32);
    public static final Color GREEN_DEEP = new Color(0x0E, 0x3D, 0x24);
    public static final Color GREEN_SOFT = new Color(0xE7, 0xF6, 0xEC);
    public static final Color BG = new Color(0xF2, 0xF5, 0xF3);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(0x1C, 0x28, 0x2D);
    public static final Color MUTED = new Color(0x6B, 0x7C, 0x85);
    public static final Color LINE = new Color(0xE3, 0xE8, 0xE5);
    public static final Color DANGER = new Color(0xC6, 0x3D, 0x3D);
    public static final Color ZEBRA = new Color(0xF7, 0xFB, 0xF8);

    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 30);
    public static final Font H1 = new Font("SansSerif", Font.BOLD, 24);
    public static final Font H2 = new Font("SansSerif", Font.BOLD, 16);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font SMALL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font MONEY = new Font("SansSerif", Font.BOLD, 40);

    private Ui() {
    }

    public static ImageIcon logoIcon(int width) {
        try {
            URL url = Ui.class.getResource("/utangtracker/ui/resources/logo.jpg");
            if (url == null) {
                return null;
            }
            ImageIcon original = new ImageIcon(url);
            Image scaled = original.getImage().getScaledInstance(width, -1, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ex) {
            return null;
        }
    }

    public static ImageIcon iconIcon(int size) {
        try {
            URL url = Ui.class.getResource("/utangtracker/ui/resources/icon.jpg");
            if (url == null) {
                return null;
            }
            ImageIcon original = new ImageIcon(url);
            Image scaled = original.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ex) {
            return null;
        }
    }

    public static javax.swing.Icon storeLogoIcon(int size) {
        return new javax.swing.Icon() {
            @Override
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = size;
                // Store roof
                g2.setColor(GREEN_DARK);
                int[] roofX = {x + s/2, x + 4, x + s - 4};
                int[] roofY = {y + 4, y + s/3, y + s/3};
                g2.fillPolygon(roofX, roofY, 3);
                // Store body
                g2.setColor(GREEN);
                g2.fillRoundRect(x + 6, y + s/3, s - 12, s/2, 6, 6);
                // Awning stripes
                g2.setColor(new Color(0x4C, 0xAF, 0x50));
                for (int i = 0; i < 4; i++) {
                    g2.fillRect(x + 8 + i * ((s-16)/4), y + s/3, (s-16)/8, s/8);
                }
                // Wallet
                g2.setColor(GREEN_DEEP);
                g2.fillRoundRect(x + s/2 - 2, y + s/2, s/2 - 4, s/3, 4, 4);
                // Coin
                g2.setColor(new Color(0x81, 0xC7, 0x84));
                g2.fillOval(x + s - s/4, y + s/2 + 2, s/5, s/5);
                g2.setColor(GREEN_DARK);
                g2.setFont(new Font("SansSerif", Font.BOLD, s/8));
                g2.drawString("₱", x + s - s/4 + 2, y + s/2 + s/6);
                g2.dispose();
            }
            @Override
            public int getIconWidth() { return size; }
            @Override
            public int getIconHeight() { return size; }
        };
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(H2);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY);
        label.setForeground(MUTED);
        return label;
    }

    public static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(MUTED);
        return label;
    }

    public static JTextField field(String placeholder) {
        JTextField input = new PlaceholderField(placeholder);
        styleField(input);
        return input;
    }

    public static JPasswordField password(String placeholder) {
        JPasswordField input = new PlaceholderPassword(placeholder);
        styleField(input);
        return input;
    }

    public static JTextArea area() {
        JTextArea box = new JTextArea(3, 20);
        box.setFont(BODY);
        box.setLineWrap(true);
        box.setWrapStyleWord(true);
        box.setBorder(new EmptyBorder(8, 10, 8, 10));
        return box;
    }

    public static JButton primary(String text) {
        return button(text, GREEN, Color.WHITE);
    }

    public static JButton dark(String text) {
        return button(text, GREEN_DARK, Color.WHITE);
    }

    public static JButton danger(String text) {
        return button(text, DANGER, Color.WHITE);
    }

    /** Flat solid-color button with any background (used by the MoneyTracker-style screens). */
    public static JButton colored(String text, Color background) {
        return button(text, background, Color.WHITE);
    }

    public static JButton ghost(String text) {
        return button(text, GREEN_SOFT, GREEN_DARK);
    }

    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(18, 20, 18, 20)));
        return panel;
    }

    public static void enableLongCellPopup(final JTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() < 1) {
                    return;
                }
                int viewRow = table.rowAtPoint(event.getPoint());
                int viewColumn = table.columnAtPoint(event.getPoint());
                if (viewRow < 0 || viewColumn < 0) {
                    return;
                }
                String header = table.getColumnName(viewColumn).toLowerCase();
                boolean readable = header.contains("date") || header.contains("time") || header.contains("note") || header.contains("borrow");
                if (!readable) {
                    return;
                }
                int modelRow = table.convertRowIndexToModel(viewRow);
                int modelColumn = table.convertColumnIndexToModel(viewColumn);
                Object value = table.getModel().getValueAt(modelRow, modelColumn);
                String text = value == null ? "" : String.valueOf(value);
                if ("—".equals(text)) {
                    text = "";
                }
                JTextArea area = new JTextArea(text.isEmpty() ? "No text in this cell." : text);
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                area.setEditable(false);
                area.setFont(BODY);
                area.setBackground(Color.WHITE);
                area.setBorder(new EmptyBorder(8, 8, 8, 8));
                JScrollPane pane = new JScrollPane(area);
                pane.setPreferredSize(new Dimension(420, 160));
                JOptionPane.showMessageDialog(table, pane, table.getColumnName(viewColumn), JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public static JScrollPane scroll(JComponent view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createLineBorder(LINE));
        pane.getViewport().setBackground(Color.WHITE);
        return pane;
    }

    /**
     * Sizes a frame proportionally to the current screen so it fits on small
     * and large displays. Keeps a sensible minimum and centers the window.
     */
    public static void applyResponsiveSize(JFrame frame, int preferredWidth, int preferredHeight, Dimension minimum) {
        if (minimum != null) {
            frame.setMinimumSize(minimum);
        }
        frame.setResizable(true);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        // Use ~82% of available screen, but never exceed the preferred design size on huge monitors
        // and never go below the minimum.
        int w = (int) Math.min(preferredWidth, screen.width * 0.88);
        int h = (int) Math.min(preferredHeight, screen.height * 0.86);
        if (minimum != null) {
            w = Math.max(w, minimum.width);
            h = Math.max(h, minimum.height);
        }
        // Leave a small margin so the window never touches the screen edges
        w = Math.min(w, screen.width - 40);
        h = Math.min(h, screen.height - 60);
        frame.setSize(w, h);
        frame.setLocationRelativeTo(null);
    }

    public static void styleTable(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.setFont(BODY);
        table.setRowHeight(36);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0xEE, 0xF2, 0xEF));
        table.setSelectionBackground(new Color(0xD8, 0xF0, 0xE0));
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        JTableHeader header = table.getTableHeader();
        header.setBackground(GREEN_DARK);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new java.awt.Dimension(0, 38));
        table.setDefaultRenderer(Object.class, new StripeRenderer());
    }

    public static JPanel appHeader(String title, String who, final javax.swing.JFrame frame) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x1B, 0x6B, 0x32));
        bar.setBorder(new EmptyBorder(10, 16, 10, 14));
        JLabel brand = new JLabel(title);
        brand.setFont(new Font("SansSerif", Font.BOLD, 18));
        brand.setForeground(Color.WHITE);
        brand.setIcon(storeLogoIcon(28));
        brand.setIconTextGap(10);
        bar.add(brand, BorderLayout.WEST);
        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        if (who != null) {
            JLabel person = new JLabel(who);
            person.setForeground(Color.WHITE);
            person.setFont(BODY);
            person.setIcon(circleIcon("user", 26, Color.WHITE, new Color(0x1B, 0x6B, 0x32)));
            person.setIconTextGap(8);
            right.add(person);
        }
        JButton out = dark("Logout");
        out.setIcon(circleIcon("out", 16, Color.WHITE, GREEN_DARK));
        out.setPreferredSize(new java.awt.Dimension(118, 34));
        out.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                LoginFrame.logout(frame);
            }
        });
        right.add(out);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    public static JPanel statCard(String caption, JLabel value, String icon) {
        JPanel card = card();
        card.setLayout(new java.awt.BorderLayout(0, 8));
        JLabel label = new JLabel(caption);
        label.setFont(SMALL);
        label.setForeground(MUTED);
        label.setIcon(circleIcon(icon, 28, GREEN, GREEN_SOFT));
        label.setIconTextGap(8);
        card.add(label, java.awt.BorderLayout.NORTH);
        card.add(value, java.awt.BorderLayout.CENTER);
        return card;
    }

    public static javax.swing.Icon circleIcon(final String kind, final int size, final Color ink, final Color fill) {
        return new javax.swing.Icon() {
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
            public void paintIcon(java.awt.Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(fill);
                g.fillOval(x, y, size, size);
                g.setColor(ink);
                g.setStroke(new BasicStroke(1.6f));
                int cx = x + size / 2;
                int cy = y + size / 2;
                if ("user".equals(kind)) {
                    g.drawOval(cx - 4, cy - 7, 8, 8);
                    g.drawArc(cx - 7, cy + 1, 14, 10, 0, 180);
                } else if ("wallet".equals(kind)) {
                    g.drawRoundRect(cx - 7, cy - 5, 14, 11, 3, 3);
                    g.drawLine(cx - 7, cy - 1, cx + 7, cy - 1);
                } else if ("people".equals(kind)) {
                    g.drawOval(cx - 6, cy - 6, 6, 6);
                    g.drawOval(cx, cy - 6, 6, 6);
                    g.drawArc(cx - 8, cy, 10, 8, 0, 180);
                    g.drawArc(cx - 1, cy, 10, 8, 0, 180);
                } else if ("calendar".equals(kind)) {
                    g.drawRoundRect(cx - 6, cy - 5, 12, 11, 2, 2);
                    g.drawLine(cx - 6, cy - 1, cx + 6, cy - 1);
                } else if ("out".equals(kind)) {
                    g.drawLine(cx - 5, cy, cx + 4, cy);
                    g.drawLine(cx + 1, cy - 4, cx + 5, cy);
                    g.drawLine(cx + 1, cy + 4, cx + 5, cy);
                } else {
                    g.drawRoundRect(cx - 6, cy - 6, 12, 12, 3, 3);
                }
                g.dispose();
            }
        };
    }

    private static void styleField(final JTextField input) {
        input.setFont(BODY);
        input.setBackground(Color.WHITE);
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(10, 12, 10, 12)));
        input.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                input.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GREEN, 2),
                        new EmptyBorder(9, 11, 9, 11)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                input.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(LINE),
                        new EmptyBorder(10, 12, 10, 12)));
            }
        });
    }

    private static JButton button(final String text, final Color bg, final Color fg) {
        final JButton button = new JButton(text) {
            private boolean hover;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = bg;
                if (!isEnabled()) {
                    fill = new Color(0xC5, 0xD0, 0xC8);
                } else if (getModel().isPressed()) {
                    fill = bg.darker();
                } else if (hover) {
                    fill = blend(bg, Color.WHITE, 0.12f);
                }
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g.setColor(fg);
                g.setFont(getFont());
                int textWidth = g.getFontMetrics().stringWidth(getText());
                int x = (getWidth() - textWidth) / 2;
                int y = (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2;
                g.drawString(getText(), x, y);
                g.dispose();
            }

            @Override
            protected void paintBorder(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0, 0, 0, 18));
                g.setStroke(new BasicStroke(1f));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g.dispose();
            }

            void setHover(boolean value) {
                hover = value;
                repaint();
            }
        };
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ((JButton) e.getSource()).putClientProperty("hover", Boolean.TRUE);
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });
        return button;
    }

    private static Color blend(Color base, Color over, float amount) {
        int r = (int) (base.getRed() * (1 - amount) + over.getRed() * amount);
        int g = (int) (base.getGreen() * (1 - amount) + over.getGreen() * amount);
        int b = (int) (base.getBlue() * (1 - amount) + over.getBlue() * amount);
        return new Color(r, g, b);
    }

    private static class StripeRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            label.setBorder(new EmptyBorder(0, 12, 0, 12));
            label.setFont(BODY);
            String text = value == null ? "" : String.valueOf(value);
            label.setText(text);
            label.setHorizontalAlignment(JLabel.LEFT);
            if (!selected && ("Unpaid".equals(text) || "Paid".equals(text) || "View".equals(text))) {
                label.setOpaque(true);
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setFont(new Font("SansSerif", Font.BOLD, 12));
                if ("Unpaid".equals(text)) {
                    label.setBackground(new Color(0xE8, 0xF5, 0xE9));
                    label.setForeground(new Color(0x1B, 0x6B, 0x32));
                } else if ("Paid".equals(text)) {
                    label.setBackground(new Color(0xE3, 0xF2, 0xFD));
                    label.setForeground(new Color(0x15, 0x65, 0xC0));
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(GREEN);
                }
            } else if (!selected) {
                label.setBackground(row % 2 == 0 ? Color.WHITE : ZEBRA);
                label.setForeground(TEXT);
            }
            return label;
        }
    }

    private static class PlaceholderField extends JTextField {
        private final String placeholder;

        PlaceholderField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(MUTED);
                g.setFont(getFont());
                g.drawString(placeholder, 14, getHeight() / 2 + g.getFontMetrics().getAscent() / 2 - 2);
                g.dispose();
            }
        }
    }

    private static class PlaceholderPassword extends JPasswordField {
        private final String placeholder;

        PlaceholderPassword(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getPassword().length == 0 && !isFocusOwner()) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(MUTED);
                g.setFont(getFont().deriveFont(Font.PLAIN));
                g.drawString(placeholder, 14, getHeight() / 2 + g.getFontMetrics().getAscent() / 2 - 2);
                g.dispose();
            }
        }
    }
}

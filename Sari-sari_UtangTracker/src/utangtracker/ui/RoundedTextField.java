package utangtracker.ui;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Pill-shaped text field (same idea as MoneyTrackerSystem's RoundedTextField),
 * with optional grey placeholder text.
 */
public class RoundedTextField extends JTextField {
    private int cornerRadius = 30;
    private Color borderColor = new Color(180, 180, 180);
    private String placeholder = "";

    public RoundedTextField() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14)); // inner padding
        setFont(Ui.BODY);
        setBackground(Color.WHITE);
    }

    public RoundedTextField(String placeholder) {
        this();
        this.placeholder = placeholder == null ? "" : placeholder;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        g2.dispose();
        super.paintComponent(graphics); // draws the text and caret
        if (getText().isEmpty() && !isFocusOwner() && !placeholder.isEmpty()) {
            Graphics2D p = (Graphics2D) graphics.create();
            p.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            p.setColor(Ui.MUTED);
            p.setFont(getFont());
            p.drawString(placeholder, getInsets().left, getHeight() / 2 + p.getFontMetrics().getAscent() / 2 - 2);
            p.dispose();
        }
    }

    @Override
    protected void paintBorder(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isFocusOwner() ? Ui.GREEN : borderColor);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        g2.dispose();
    }
}

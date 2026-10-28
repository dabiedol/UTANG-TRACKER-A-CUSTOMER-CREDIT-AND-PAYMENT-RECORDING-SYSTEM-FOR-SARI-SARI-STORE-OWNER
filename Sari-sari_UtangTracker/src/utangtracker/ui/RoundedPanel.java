package utangtracker.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * Panel with rounded corners (same idea as MoneyTrackerSystem's RoundedPanel).
 * Set {@link #setSquareBottom(boolean)} to get a header strip: rounded on top,
 * flat on the bottom, so it can sit flush on top of a rounded card.
 */
public class RoundedPanel extends JPanel {
    private int cornerRadius = 30;
    private boolean squareBottom;

    public RoundedPanel() {
        this((LayoutManager) null, 30);
    }

    public RoundedPanel(int cornerRadius) {
        this((LayoutManager) null, cornerRadius);
    }

    public RoundedPanel(LayoutManager layout, int cornerRadius) {
        super(layout);
        this.cornerRadius = cornerRadius;
        setOpaque(false); // needed so the corners look transparent
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        repaint();
    }

    public void setSquareBottom(boolean squareBottom) {
        this.squareBottom = squareBottom;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = getBackground();
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        if (squareBottom) {
            g2.fillRect(0, getHeight() / 2, getWidth(), getHeight() - getHeight() / 2);
        }
        g2.dispose();
        super.paintComponent(graphics);
    }
}

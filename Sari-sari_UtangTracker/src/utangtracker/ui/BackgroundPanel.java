package utangtracker.ui;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.net.URL;

/**
 * Panel that paints the sari-sari store photo as a full background.
 */
public class BackgroundPanel extends JPanel {
    private Image background;

    public BackgroundPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        loadBackground();
    }

    public BackgroundPanel() {
        this(null);
    }

    private void loadBackground() {
        try {
            URL url = getClass().getResource("/utangtracker/ui/resources/sari-sari-bg.png");
            if (url == null) {
                url = getClass().getResource("resources/sari-sari-bg.png");
            }
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                background = icon.getImage();
            }
            // File fallback (when running from project root)
            if (background == null) {
                java.io.File f = new java.io.File("src/utangtracker/ui/resources/sari-sari-bg.png");
                if (f.exists()) {
                    background = new ImageIcon(f.getAbsolutePath()).getImage();
                }
            }
        } catch (Exception ignored) {
            background = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                g2.drawImage(background, 0, 0, w, h, this);
            }
            g2.dispose();
        } else {
            // Visible fallback so we know the panel is there
            g.setColor(new java.awt.Color(0xB3, 0xE5, 0xFC));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}

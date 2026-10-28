package utangtracker;

import utangtracker.db.Database;
import utangtracker.ui.LoginFrame;
import utangtracker.ui.Ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;

public class UtangTracker {
    public static void main(String[] args) {
        Database.get();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                applyTheme();
                LoginFrame frame = new LoginFrame();
                frame.setVisible(true);
            }
        });
    }

    private static void applyTheme() {
        UIManager.put("Panel.background", Ui.BG);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", Ui.TEXT);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.background", Ui.BG);
        UIManager.put("TabbedPane.focus", Ui.GREEN);
    }
}

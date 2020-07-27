package application.ui;

import crypto.Encoding;
import crypto.Entry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

public class UIEntry extends JPanel implements MouseListener {

    public static final int BORDER_THICKNESS = 1;
    public static Color SELECTED_FOREGROUND_COLOR;
    public static Color SELECTED_BACKGROUND_COLOR;
    public static LineBorder SELECTED_BORDER;
    public static Color UNSELECTED_FOREGROUND_COLOR;
    public static Color UNSELECTED_BACKGROUND_COLOR;
    public static LineBorder UNSELECTED_BORDER;
    private static final Color CHANGED_FOREGROUND_COLOR = Color.BLACK;
    private static final Color CHANGED_BACKGROUND_COLOR = new Color(255, 185, 121);
    private static final LineBorder CHANGED_BORDER =
            new LineBorder(CHANGED_BACKGROUND_COLOR.darker(), BORDER_THICKNESS);
    public static Font FONT;
    public static String ECHO_CHAR_STRING;
    public static int DOUBLE_CLICK_INTERVAL;

    private final Entry entry;

    private final JLabel websiteLabel, usernameLabel, passwordLabel;

    private final AppMainFrame parent;

    private boolean selected, changed;

    private long doubleClickExpire;

    public UIEntry(AppMainFrame parent, Entry entry) {
        this.parent = parent;
        this.entry = entry;
        setLayout(new GridLayout(0, 3, 5, 5));
        websiteLabel = new JLabel("");
        websiteLabel.setOpaque(true);
        websiteLabel.setFont(FONT);
        add(websiteLabel);
        usernameLabel = new JLabel("");
        usernameLabel.setOpaque(true);
        usernameLabel.setFont(FONT);
        add(usernameLabel);
        passwordLabel = new JLabel("");
        passwordLabel.setOpaque(true);
        passwordLabel.setFont(FONT);
        add(passwordLabel);

        setColors(UNSELECTED_FOREGROUND_COLOR, UNSELECTED_BACKGROUND_COLOR, UNSELECTED_BORDER);

        usernameLabel.addMouseListener(this);
        websiteLabel.addMouseListener(this);
        passwordLabel.addMouseListener(this);
        addMouseListener(this);
        update();
        setMaximumSize(new Dimension(10000, (int)getPreferredSize().getHeight()));
        setBorder(new EmptyBorder(3, 3, 3, 3));
    }

    public Entry getEntry() {
        return entry;
    }

    public void update() {
        String website = entry.getWebsite();
        if(website.length() > 18)
            websiteLabel.setText(website.substring(0, 15) + "...");
        else
            websiteLabel.setText(website);
        String username = entry.getUsername();
        if(username.length() > 18)
            usernameLabel.setText(username.substring(0, 15) + "...");
        else
            usernameLabel.setText(username);
        if(entry.getPasswordData() == null)
            passwordLabel.setText(" ");
        else
            passwordLabel.setText(ECHO_CHAR_STRING);
        validate();
    }

    private void setColors(Color fgColor, Color bgColor, LineBorder border) {
        websiteLabel.setForeground(fgColor);
        websiteLabel.setBackground(bgColor);
        websiteLabel.setBorder(border);
        usernameLabel.setForeground(fgColor);
        usernameLabel.setBackground(bgColor);
        usernameLabel.setBorder(border);
        passwordLabel.setForeground(fgColor);
        passwordLabel.setBackground(bgColor);
        passwordLabel.setBorder(border);
    }

    public void selected() {
        selected = true;
        setColors(SELECTED_FOREGROUND_COLOR, SELECTED_BACKGROUND_COLOR, SELECTED_BORDER);
        validate();
    }

    public void deselected(boolean changesMade) {
        selected = false;
        changed = changesMade || changed;
        if(changed)
            setColors(CHANGED_FOREGROUND_COLOR, CHANGED_BACKGROUND_COLOR, CHANGED_BORDER);
        else
            setColors(UNSELECTED_FOREGROUND_COLOR, UNSELECTED_BACKGROUND_COLOR, UNSELECTED_BORDER);
        validate();
    }

    public void changesMade() {
        changed = true;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        long time = e.getWhen();
        if(time < doubleClickExpire && !selected)
            parent.entryClicked(this);
        doubleClickExpire = time + DOUBLE_CLICK_INTERVAL;
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}

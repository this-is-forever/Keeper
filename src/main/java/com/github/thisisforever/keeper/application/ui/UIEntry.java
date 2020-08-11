package com.github.thisisforever.keeper.application.ui;

import com.github.thisisforever.keeper.cryptox.Entry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * A clickable UI component with a website, username and password. The application's main frame's entry list is filled
 * with {@link UIEntry} objects.
 */
public class UIEntry extends JPanel implements MouseListener {

    // Defines the border thickness for UIEntries
    private static final int BORDER_THICKNESS = 1;
    // Defines the colors, borders and fonts used by UIEntry child components
    // setupLookAndFeel defines these attributes based on the set look & feel
    private static Color SELECTED_FOREGROUND_COLOR;
    private static Color SELECTED_BACKGROUND_COLOR;
    private static LineBorder SELECTED_BORDER;
    private static Color UNSELECTED_FOREGROUND_COLOR;
    private static Color UNSELECTED_BACKGROUND_COLOR;
    private static LineBorder UNSELECTED_BORDER;
    private static final Color CHANGED_FOREGROUND_COLOR = Color.BLACK;
    private static final Color CHANGED_BACKGROUND_COLOR = new Color(255, 185, 121);
    private static final LineBorder CHANGED_BORDER =
            new LineBorder(CHANGED_BACKGROUND_COLOR.darker(), BORDER_THICKNESS);
    private static Font FONT;
    // String that holds dummy text for the password label
    private static String ECHO_CHAR_STRING;
    // Determines the amount of time between clicks that will register a double click
    private static int DOUBLE_CLICK_INTERVAL;

    // References this UI component's underlying entry data
    private final Entry entry;

    // References the text components for this UI entry that display the website, username and dummy password
    private final JLabel websiteLabel, usernameLabel, passwordLabel;

    // References the parent frame of this UI component; used to notify the parent when the component is double-clicked
    private final AppMainFrame parent;

    // Flags set when the UI entry is selected and when the entry has been changed since the application opened
    private boolean selected, changed;

    // Determines a point in time at which point a second click on this component will no longer be considered a
    // double-click
    private long doubleClickExpire;

    /**
     * Changes fonts, colors, borders, etc. for all {@link UIEntry} components based on the set look and feel
     */
    public static void setupLookAndFeel() {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();

        // Get the L&F font for text fields and change it to 16 point font
        UIEntry.FONT = defaults.getFont("TextField.font").deriveFont((float)16);

        // Get the L&F colors for List items
        UIEntry.SELECTED_FOREGROUND_COLOR = defaults.getColor("List.selectionForeground");
        UIEntry.SELECTED_BACKGROUND_COLOR = defaults.getColor("List.selectionBackground");
        UIEntry.SELECTED_BORDER = new LineBorder(UIEntry.SELECTED_BACKGROUND_COLOR, UIEntry.BORDER_THICKNESS);
        UIEntry.UNSELECTED_FOREGROUND_COLOR = defaults.getColor("List.foreground");
        UIEntry.UNSELECTED_BACKGROUND_COLOR = defaults.getColor("List.background");
        UIEntry.UNSELECTED_BORDER = new LineBorder(UIEntry.UNSELECTED_BACKGROUND_COLOR, UIEntry.BORDER_THICKNESS);

        // Get the L&F character for password fields, AKA an "echo character"
        char echoChar = (char)defaults.get("PasswordField.echoChar");

        // Create the dummy String with 12 echo characters
        UIEntry.ECHO_CHAR_STRING = Character.toString(echoChar).repeat(12);

        // Determine the double-click interval for the user's system, defaulting to .5 seconds if one isn't set
        Object clickIntervalObj = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
        if(clickIntervalObj instanceof Integer) {
            UIEntry.DOUBLE_CLICK_INTERVAL = (int) clickIntervalObj;
        } else {
            UIEntry.DOUBLE_CLICK_INTERVAL = 500;
        }
    }

    /**
     * Instantiates a {@link UIEntry} object with a given parent and underlying {@link Entry}.
     * @param parent A reference to the parent window
     * @param entry A reference to a password entry to be displayed
     */
    public UIEntry(AppMainFrame parent, Entry entry) {
        this.parent = parent;
        this.entry = entry;
        // Set up the layout with 3 columns for the 3 fields, with 5 pixels between them
        setLayout(new GridLayout(0, 3, 5, 5));
        // Create each label and add it to the component
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

        // Default to unselected colors and border
        setColors(UNSELECTED_FOREGROUND_COLOR, UNSELECTED_BACKGROUND_COLOR, UNSELECTED_BORDER);

        // Begin listening for mouse events, so we know when the user clicks the component
        usernameLabel.addMouseListener(this);
        websiteLabel.addMouseListener(this);
        passwordLabel.addMouseListener(this);
        addMouseListener(this);

        // Fill the labels with text
        updateLabels();

        // Ensure the component doesn't resize vertically in the event there aren't enough entries to fill the
        // parent container
        setMaximumSize(new Dimension(10000, (int)getPreferredSize().getHeight()));

        // Add 3 px padding around the component
        setBorder(new EmptyBorder(3, 3, 3, 3));
    }

    /**
     * Gets the underlying password entry for this UI element
     * @return a reference to the component's underlying {@link Entry} object
     */
    public Entry getEntry() {
        return entry;
    }

    /**
     * Fills the entry's labels with the underlying {@link Entry} object's information
     */
    public void updateLabels() {
        String website = entry.getWebsite();
        if(website.length() > 18) {
            websiteLabel.setText(website.substring(0, 15) + "...");
        } else {
            websiteLabel.setText(website);
        }
        String username = entry.getUsername();
        if(username.length() > 18) {
            usernameLabel.setText(username.substring(0, 15) + "...");
        } else {
            usernameLabel.setText(username);
        }
        if(entry.getPasswordData() == null) {
            passwordLabel.setText(" ");
        } else {
            passwordLabel.setText(ECHO_CHAR_STRING);
        }
        // Resize the component and redraw it to reflect the changes
        validate();
    }

    /**
     * Method called by the parent frame when the component was successfully selected. Changes the component's
     * colors to the L&F's colors for selected list components and sets the selected flag.
     */
    public void selected() {
        selected = true;
        setColors(SELECTED_FOREGROUND_COLOR, SELECTED_BACKGROUND_COLOR, SELECTED_BORDER);
        validate();
    }

    /**
     *  Changes the component's colors to reflect that it is no longer selected. Notifies the component whether the
     *  user made changes to the underlying entry
     * @param changesMade true if changes were made, otherwise false
     */
    public void deselected(boolean changesMade) {
        selected = false;
        changed = changesMade || changed;
        if(changed) {
            setColors(CHANGED_FOREGROUND_COLOR, CHANGED_BACKGROUND_COLOR, CHANGED_BORDER);
        } else {
            setColors(UNSELECTED_FOREGROUND_COLOR, UNSELECTED_BACKGROUND_COLOR, UNSELECTED_BORDER);
        }
        validate();
    }

    /**
     * Sets the component's {@link UIEntry#changed} flag
     */
    public void changesMade() {
        changed = true;
    }

    /**
     * Method called as the user releases the mouse button. If the entry was double-clicked, notifies the parent
     * frame that this component was double-clicked.
     * @param eventInfo Event information passed by Swing
     */
    @Override
    public void mouseReleased(MouseEvent eventInfo) {
        long time = eventInfo.getWhen();
        if(time < doubleClickExpire && !selected) {
            parent.entryClicked(this);
        }
        doubleClickExpire = time + DOUBLE_CLICK_INTERVAL;
    }

    /**
     * Sets the foreground and backgrounds colors, as well as border for this UI component
     * @param fgColor The foreground (text) color for the component
     * @param bgColor The background color for the component
     * @param border The border for the component
     */
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

    // Remaining methods are required while implementing the MouseListener interface
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}

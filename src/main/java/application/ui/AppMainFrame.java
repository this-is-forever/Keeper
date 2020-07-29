package application.ui;

import application.ConfigurationManager;
import application.Main;
import crypto.*;
import swingextended.*;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * The main GUI window for the application
 */
public class AppMainFrame extends ExFrame {

    // Defines the name of the application, affecting window titles
    public static final String APPLICATION_NAME = "Keeper";
    // Defines the font used in the form fields
    public static Font FORM_FONT;
    // Defines the filename for the configuration file
    private static final String CONFIGURATION_FILE_NAME = "keeper.cfg";
    // Defines the filename for the key file, whose data is used to encrypt/decrypt the user's passwords
    private static final String KEY_FILE_NAME = "keeper.key";

    // References the text fields used for editing password entries
    private JTextField websiteField, usernameField;
    private ExFocusShowPasswordField passwordField;
    // References the menu bar, where the Settings menu will appear
    private JMenuBar menuBar;
    // References the UI component which displays the list of UIEntries
    private Box entriesBox;
    // References the scroll pane that displays entriesBox, allowing the user to scroll when lots of password entries
    // are being displayed
    private JScrollPane entryScrollPane;
    private JCheckBoxMenuItem lowercaseItem, uppercaseItem, numbersItem, symbolsItem;

    // References the manager that will load and save app settings to and from CONFIGURATION_FILE_NAME
    private final ConfigurationManager configuration;

    // References the various dialogs used by the program
    private PleaseWaitDialog waitDialog;
    private ExPasswordDialog passwordDialog;
    private InitialSetupDialog initialSetupDialog;
    private ExIntegerDialog passwordLengthDialog;

    // Flag set when the user has altered the current password entry
    private boolean changedWithoutSaving;
    // Flag set as the app is saving the archive, configuration and key files
    private boolean saving;
    // References the UIEntry being edited by the user
    private UIEntry currentEntry;
    // References a list of UIEntry objects, representing the archive's passwords after loading
    private ArrayList<UIEntry> entries;
    // References a list of UI components that will be disabled when no entry is currently being edited
    private final ArrayList<Component> toggledComponents;
    // Determines how long generated passwords will be; loaded from config and altered through Settings menu
    private int passwordGeneratorLength;
    // References the output file location for the archive and the key file
    private File archiveFile, keyFile;

    // References the app's password archive manager, used to encrypt and decrypt password entries and the archive
    // itself
    private PasswordArchiveManager archiveManager;

    public AppMainFrame() {
        // Place 5 pixels of spacing between layers and around the edges of the frame
        super(5);

        // Set up the frame's and child dialog's look and feel
        setupLookAndFeel();

        // Start the toggled components list as an empty list
        toggledComponents = new ArrayList<>();

        // Create a handle to the configuration file
        File configFile = new File(Main.getExecutableDirectory() + CONFIGURATION_FILE_NAME);
        // Load the app's configuration
        configuration = new ConfigurationManager(configFile);
        configuration.load();

        // Set the password generator's length to the value defined in the user's config, or 12 if not configured
        passwordGeneratorLength = configuration.getIntProperty("passwordLength", 12);

        // Determine where the user's archive is saved
        String path = configuration.getProperty("archiveFile");
        // Was the archive file's path defined?
        if(path != null)
            // Yes; create a handle to the archive file
            archiveFile = new File(path);
        else
            // No; set archiveFile to null so createAndShow knows to prompt the user for an archive file
            archiveFile = null;
    }

    /**
     * Sets up the look and feel of various components used by the application
     */
    public static void setupLookAndFeel() {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        FORM_FONT = defaults.getFont("TextField.font").deriveFont((float)16);
        UIEntry.setupLookAndFeel();
    }

    /**
     * Method called on the Swing UI thread that creates the application's GUI and makes it visible. Most of the
     * app's functionality is defined here.
     */
    public void createAndShow() {
        // Initialize the password dialog
        passwordDialog = new ExPasswordDialog(this, "Password required");
        // Initialize the setup dialog
        initialSetupDialog = new InitialSetupDialog(this, archiveFile);
        // Determine where the key file is saved
        keyFile = new File(Main.getExecutableDirectory() + KEY_FILE_NAME);

        // If the archive file was defined in the config and it exists, prompt the user for the archive password
        if(archiveFile != null && archiveFile.exists()) {
            char[] password = passwordDialog.showAndWait();
            if(password == null)
                System.exit(0);
            try {
                archiveManager = new PasswordArchiveManager(password, keyFile);
            } catch(InvalidPasswordException e) {
                JOptionPane.showMessageDialog(this, "Incorrect password!",
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        } else {
            // Otherwise, show the initial setup dialog and wait for the user to finish
            if(!initialSetupDialog.showAndWait())
                System.exit(0);
            archiveFile = new File(initialSetupDialog.getArchivePath());
            try {
                archiveManager = new PasswordArchiveManager(initialSetupDialog.getPassword(), keyFile);
            } catch (InvalidPasswordException e) {
                JOptionPane.showMessageDialog(this, "Incorrect password!",
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            // If the key file doesn't exist yet the archive file does, we have a problem - they key file is required
            // in order to access the archive's passwords
            if(!keyFile.exists() && archiveFile.exists()) {
                JOptionPane.showMessageDialog(this, "Unable to find key file for the chosen" +
                        "archive file!", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        // Begin constructing the GUI
        setTitle(APPLICATION_NAME);

        waitDialog = new PleaseWaitDialog(this, APPLICATION_NAME);
        passwordLengthDialog = new ExIntegerDialog(this, "Keeper", "Enter password length:",
                1, 1024);

        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu settingsMenu = new JMenu("Settings");
        menuBar.add(settingsMenu);

        JMenuItem passwordLengthItem = new JMenuItem("Set password generator length...");
        passwordLengthItem.addActionListener(this::configurePasswordLengthItemPressed);
        settingsMenu.add(passwordLengthItem);

        lowercaseItem = new JCheckBoxMenuItem("Generate lowercase letters");
        lowercaseItem.setState(configuration.getBooleanProperty("lowercase", true));
        settingsMenu.add(lowercaseItem);

        uppercaseItem = new JCheckBoxMenuItem("Generate uppercase letters");
        uppercaseItem.setState(configuration.getBooleanProperty("uppercase", true));
        settingsMenu.add(uppercaseItem);

        numbersItem = new JCheckBoxMenuItem("Generate numbers");
        numbersItem.setState(configuration.getBooleanProperty("numbers", true));
        settingsMenu.add(numbersItem);

        symbolsItem = new JCheckBoxMenuItem("Generate symbols");
        symbolsItem.setState(configuration.getBooleanProperty("symbols", false));
        settingsMenu.add(symbolsItem);

        entriesBox = Box.createVerticalBox();
        entryScrollPane = new JScrollPane(entriesBox);
        entriesBox.add(Box.createVerticalGlue());

        entryScrollPane.setPreferredSize(new Dimension(-1, 400));
        Color borderColor = ((LineBorder)entryScrollPane.getBorder()).getLineColor();
        entryScrollPane.setBorder(new MatteBorder(0, 0, 1, 0, borderColor));
        entryScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        add(entryScrollPane);

        ExLayer layer = newLayer(ExLayer.CENTER);

        DocumentListener formChangedListener = new FieldChangedListener();
        websiteField = new JTextField(16);
        websiteField.setFont(FORM_FONT);
        websiteField.getDocument().addDocumentListener(formChangedListener);
        toggledComponents.add(websiteField);
        layer.add(websiteField);

        usernameField = new JTextField(16);
        usernameField.setFont(FORM_FONT);
        usernameField.getDocument().addDocumentListener(formChangedListener);
        toggledComponents.add(usernameField);
        layer.add(usernameField);
        layer.validate();
        Dimension layerSize = layer.getPreferredSize();
        layerSize.width = 10000;
        layer.setMaximumSize(layerSize);

        layer = newLayer(ExLayer.CENTER);

        passwordField = new ExFocusShowPasswordField(32);
        passwordField.enableInputMethods(true);
        passwordField.setFont(FORM_FONT);
        passwordField.getDocument().addDocumentListener(formChangedListener);
        toggledComponents.add(passwordField);
        layer.add(passwordField);

        Map<String, ImageIcon> icons = ResourceManager.loadIcons();
        Icon icon = icons.get("Copy");
        int buttonWidth = icon.getIconWidth() + 8;
        int buttonHeight = icon.getIconHeight() + 8;
        Dimension buttonSize = new Dimension(buttonWidth, buttonHeight);

        JButton button = new JButton();
        button.setIcon(icon);
        button.setToolTipText("Copy Password to Clipboard");
        button.setPreferredSize(buttonSize);
        button.setFocusable(false);
        button.addActionListener(this::copyButtonPressed);
        toggledComponents.add(button);
        layer.add(button);

        button = new JButton();
        button.setIcon(icons.get("Generate"));
        button.setToolTipText("Generate New Password");
        button.setPreferredSize(buttonSize);
        button.setFocusable(false);
        button.addActionListener(this::generateButtonPressed);
        toggledComponents.add(button);
        layer.add(button);

        ArrayList<Image> windowIcons = new ArrayList<>(2);
        windowIcons.add(icons.get("Padlock64").getImage());
        windowIcons.add(icons.get("Padlock16").getImage());
        setIconImages(windowIcons);

        button = new JButton();
        button.setIcon(icons.get("Remove"));
        button.setPreferredSize(buttonSize);
        button.setFocusable(false);
        button.setToolTipText("Delete this Password Entry");
        button.addActionListener(this::removeButtonPressed);
        toggledComponents.add(button);
        layer.add(button);
        layer.validate();
        layer.setMaximumSize(layerSize);

        layer = newLayer(ExLayer.CENTER);

        button = new JButton("Save Entry Changes");
        button.addActionListener(this::saveChangesButtonPressed);
        toggledComponents.add(button);
        layer.add(button);

        button = new JButton("Discard Entry Changes");
        button.addActionListener(this::discardChangesButtonPressed);
        toggledComponents.add(button);
        layer.add(button);

        button = new JButton("Add New Entry");
        button.addActionListener(this::newEntryButtonPressed);
        layer.add(button);

        layer.validate();
        layer.getPreferredSize();
        layerSize.width = 1000;
        layer.setMaximumSize(layerSize);

        // Disable the form until the user selects an entry or creates a new one
        setFormEnabled(false);
        // Resize to fit child components
        pack();
        // Let the OS decide where to put the frame
        setLocationByPlatform(true);
        // Make the window visible
        setVisible(true);
        // Does the archive file exist?
        if(!archiveFile.exists()) {
            // It doesn't, just create a new list of entries which we'll add to when the user starts making some
            entries = new ArrayList<>();
        } else {
            // It does; show the wait dialog and begin opening the archive on a separate thread
            SwingUtilities.invokeLater(() ->
                    waitDialog.showSelf(PleaseWaitDialog.OPENING_MESSAGE));
            new Thread(() ->{
                ArrayList<Entry> archive = archiveManager.openDatabase(archiveFile);
                if(archive == null) {
                    JOptionPane.showMessageDialog(this, "Error: Unable to open archive!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
                // Sort the list by website and then by username if websites are the same
                Collections.sort(archive);
                SwingUtilities.invokeLater(() -> {
                    // Add the entries on the Swing event thread and display them
                    entries = new ArrayList<>(archive.size());
                    for (Entry e : archive) {
                        UIEntry entry = new UIEntry(this, e);
                        entries.add(entry);
                        entriesBox.add(entry, entriesBox.getComponentCount() - 1);
                    }
                    entryScrollPane.validate();
                    // Get rid of the wait dialog now that we're done
                    waitDialog.setVisible(false);
                });
            }).start();
        }
    }

    /**
     * Method called by a {@link UIEntry} object when the object is double-clicked by the user.
     * If changes have been made to the currently selected object, the user is prompted to see if they want to save
     * changes. The clicked entry is only selected if the user clicks Yes or No, no changes were made to the
     * currently selected entry, or no entry had been selected yet.
     * If any of the above are true, the double-clicked entry is displayed in the form.
     * @param uiEntry The calling {@link UIEntry}
     */
    public void entryClicked(UIEntry uiEntry) {
        if(changedWithoutSaving) {
            if(!promptSaveEntry())
                return;
        } else if(currentEntry != null)
            currentEntry.deselected(false);
        displayEntry(uiEntry);
        setFormEnabled(true);
        websiteField.requestFocus();
    }

    /**
     * Method called when the user attempts to close the application.
     * If any changes have been made to the currently selected entry, prompts the user to see if they want to commit
     * changes before saving the archive. If the user chooses Yes or No, closing proceeds.
     * Upon successful closing, the user's settings are saved, the key file is re-encrypted using the user's password,
     * and the archive itself is updated and re-encrypted.
     * @return
     */
    @Override
    public boolean closingAttempted() {
        if(changedWithoutSaving)
            if(!promptSaveEntry())
                return false;
            else
                changedWithoutSaving = false;
        // Only execute once
        if(!saving) {
            saving = true;
            SwingUtilities.invokeLater(() -> waitDialog.showSelf(PleaseWaitDialog.SAVING_MESSAGE));
            new Thread(() -> {
                configuration.putBooleanProperty("uppercase", uppercaseItem.getState());
                configuration.putBooleanProperty("lowercase", lowercaseItem.getState());
                configuration.putBooleanProperty("numbers", numbersItem.getState());
                configuration.putBooleanProperty("symbols", symbolsItem.getState());

                configuration.putIntProperty("passwordLength", passwordGeneratorLength);
                if(archiveFile != null)
                    configuration.put("archiveFile", archiveFile.getPath());
                configuration.store();

                copyToClipboard("");

                archiveManager.closeDatabase(archiveFile, entries);
                passwordDialog.dispose();
                waitDialog.dispose();
                passwordLengthDialog.dispose();
                dispose();
            }).start();
        }
        return false;
    }

    /**
     * Method called when the user clicks the Copy button. Copies the current entry's password to the clipboard.
     * @param eventInfo Event information passed by Swing
     */
    private void copyButtonPressed(ActionEvent eventInfo) {
        copyToClipboard(new String(passwordField.getPassword()));
    }

    /**
     * Method called when the user clicks the Generate New Password button. Generates a new password defined by the
     * user's settings with a given length and list of possible symbols. Options for password generation are changed in
     * the Settings menu and are loaded & saved upon program start and exit, respectively.
     * @param eventInfo Event information passed by Swing
     */
    private void generateButtonPressed(ActionEvent eventInfo) {
        PasswordGen.setFlags(uppercaseItem.getState(), lowercaseItem.getState(),
                numbersItem.getState(), symbolsItem.getState());
        if(!PasswordGen.anySelected())
            JOptionPane.showMessageDialog(this,
                    "You must select at least one type of character to generate!", "Error",
                    JOptionPane.ERROR_MESSAGE);
        else
            passwordField.setText(new String(PasswordGen.generatePassword(passwordGeneratorLength)));
    }

    /**
     * Method called when the user presses the Remove Entry button. Removes the currently selected password entry
     * from the archive.
     * @param eventInfo Event information passed by Swing
     */
    private void removeButtonPressed(ActionEvent eventInfo) {
        if(currentEntry != null) {
            int index = entries.indexOf(currentEntry);
            currentEntry = null;
            entries.remove(index);
            entriesBox.remove(index);
            entriesBox.revalidate();
            entriesBox.repaint();
        }
        clearForm();
        setFormEnabled(false);
    }

    /**
     * Method called when the Save Entry Changes button is pressed. Commits any changes made to the selected entry.
     * @param eventInfo Event information passed by Swing
     */
    private void saveChangesButtonPressed(ActionEvent eventInfo) {
        Entry e = currentEntry.getEntry();
        e.setWebsite(websiteField.getText());
        e.setUsername(usernameField.getText());
        e.setPassword(archiveManager, passwordField.getPassword());
        currentEntry.updateLabels();
        currentEntry.changesMade();
        changedWithoutSaving = false;
    }

    /**
     * Method called when the Discard Entry Changes button is pressed. Resets any changes made to the currently selected
     * password entry.
     * @param eventInfo Event info passed by Swing
     */
    private void discardChangesButtonPressed(ActionEvent eventInfo) {
        Entry entry = currentEntry.getEntry();
        websiteField.setText(entry.getWebsite());
        usernameField.setText(entry.getUsername());
        passwordField.setText(entry.getPassword(archiveManager));
        changedWithoutSaving = false;
    }

    /**
     * Method called when the New Entry button is pressed. Adds a new password entry to the archive, prompting the user
     * to save changes to the current entry if any changes have been made.
     * @param eventInfo Event information passed by Swing.
     */
    private void newEntryButtonPressed(ActionEvent eventInfo) {
        if (changedWithoutSaving) {
            if (!promptSaveEntry())
                return;
            else
                changedWithoutSaving = false;
        } else if(currentEntry != null)
            currentEntry.deselected(false);
        UIEntry newEntry = new UIEntry(this, new Entry("", "", null));
        newEntry.selected();
        displayEntry(newEntry);
        entriesBox.add(newEntry, entriesBox.getComponentCount() - 1);
        entries.add(newEntry);
        entryScrollPane.revalidate();
        SwingUtilities.invokeLater(() -> {
            JScrollBar scrollBar = entryScrollPane.getVerticalScrollBar();
            scrollBar.setValue(scrollBar.getMaximum());
        });
        setFormEnabled(true);
        websiteField.requestFocus();
    }

    /**
     * Method called when the Configure Password Generator Length menu item under the Settings menu is pressed. Shows
     * the generator length dialog, allowing the user to change how long generated passwords will be.
     * @param eventInfo Even information passed by Swing
     */
    private void configurePasswordLengthItemPressed(ActionEvent eventInfo) {
        int length = passwordLengthDialog.showAndWait(passwordGeneratorLength);
        if(length >= 1)
            passwordGeneratorLength = length;
    }

    /**
     * Enabled or disables all of the components in {@link AppMainFrame#toggledComponents}.
     * @param b true if the components should be enabled, otherwise false
     */
    private void setFormEnabled(boolean b) {
        for(Component c : toggledComponents)
            c.setEnabled(b);
    }

    /**
     * Clears the form, emptying the website, username and password fields and resets the changedWithoutSaving flag.
     */
    private void clearForm() {
        websiteField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        changedWithoutSaving = false;
    }

    /**
     * Copies a string of text to the clipboard.
     * @param s A {@link String} object with text to copy to the clipboard.
     */
    private void copyToClipboard(String s) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        if(tk == null) {
            System.out.println("Unable to fetch system toolkit");
            return;
        }
        Clipboard clipboard = tk.getSystemClipboard();
        if(clipboard == null) {
            System.out.println("Unable to fetch system clipboard");
            return;
        }
        StringSelection clip = new StringSelection(s);
        clipboard.setContents(clip, null);
    }

    /**
     * Displays a given password entry, filling the form with its information. Resets the changedWithoutSaving flag in
     * the process.
     * @param uiEntry A reference to a {@link UIEntry to display} object representing the password entry to display
     */
    private void displayEntry(UIEntry uiEntry) {
        Entry entry = uiEntry.getEntry();
        websiteField.setText(entry.getWebsite());
        usernameField.setText(entry.getUsername());
        passwordField.setText(entry.getPassword(archiveManager));
        currentEntry = uiEntry;
        currentEntry.selected();

        changedWithoutSaving = false;
    }

    /**
     * Prompts the user to choose whether they want to commit changes made to the currently selected password entry.
     * If the user chooses Yes, changes are saved; if No, the changes are discarded; if Cancel, nothing happens and the
     * method returns false.
     * @return true if Yes or No were chosen, otherwise false
     */
    private boolean promptSaveEntry() {
        final int option = JOptionPane.showConfirmDialog(this, "Do you want to save changes" +
                " to the current entry?");
        if(option == JOptionPane.CANCEL_OPTION)
            return false;
        // Update the current entry if one was being edited
        if(currentEntry != null) {
            if (option == JOptionPane.YES_OPTION) {
                Entry e = currentEntry.getEntry();
                e.setWebsite(websiteField.getText());
                e.setUsername(usernameField.getText());
                e.setPassword(archiveManager, passwordField.getPassword());
                currentEntry.updateLabels();
                currentEntry.deselected(true);
            } else
                currentEntry.deselected(false);
        }
        // Notify the user either saved or chose not to
        return true;
    }

    /**
     * Inner class used to set the changedWithoutSaving flag when the user makes changes to an entry
     */
    private class FieldChangedListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            changedWithoutSaving = true;
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changedWithoutSaving = true;
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            changedWithoutSaving = true;
        }

    }
}

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
public class AppMainFrame extends ExFrame implements DocumentListener {

    // Defines the name of the application, affecting window titles
    public static final String APPLICATION_NAME = "Keeper";
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
        super(5);
        toggledComponents = new ArrayList<>();
        File configFile = new File(Main.getExecutableDirectory() + CONFIGURATION_FILE_NAME);
        configuration = new ConfigurationManager(configFile);
        configuration.load();

        passwordGeneratorLength = configuration.getIntProperty("passwordLength", 12);
        String path = configuration.getProperty("archiveFile");
        if(path != null)
            archiveFile = new File(path);
        else
            archiveFile = null;
    }

    public void createAndShow() {
        passwordDialog = new ExPasswordDialog(this, "Password required");
        initialSetupDialog = new InitialSetupDialog(this, archiveFile);
        keyFile = new File(Main.getExecutableDirectory() + KEY_FILE_NAME);
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
            if(!keyFile.exists() && archiveFile.exists()) {
                JOptionPane.showMessageDialog(this, "Unable to find key file for the chosen" +
                        "archive file!", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        setTitle(APPLICATION_NAME);

        PasswordGen.setFlags(true, true, true, true);

        waitDialog = new PleaseWaitDialog(this, APPLICATION_NAME);
        passwordLengthDialog = new ExIntegerDialog(this, "Keeper", "Enter password length:",
                1, 1024);

        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Font f = defaults.getFont("TextField.font").deriveFont((float)16);
        UIEntry.SELECTED_FOREGROUND_COLOR = defaults.getColor("List.selectionForeground");
        UIEntry.SELECTED_BACKGROUND_COLOR = defaults.getColor("List.selectionBackground");
        UIEntry.SELECTED_BORDER = new LineBorder(UIEntry.SELECTED_BACKGROUND_COLOR, UIEntry.BORDER_THICKNESS);
        UIEntry.UNSELECTED_FOREGROUND_COLOR = defaults.getColor("List.foreground");
        UIEntry.UNSELECTED_BACKGROUND_COLOR = defaults.getColor("List.background");
        UIEntry.UNSELECTED_BORDER = new LineBorder(UIEntry.UNSELECTED_BACKGROUND_COLOR, UIEntry.BORDER_THICKNESS);
        char echoChar = (char)defaults.get("PasswordField.echoChar");
        UIEntry.ECHO_CHAR_STRING = Character.toString(echoChar).repeat(12);
        Object clickIntervalObj = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
        if(clickIntervalObj instanceof Integer)
            UIEntry.DOUBLE_CLICK_INTERVAL = (int)clickIntervalObj;
        else
            UIEntry.DOUBLE_CLICK_INTERVAL = 500;

        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu settingsMenu = new JMenu("Settings");
        menuBar.add(settingsMenu);

        JMenuItem passwordLengthItem = new JMenuItem("Set password generator length...");
        passwordLengthItem.addActionListener(this::showPasswordLengthDialog);
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

        JMenuItem configCustomItem = new JMenuItem("Configure custom characters...");

        entriesBox = Box.createVerticalBox();
        entryScrollPane = new JScrollPane(entriesBox);
        entriesBox.add(Box.createVerticalGlue());
        UIEntry.FONT = f;

        entryScrollPane.setPreferredSize(new Dimension(-1, 400));
        Color borderColor = ((LineBorder)entryScrollPane.getBorder()).getLineColor();
        entryScrollPane.setBorder(new MatteBorder(0, 0, 1, 0, borderColor));
        entryScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        add(entryScrollPane);

        ExLayer layer = newLayer(ExLayer.CENTER);

        websiteField = new JTextField(16);
        websiteField.setFont(f);
        websiteField.getDocument().addDocumentListener(this);
        toggledComponents.add(websiteField);
        layer.add(websiteField);

        usernameField = new JTextField(16);
        usernameField.setFont(f);
        usernameField.getDocument().addDocumentListener(this);
        toggledComponents.add(usernameField);
        layer.add(usernameField);
        layer.validate();
        Dimension layerSize = layer.getPreferredSize();
        layerSize.width = 1000;
        layer.setMaximumSize(layerSize);

        layer = newLayer(ExLayer.CENTER);

        passwordField = new ExFocusShowPasswordField(32);
        passwordField.enableInputMethods(true);
        passwordField.setFont(f);
        passwordField.getDocument().addDocumentListener(this);
        toggledComponents.add(passwordField);
        layer.add(passwordField);

        final String[] iconNames = { "Generate.png", "Remove.png", "Copy.png", "Padlock64.png", "Padlock16.png"};
        Map<String, ImageIcon> icons= loadIcons(iconNames);
        Icon icon = icons.get("Copy.png");
        int buttonWidth = icon.getIconWidth() + 8;
        int buttonHeight = icon.getIconHeight() + 8;
        Dimension buttonSize = new Dimension(buttonWidth, buttonHeight);

        JButton button = new JButton();
        button.setIcon(icon);
        button.setToolTipText("Copy Password to Clipboard");
        button.setPreferredSize(buttonSize);
        button.setFocusable(false);
        button.addActionListener(this::copy);
        toggledComponents.add(button);
        layer.add(button);

        button = new JButton();
        button.setIcon(icons.get("Generate.png"));
        button.setToolTipText("Generate New Password");
        button.setPreferredSize(buttonSize);
        button.setFocusable(false);
        button.addActionListener(this::generate);
        toggledComponents.add(button);
        layer.add(button);

        ArrayList<Image> windowIcons = new ArrayList<>(2);
        windowIcons.add(icons.get("Padlock64.png").getImage());
        windowIcons.add(icons.get("Padlock16.png").getImage());
        setIconImages(windowIcons);

        button = new JButton();
        button.setIcon(icons.get("Remove.png"));
        button.setPreferredSize(buttonSize);
        button.setFocusable(false);
        button.setToolTipText("Delete this Password Entry");
        button.addActionListener(this::removeEntry);
        toggledComponents.add(button);
        layer.add(button);
        layer.validate();
        layer.getPreferredSize();
        layerSize.width = 1000;
        layer.setMaximumSize(layerSize);

        layer = newLayer(ExLayer.CENTER);

        button = new JButton("Save Entry Changes");
        button.addActionListener(this::saveChanges);
        toggledComponents.add(button);
        layer.add(button);

        button = new JButton("Discard Entry Changes");
        button.addActionListener(this::discardChanges);
        toggledComponents.add(button);
        layer.add(button);

        button = new JButton("Add New Entry");
        button.addActionListener(this::newEntry);
        layer.add(button);

        layer.validate();
        layer.getPreferredSize();
        layerSize.width = 1000;
        layer.setMaximumSize(layerSize);

        setFormEnabled(false);
        pack();

        setLocationByPlatform(true);
        setVisible(true);
        if(!archiveFile.exists()) {
            entries = new ArrayList<>();
        } else {
            SwingUtilities.invokeLater(() ->
                    waitDialog.showSelf(true));
            new Thread(() ->{
                ArrayList<Entry> archive = archiveManager.openDatabase(archiveFile);
                if(archive == null) {
                    JOptionPane.showMessageDialog(this, "Error: Unable to open archive!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                Collections.sort(archive);
                SwingUtilities.invokeLater(() -> {
                    entries = new ArrayList<>(archive.size());
                    for (Entry e : archive) {
                        UIEntry entry = new UIEntry(this, e);
                        entries.add(entry);
                        entriesBox.add(entry, entriesBox.getComponentCount() - 1);
                    }
                    entryScrollPane.validate();
                    //oldOpen();
                    waitDialog.setVisible(false);
                });
            }).start();
        }
    }

    private void saveChanges(ActionEvent eventInfo) {
        Entry e = currentEntry.getEntry();
        e.setWebsite(websiteField.getText());
        e.setUsername(usernameField.getText());
        e.setPassword(archiveManager, passwordField.getPassword());
        currentEntry.update();
        currentEntry.changesMade();
        changedWithoutSaving = false;
    }

    private void discardChanges(ActionEvent eventInfo) {
        Entry entry = currentEntry.getEntry();
        websiteField.setText(entry.getWebsite());
        usernameField.setText(entry.getUsername());
        passwordField.setText(entry.getPassword(archiveManager));
        changedWithoutSaving = false;
    }

    private void newEntry(ActionEvent eventInfo) {
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
        //entriesBox.validate();
        entryScrollPane.revalidate();
        SwingUtilities.invokeLater(() -> {
            JScrollBar scrollBar = entryScrollPane.getVerticalScrollBar();
            scrollBar.setValue(scrollBar.getMaximum());
        });
        setFormEnabled(true);
        websiteField.requestFocus();
    }

    private void removeEntry(ActionEvent eventInfo) {
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

    private void setFormEnabled(boolean b) {
        for(Component c : toggledComponents)
            c.setEnabled(b);
    }

    private void clearForm() {
        websiteField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        changedWithoutSaving = false;
    }

    private void generate(ActionEvent eventInfo) {
        PasswordGen.setFlags(uppercaseItem.getState(), lowercaseItem.getState(),
                numbersItem.getState(), symbolsItem.getState());
        passwordField.setText(new String(PasswordGen.generatePassword(passwordGeneratorLength)));
    }

    private void showPasswordLengthDialog(ActionEvent eventInfo) {
        int length = passwordLengthDialog.showAndWait(passwordGeneratorLength);
        if(length >= 1)
            passwordGeneratorLength = length;
    }

    private void copy(ActionEvent eventInfo) {
        copyToClipboard(new String(passwordField.getPassword()));
    }

    private void copyToClipboard(String s) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        assert tk != null : "Unable to fetch system toolkit";
        Clipboard clipboard = tk.getSystemClipboard();
        assert clipboard != null : "Unable to fetch system clipboard";
        StringSelection clip = new StringSelection(s);
        clipboard.setContents(clip, null);
    }

    private void displayEntry(UIEntry uiEntry) {
        Entry entry = uiEntry.getEntry();
        websiteField.setText(entry.getWebsite());
        usernameField.setText(entry.getUsername());
        passwordField.setText(entry.getPassword(archiveManager));
        currentEntry = uiEntry;
        currentEntry.selected();

        changedWithoutSaving = false;
    }

    private Map loadIcons(String [] iconNames) {
        Map<String, Icon> icons = new HashMap<>();
        for(String s : iconNames) {
            try {
                ImageIcon icon = new ImageIcon(ImageIO.read(AppMainFrame.class.getResource("icons/" + s)));
                icons.put(s, icon);
            } catch (IOException e) {
                System.err.println("Unable to load icon " + s);
            }
        }
        return icons;
    }

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
            SwingUtilities.invokeLater(() -> waitDialog.showSelf(false));
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
                dispose();
            }).start();
        }
        return false;
    }

    private boolean promptSaveEntry() {
        final int option = JOptionPane.showConfirmDialog(this, "Do you want to save changes" +
                " to the current entry?");
        if(option == JOptionPane.CANCEL_OPTION)
            return false;
        if(currentEntry != null) {
            if (option == JOptionPane.YES_OPTION) {
                Entry e = currentEntry.getEntry();
                e.setWebsite(websiteField.getText());
                e.setUsername(usernameField.getText());
                e.setPassword(archiveManager, passwordField.getPassword());
                currentEntry.update();
                currentEntry.deselected(true);
            } else
                currentEntry.deselected(false);
        }
        return true;
    }

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

    public void oldOpen() {
        byte[] iv = new byte[16];
        byte[] data = null;
        FileInputStream in = null;
        try {
            // Open the archive for reading
            in = new FileInputStream(new File(""));
            // read the first 16 bytes into iv
            in.read(iv);
            // read the rest of the archive into data
            data = new byte[in.available()];
            in.read(data);
            // close the file
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (data == null || iv == null) {
            System.out.println("Error when loading archive.");
            return;
        }

        Cipher c = null;
        try {
            c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        if (c == null) {
            System.out.println("Error when initializing cipher");
            return;
        }

        // Generate a key based on the password given for the archive
        char[] archivePassword = "".toCharArray();
        SecretKey secretKey = OldCrypto.generateKey(archivePassword);
        // Clear the contents of the created array for security purposes
        Crypto.erase(archivePassword);
        try {
            c.init(Cipher.DECRYPT_MODE, secretKey,
                    new IvParameterSpec(iv));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return;
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return;
        }
        byte[] byteDecryptedText = new byte[data.length];
        try {
            byteDecryptedText = c.doFinal(data);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return;
        }
        // The decryption was successful; read the contents into the frame

        // Start in state 0 and position 0, with the start of the first string starting at 0
        int state = 0;
        int pos = 0, start = 0;
        String website = null, account = null;
        // Iterate while the state is valid
        while (state < 3) {
            // Read bytes until we hit a null terminator; input the resulting string into the next entry

            // First string (website)
            if (state == 0) {
                if (byteDecryptedText[pos] == 0) {
                    website = new String(byteDecryptedText, start, pos - start);
                    start = pos + 1;
                    state++;
                }
            }
            // Second string (account)
            else if (state == 1) {
                if (byteDecryptedText[pos] == 0) {
                    account = new String(byteDecryptedText, start, pos - start);

                    start = pos + 1;
                    state++;
                }
            }
            // Third string (password) - Converted to char[]
            else if (state == 2) {
                if (byteDecryptedText[pos] == 0) {
                    ByteBuffer b = ByteBuffer.wrap(byteDecryptedText, start, pos - start);
                    CharBuffer charBuffer = Charset.forName("UTF-8").decode(b);
                    char[] decoded = charBuffer.array();
                    byte[] encryptedData = archiveManager.encryptPassword(decoded);
                    // Create a new keeper entry since we have all 3 components and reset the state
                    UIEntry entry = new UIEntry(this, new Entry(website, account, encryptedData));
                    entriesBox.add(entry, entriesBox.getComponentCount() - 1);
                    entries.add(entry);
                    start = pos + 1;
                    state = 0;
                }
            }
            pos++;
            // If we reached the end of the data, set the state to 3 (invalid)
            if (pos == byteDecryptedText.length)
                state = 3;
        }
        entriesBox.revalidate();
        entriesBox.repaint();
    }
}

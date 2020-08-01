package application.ui;

import crypto.CryptoUtil;
import crypto.PasswordBasedCryptographer;
import swingextended.ExLayer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Map;

/**
 * The dialog window which is displayed during the first initial program run. Allows the user to choose where to save
 * their password archive and pick the archive's master password.
 */
public class InitialSetupDialog extends JDialog implements WindowListener {

    // Defines the tutorial text displayed above the form
    private static final String firstTimeSetup = "<html><body style='width: 350px'>" +
            "<center>Welcome to Keeper!</center><br><p align='justify'>For first time set up, please choose where and "
            + "what to save your password archive as, along with a master password. Additionally, a key file, " +
            "keeper.key, will be created in the same directory as Keeper.jar. This file is needed to decrypt your " +
            "passwords, so be sure to create a back up!</p><br>" +
            "</body></html>";

    // References the parent window, for modality and location purposes
    private final AppMainFrame parent;
    // References the text field that holds the archive file's location
    private final JTextField archiveField;
    // References the master password field
    private final JPasswordField passwordField;
    // References the file chooser dialog, which displays when the Browse... button is clicked
    private final JFileChooser fileChooser;
    // Flag set when the dialog is closed with the user setting an archive file path and master password
    private boolean submitted;

    /**
     * Creates an set up dialog with a given parent window and an optional starting file path.
     * @param parent The parent window, for location and modality purposes
     * @param archiveFile A reference to a {@link File} object representing a starting file path to display in the
     *                    form.
     */
    public InitialSetupDialog(AppMainFrame parent, File archiveFile) {
        super(parent, "Configuration");
        // Maintain a reference to the parent window
        this.parent = parent;
        // Load a font to make the tutorial text pretty
        ResourceManager.loadFonts();
        // Change the icon
        setIconImage(ResourceManager.loadIcons().get("Padlock16").getImage());
        // Initialize the file chooser dialog
        fileChooser = new JFileChooser();
        // Do nothing when the user attempts to close the window
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        // Move the window to the OS's default location for new windows in the event the parent window isn't yet
        // visible
        if(!parent.isVisible()) {
            setLocationByPlatform(true);
        }
        // Ensure the dialog blocks the parent window while open
        setModal(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        // Place the starting file location in the form if one was given, otherwise leave it blank
        if(archiveFile == null) {
            archiveField = new JTextField(24);
        } else {
            archiveField = new JTextField(archiveFile.getPath(), 24);
        }
        // Add generous padding around the window's content
        JPanel contentPane = (JPanel)getContentPane();
        contentPane.setBorder(new EmptyBorder(20, 40, 10, 40));

        // Lay out child components top to bottom
        setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        // Add the instruction text to the frame
        JLabel infoLabel = new JLabel(firstTimeSetup);
        infoLabel.setFont(new Font("Open Sans Light", Font.PLAIN, 18));
        add(infoLabel);
        // Add the archive file location field and the Browse... button to the frame
        ExLayer layer = new ExLayer(ExLayer.LEFT, 5);
        layer.add(new JLabel("Archive file:"));
        add(layer);
        add(Box.createVerticalStrut(5));

        layer = new ExLayer(ExLayer.CENTER, 5);
        layer.add(archiveField);
        archiveField.addActionListener(this::formSubmitted);
        JButton button = new JButton("Browse...");
        button.addActionListener(this::browseArchive);
        layer.add(button);
        add(layer);

        // Add 5 px of padding between these layers
        add(Box.createVerticalStrut(5));

        // Add the master password field to the frame
        layer = new ExLayer(ExLayer.LEFT, 5);
        layer.add(new JLabel("Master password:"));
        add(layer);
        add(Box.createVerticalStrut(5));

        passwordField = new JPasswordField(30);
        layer = new ExLayer(ExLayer.CENTER, 5);
        passwordField.addActionListener(this::formSubmitted);
        layer.add(passwordField);
        Map<String, ImageIcon> icons = ResourceManager.loadIcons();
        JButton showPasswordButton = new JButton(icons.get("View"));
        showPasswordButton.setToolTipText("Show/Hide Password");
        Dimension d = showPasswordButton.getPreferredSize();
        showPasswordButton.setPreferredSize(new Dimension(d.height, d.height));
        showPasswordButton.addActionListener(this::showPasswordButtonPressed);
        layer.add(showPasswordButton);
        add(layer);

        add(Box.createVerticalStrut(5));

        // Add the Ok and Cancel buttons to the frame
        layer = new ExLayer(ExLayer.CENTER, 5);
        button = new JButton("Ok");
        button.addActionListener(this::formSubmitted);
        layer.add(button);
        button = new JButton("Cancel");
        button.addActionListener(this::formCanceled);
        layer.add(button);
        add(layer);

        // Begin listening for window events, so we know when the user closes the dialog
        addWindowListener(this);
        // Resize the frame to fit its children
        pack();
    }

    /**
     * Shows the dialog and blocks the parent window until the dialog is submitted, canceled, closed or hidden.
     * @return true if the form was submitted, false if it was canceled or closed without submitting
     */
    public boolean showAndWait() {
        // Move the window in front of its parent if the parent is visible
        if(parent != null && parent.isVisible()) {
            setLocationRelativeTo(parent);
        }
        // Display the window; this thread blocks here until the dialog is made invisible via setVisible(false)
        setVisible(true);
        // Return the result
        return submitted;
    }

    /**
     * Gets the master password from the dialog
     * @return a char[] array containing the master password given by the user
     */
    public char[] getPassword() {
        return passwordField.getPassword();
    }

    /**
     * Gets the archive file path provided by the user
     * @return A {@link String} object containing the path
     */
    public String getArchivePath() {
        return archiveField.getText();
    }

    /**
     * Method called when the user attempts to close the dialog without submitting the form. Hides the window and
     * resets the submitted flag, notifying the thread that called {@link InitialSetupDialog#showAndWait()} that the
     * form was cancelled.
     * @param eventInfo Event information passed by Swing
     */
    @Override
    public void windowClosing(WindowEvent eventInfo) {
        formCanceled(null);
    }

    /**
     * Method called when the user hits the Enter key while the form has focus.
     * Validates the form information. If valid information was given, the dialog is hidden and the thread that called
     * {@link InitialSetupDialog#showAndWait} that the user submitted the form; if invalid information was given, the
     * dialog remains open.
     * @param eventInfo Event information passed by Swing
     */
    private void formSubmitted(ActionEvent eventInfo) {
        // Create a handle to the file's directory
        File test = new File(archiveField.getText()).getParentFile();
        // Ensure the directory exists; if not, we have a problem - cancel form submission
        if(!(test.exists() && test.isDirectory())) {
            JOptionPane.showMessageDialog(this, "Please enter a valid file path!");
            archiveField.requestFocus();
            return;
        }
        // Ensure the user entered a password; if not, we have a problem - cancel form submission
        char[] password = passwordField.getPassword();
        CryptoUtil.erase(password);
        if(password.length == 0) {
            JOptionPane.showMessageDialog(this, "You must enter a password!");
            return;
        }
        // Everything looks good; hide the window and set the submitted flag
        submitted = true;
        setVisible(false);
    }

    /**
     * Method called when the dialog is closed or the Cancel button is pressed. Hides the dialog and resets the
     * submitted flag.
     * @param eventInfo Event information passed by Swing
     */
    private void formCanceled(ActionEvent eventInfo) {
        submitted = false;
        setVisible(false);
    }

    /**
     * Method called when the Browse... button is clicked. Displays the file chooser dialog.
     * @param eventInfo Event information passed by Swing
     */
    private void browseArchive(ActionEvent eventInfo) {
        if(fileChooser.showDialog(this, "Create") == JFileChooser.APPROVE_OPTION) {
            archiveField.setText(fileChooser.getSelectedFile().getPath());
        }
    }

    private char echoChar;
    private void showPasswordButtonPressed(ActionEvent e) {
        char setChar = passwordField.getEchoChar();
        if(echoChar == 0) {
            echoChar = passwordField.getEchoChar();
        }
        if(setChar == 0) {
            passwordField.setEchoChar(echoChar);
        } else {
            passwordField.setEchoChar('\0');
        }
    }

    // The following methods do nothing; they are required while implementing the WindowListener interface
    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}

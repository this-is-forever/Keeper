package application.ui;

import crypto.Crypto;
import swingextended.ExFocusShowPasswordField;
import swingextended.ExLayer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

public class InitialSetupDialog extends JDialog implements WindowListener {

    private final AppMainFrame parent;

    private final JTextField archiveField;

    private final ExFocusShowPasswordField passwordField;

    private boolean submitted;

    private String firstTimeSetup = "<html><body style='width: 300px'>" +
            "Welcome to Keeper!<br>For first time set up, please choose where to save your password archive, " +
            "along with a master password for the archive. You can name your archive file with any name and any " +
            "extension. Your master password will be required to retrieve your passwords." +
            " Keeper will remember where your archive file is saved. Additionally, a key file, keeper.key, will be " +
            "created in the same directory as Keeper.jar. It's recommended that you keep both Keeper and this key file " +
            "offline, as the key file is needed to decrypt your passwords. This is an additional layer of security." +
            " Without the key file, your passwords cannot be accessed. Be sure to back up your key file!" +
            "</body></html>";

    public InitialSetupDialog(AppMainFrame parent, File archiveFile) {
        super(parent, "Configuration");
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        if(!parent.isVisible())
            setLocationByPlatform(true);
        setModal(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        this.parent = parent;
        if(archiveFile == null)
            archiveField = new JTextField(40);
        else
            archiveField = new JTextField(archiveFile.getPath(), 40);
        JPanel contentPane = (JPanel)getContentPane();
        contentPane.setBorder(new EmptyBorder(20, 40, 20, 40));
        setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        add(new JLabel(firstTimeSetup));

        ExLayer layer = new ExLayer(ExLayer.LEFT, 5);
        layer.add(new JLabel("Archive file:"));
        add(layer);

        passwordField = new ExFocusShowPasswordField(40);

        layer = new ExLayer(ExLayer.CENTER, 5);
        layer.add(archiveField);
        archiveField.addActionListener(this::formSubmitted);
        JButton button = new JButton("Browse...");
        button.addActionListener(this::browseArchive);
        layer.add(button);
        add(layer);

        add(Box.createVerticalStrut(5));

        layer = new ExLayer(ExLayer.LEFT, 5);
        layer.add(new JLabel("Master password:"));
        add(layer);

        layer = new ExLayer(ExLayer.CENTER, 5);
        passwordField.addActionListener(this::formSubmitted);
        layer.add(passwordField);
        add(layer);

        layer = new ExLayer(ExLayer.CENTER, 5);
        button = new JButton("Ok");
        button.addActionListener(this::formSubmitted);
        layer.add(button);
        button = new JButton("Cancel");
        button.addActionListener(this::formCanceled);
        layer.add(button);
        add(layer);

        addWindowListener(this);
        pack();
    }

    public boolean showAndWait() {
        setVisible(true);
        return submitted;
    }

    private void formSubmitted(ActionEvent eventInfo) {
        File test = new File(archiveField.getText()).getParentFile();
        if(!(test.exists() && test.isDirectory())) {
            JOptionPane.showMessageDialog(this, "Please enter a valid file path!");
            archiveField.requestFocus();
            return;
        }
        char[] password = passwordField.getPassword();
        Crypto.erase(password);
        if(password.length == 0) {
            JOptionPane.showMessageDialog(this, "You must enter a password!");
            return;
        }
        submitted = true;
        setVisible(false);
    }

    private void formCanceled(ActionEvent eventInfo) {
        submitted = false;
        setVisible(false);
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }

    public String getArchivePath() {
        return archiveField.getText();
    }

    private void browseArchive(ActionEvent eventInfo) {

    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        formCanceled(null);
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

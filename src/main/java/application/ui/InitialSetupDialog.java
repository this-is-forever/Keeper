package application.ui;

import crypto.Crypto;
import swingextended.ExFocusShowPasswordField;
import swingextended.ExLayer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;

public class InitialSetupDialog extends JDialog implements WindowListener {

    private final AppMainFrame parent;

    private final JTextField archiveField;

    private final ExFocusShowPasswordField passwordField;

    private final JFileChooser fileChooser;

    private boolean submitted;

    private String firstTimeSetup = "<html><body style='width: 350px'>" +
            "<center>Welcome to Keeper!</center><br><p align='justify'>For first time set up, please choose where and "
            + "what to save your password archive as, along with a master password. Additionally, a key file, " +
            "keeper.key, will be created in the same directory as Keeper.jar. This file is needed to decrypt your " +
            "passwords, so be sure to create a back up!</p><br>" +
            "</body></html>";

    public InitialSetupDialog(AppMainFrame parent, File archiveFile) {
        super(parent, "Configuration");
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Font f = Font.createFont(Font.TRUETYPE_FONT,
                    AppMainFrame.class.getResource("fonts/OpenSans-Light.ttf").openStream());
            ge.registerFont(f);
        } catch (FontFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileChooser = new JFileChooser();

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        if(!parent.isVisible())
            setLocationByPlatform(true);
        setModal(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        this.parent = parent;
        if(archiveFile == null)
            archiveField = new JTextField(24);
        else
            archiveField = new JTextField(archiveFile.getPath(), 30);
        JPanel contentPane = (JPanel)getContentPane();
        contentPane.setBorder(new EmptyBorder(20, 40, 20, 40));
        setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JLabel infoLabel = new JLabel(firstTimeSetup);
        infoLabel.setFont(new Font("Open Sans Light", Font.PLAIN, 18));
        add(infoLabel);
        //add(Box.createVerticalStrut());

        ExLayer layer = new ExLayer(ExLayer.LEFT, 5);
        layer.add(new JLabel("Archive file:"));
        add(layer);

        passwordField = new ExFocusShowPasswordField(30);

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
        if(fileChooser.showDialog(this, "Create") == JFileChooser.APPROVE_OPTION) {
            archiveField.setText(fileChooser.getSelectedFile().getPath());
        }
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

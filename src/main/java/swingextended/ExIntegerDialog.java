package swingextended;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.*;

public class ExIntegerDialog extends JDialog implements ActionListener, WindowFocusListener, KeyListener {
    // A reference to the text box in which the user will enter a password
    private JTextField inputField;
    // References the application's main window
    private JFrame owner;
    // References to the dialog's buttons
    private JButton okButton, cancelButton;
    // A flag set when the user hits Ok or presses Enter
    private boolean submitted;

    private final int lowerBound, upperBound;

    /**
     * Creates a new {@link ExPasswordDialog} object
     * @param owner A reference to the dialog's owner
     */
    public ExIntegerDialog(JFrame owner, String title, String prompt, int lowerBound, int upperBound) {
        super(owner);
        this.owner = owner;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        setTitle(title);
        if(!owner.isVisible())
            setLocationByPlatform(true);
        create(prompt);
    }

    public void setParent(JFrame owner) {
        this.owner = owner;
    }

    /**
     * Clears the text in the password field
     */
    public void clear() {
        inputField.setText("");
    }

    /**
     * Displays the password dialog and waits until the user enters the information or cancels
     * @return true if the user entered a password, otherwise false (the user canceled)
     */
    public int showAndWait(int initialValue) {
        // Reset the submitted flag
        submitted = false;
        inputField.setText(Integer.toString(initialValue));
        // Ensure the password field has focused
        inputField.requestFocus();
        // Move the dialog so it's centered over the parent frame
        setLocationRelativeTo(getParent());
        // Show the dialog and wait until the dialog is closed
        setVisible(true);
        // Return the result (flag is manipulated by another thread)
        String input = inputField.getText();
        if(submitted)
            return Integer.parseInt(input);
        return -1;
    }

    /**
     * Method called when the user hits the Enter key, Ok or Cancel
     * @param eventInfo Event information
     */
    @Override
    public void actionPerformed(ActionEvent eventInfo) {
        Object source = eventInfo.getSource();
        // Did the user click the OK button or hit enter?
        if(source == okButton || source == inputField) {
            // Validate the input
            try {
                int i = Integer.parseInt(inputField.getText());
                if(i < lowerBound || i > upperBound)
                    return;
            } catch(NumberFormatException e) {
                return;
            }
            // Yes; set the submitted flag
            submitted = true;
        }
        if(source == cancelButton || source == okButton || source == inputField)
            // Hide the dialog and go back to the main application frame
            setVisible(false);
    }

    /**
     * Ensures the main application window is brought to the front when the application loses and then regains
     * focus while the dialog is open
     * @param e Event information passed by Swing
     */
    @Override
    public void windowGainedFocus(WindowEvent e) {
        if(owner != null)
            owner.toFront();
    }

    @Override
    public void windowLostFocus(WindowEvent e) {

    }

    /**
     * Creates the dialog frame's components, adds them to itself and resizes to fit its children
     */
    private void create(String promptText) {
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        // Ensure the dialog maintains focus while open
        setModal(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        // Prevent the user from resizing the dialog
        setResizable(false);
        // Begin listening for when the window gains focus
        addWindowFocusListener(this);
        // Add padding around the dialog's contents
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(5, 10, 5, 10));
        // Lay out children vertically
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // Create a new left-aligned layer
        Box layer = new ExLayer(ExLayer.LEFT, 5);
        // Add a descriptive label to it
        layer.add(new JLabel(promptText));
        // Add the layer to the dialog's content pane
        add(layer);

        layer = new ExLayer(ExLayer.LEFT, 5);
        // Create a new password field sized to fit 24 characters
        inputField = new JTextField();
        // Add self as an action listener for the password field (activates when the user presses enter when the
        // field has focus
        inputField.addActionListener(this);
        inputField.addKeyListener(this);
        layer.add(inputField);
        add(layer);

        layer = new ExLayer(ExLayer.CENTER, 5);

        okButton = new JButton("OK");
        okButton.addActionListener(this);
        layer.add(okButton);

        cancelButton = new JButton("Cancel");
        layer.add(cancelButton);
        cancelButton.addActionListener(this);

        add(layer);
        // Resize the dialog to fit its children
        pack();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        char input = e.getKeyChar();
        if(input < '0' || input > '9')
            e.consume();
    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}

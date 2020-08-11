package com.github.thisisforever.keeper.swingx;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.*;

/**
 * A dialog which prompts the user for an integer value between a given lower and upper bound (inclusive)
 */
public class ExIntegerDialog extends JDialog implements ActionListener, WindowFocusListener, KeyListener {
    // A reference to the text box in which the user will enter a password
    private JTextField inputField;
    // References the application's main window
    private JFrame owner;
    // References to the dialog's buttons
    private JButton okButton, cancelButton;
    // A flag set when the user hits Ok or presses Enter
    private boolean submitted;

    // Defines the lower and upper bounds of the dialog's range of valid input values
    private final int lowerBound, upperBound;

    /**
     * Creates a new {@link ExIntegerDialog} object with a given parent, title, prompt text, lower bound and
     * upper bound
     * @param owner A reference to the dialog's parent window
     * @param title A {@link String} containing text that will be displayed on the title bar
     * @param prompt A {@link String} containing text that will be displayed in the prompt
     * @param lowerBound the lower bound of valid values for this dialog
     * @param upperBound the upper bound (inclusive) of valid values for this dialog
     */
    public ExIntegerDialog(JFrame owner, String title, String prompt, int lowerBound, int upperBound) {
        super(owner);
        this.owner = owner;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        setTitle(title);
        if(!owner.isVisible()) {
            setLocationByPlatform(true);
        }
        create(prompt);
    }

    /**
     * Clears the text in the input field
     */
    public void clear() {
        inputField.setText("");
    }

    /**
     * Displays the input dialog and waits until the user enters the information or cancels
     * @return the value the user entered if the form was submitted, otherwise -1
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
        if(submitted) {
            return Integer.parseInt(input);
        }
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
                if(i < lowerBound || i > upperBound) {
                    return;
                }
            } catch(NumberFormatException e) {
                return;
            }
            // Yes; set the submitted flag
            submitted = true;
        }
        if(source == cancelButton || source == okButton || source == inputField) {
            // Hide the dialog and go back to the main application frame
            setVisible(false);
        }
    }

    /**
     * Ensures the main application window is brought to the front when the application loses and then regains
     * focus while the dialog is open
     * @param eventInfo Event information passed by Swing
     */
    @Override
    public void windowGainedFocus(WindowEvent eventInfo) {
        if(owner != null) {
            owner.toFront();
        }
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

    /**
     * Method called when the user presses a key within the input field. Ensures only number characters are
     * read.
     * @param eventInfo Information about the event passed by Swing
     */
    @Override
    public void keyTyped(KeyEvent eventInfo) {
        char input = eventInfo.getKeyChar();
        if(input < '0' || input > '9') {
            eventInfo.consume();
        }
    }

    // Remaining methods were required to implement the WindowFocusListener and KeyListener interfaces
    @Override
    public void windowLostFocus(WindowEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}

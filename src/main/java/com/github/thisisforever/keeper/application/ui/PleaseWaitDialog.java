package com.github.thisisforever.keeper.application.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

/**
 * A modal dialog popup that displays "Opening/Saving archive, please wait..." to prevent user input while the archive
 * is being opened or saved.
 */
public class PleaseWaitDialog extends JDialog implements WindowFocusListener {

    // Flags used to determine which message to show when showSelf(boolean) is called
    public static final boolean OPENING_MESSAGE = true;
    public static final boolean SAVING_MESSAGE = false;

    // References the parent window, for location and modality purposes
    private final JFrame parent;
    // References the label which displays the please wait message
    private final JLabel label;

    /**
     * Creates the please wait dialog with a given parent window and title. Does not make the dialog visible until
     * {@link PleaseWaitDialog#showSelf} is called.
     * @param parent A reference to a {@link JFrame} that is the parent window to this dialog. The parent window
     *              is blocked while the dialog is visible.
     * @param title A title for the dialog window.
     */
    public PleaseWaitDialog(JFrame parent, String title) {
        super(parent, title);
        this.parent = parent;
        // Prevent the user from closing the dialog window
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        // Ensure the dialog blocks the parent window while open
        setModal(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        // Begin listening for when the window gains focus
        addWindowFocusListener(this);
        // Prevent resizing
        setResizable(false);
        // Create generous padding around the frame's content
        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setBorder(new EmptyBorder(25, 30, 25, 30));
        // Lay out items top to bottom
        setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        // Add a label to the window, whose text will be changed when showSelf(boolean) is called
        label = new JLabel();
        add(label);
        // Resize the window to fit its children
        pack();
    }

    /**
     * Method called when the dialog window gains focus. Brings the parent window to the front of the window stack,
     * that way the parent window is visible behind the dialog window if the user switches windows while the dialog is
     * open.
     * @param eventInfo Event information passed by Swing
     */
    @Override
    public void windowGainedFocus(WindowEvent eventInfo) {
        parent.toFront();
    }

    /**
     * Method called when the dialog window loses focus. Does nothing - required while implementing
     * the {@link WindowFocusListener} interface.
     * @param eventInfo Even information passed by Swing
     */
    @Override
    public void windowLostFocus(WindowEvent eventInfo) {

    }

    /**
     * Overrides the setVisible method of JDialog so the dialog is positioned relative to the application window
     * @param visibility Flag set if the window should show
     */
    @Override
    public void setVisible(boolean visibility) {
        // Are we making the window visible and is the parent window visible?
        if(visibility && parent.isVisible()) {
            // Yes; reposition the dialog in front of the parent window
            setLocationRelativeTo(parent);
        }
        // Make the window visible
        super.setVisible(visibility);
    }

    /**
     * Shows the dialog, blocking the parent window's event thread until {@link PleaseWaitDialog#setVisible}(false) is
     * called by another thread.
     * @param opening flag set when "Opening archive" should be displayed, otherwise "Saving archive" is shown.
     */
    public void showSelf(boolean opening) {
        // Was the opening flag set?
        if(opening) {
            // Yes; display the opening message
            label.setText("Opening archive, please wait...");
        } else {
            // No; display the saving message
            label.setText("Saving archive, please wait...");
        }
        // Resize the window to fit the altered label text
        pack();
        // Show the dialog
        setVisible(true);
    }

}

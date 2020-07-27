package application.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

public class PleaseWaitDialog extends JDialog implements WindowFocusListener {

    private final JFrame owner;
    private final JLabel label;

    public PleaseWaitDialog(JFrame owner, String title) {
        super(owner, title);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.owner = owner;
        // Ensure the dialog maintains focus while open
        setModal(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        // Begin listening for when the window gains focus
        addWindowFocusListener(this);
        // Prevent resizing
        setResizable(false);
        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setBorder(new EmptyBorder(25, 30, 25, 30));
        setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        label = new JLabel();
        add(label);

        pack();
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        owner.toFront();
    }

    @Override
    public void windowLostFocus(WindowEvent e) {

    }

    /**
     * Overrides the setVisible method of JDialog so the dialog is positioned relative to the application window
     * @param visibility Flag set if the window should show
     */
    @Override
    public void setVisible(boolean visibility) {
        if(visibility)
            setLocationRelativeTo(owner);
        super.setVisible(visibility);
    }

    public void showSelf(boolean opening) {
        if(opening)
            label.setText("Opening archive, please wait...");
        else
            label.setText("Saving archive, please wait...");
        pack();
        setVisible(true);
    }

}

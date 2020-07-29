package swingextended;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * A {@link JPasswordField} whose password is shown when the field gains focus.
 */
public class ExFocusShowPasswordField extends JPasswordField implements FocusListener {

    // Defines the character the password field shows in place of the password characters when the field does not
    // have focus
    private final char echoChar;

    /**
     * Creates a new password field with the given number of columns
     * @param columns The number of columns (characters) that determine how wide the component will be
     */
    public ExFocusShowPasswordField(int columns) {
        super(columns);
        echoChar = getEchoChar();
        addFocusListener(this);
    }

    /**
     * Method called when the component gains focus. Makes the password visible
     * @param eventInfo Event information passed by Swing
     */
    @Override
    public void focusGained(FocusEvent eventInfo) {
        setEchoChar('\0');
    }

    /**
     * Method called when the component loses focus. Hides the password
     * @param eventInfo Event information passed by Swing
     */
    @Override
    public void focusLost(FocusEvent eventInfo) {
        setEchoChar(echoChar);
    }
}

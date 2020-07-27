package swingextended;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class ExFocusShowPasswordField extends JPasswordField implements FocusListener {

    private final char echoChar;

    public ExFocusShowPasswordField(int columns) {
        super(columns);
        echoChar = getEchoChar();
        addFocusListener(this);
    }

    @Override
    public void focusGained(FocusEvent e) {
        setEchoChar('\0');
    }

    @Override
    public void focusLost(FocusEvent e) {
        setEchoChar(echoChar);
    }
}

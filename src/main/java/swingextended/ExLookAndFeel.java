package swingextended;

import javax.swing.*;

/**
 * Used to set Swing's Look & Feel to the user's operating system
 */
public class ExLookAndFeel {

    // Flag used by setLookAndFeel to ensure the look and feel is only loaded once
    private static boolean lookAndFeelSet;

    /**
     * Sets the look and feel to match the user's operating system.
     * @return true if successful, otherwise false
     */
    public static boolean set() {
        // Only set the L&F once
        if(lookAndFeelSet) {
            return true;
        }
        // Attempt to set the look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            lookAndFeelSet = true;
            return true;
        } catch(ClassNotFoundException | IllegalAccessException |
                InstantiationException | UnsupportedLookAndFeelException e) {
            return false;
        }
    }

}

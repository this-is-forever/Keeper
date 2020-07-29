package application;

import application.ui.AppMainFrame;

import javax.swing.*;

/**
 * Program entry point
 */
public class Main {

    /**
     * Runs the program.
     * @param args Currently unused arguments from the command line
     */
    public static void main(String[] args) {
        AppMainFrame app = new AppMainFrame();
        SwingUtilities.invokeLater(app::createAndShow);
    }

    /**
     * Gets the root directory of the currently running program, for the sake of loading configuration
     * @return a {@link String} representing the directory of the program
     */
    public static String getExecutableDirectory() {
        String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        int lastIndex = path.lastIndexOf("/");
        return path.substring(0, lastIndex+1);
    }

}

package application.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResourceManager {

    // Defines PNG files to load from the icons folder
    private static final String[] APPLICATION_ICONS = {
            "View",
            "Copy",
            "Generate",
            "Padlock16",
            "Padlock64",
            "Remove"
    };

    // Defines font files to load from the fonts folder
    private static final String[] APPLICATION_FONTS = {
            "OpenSans-Light.ttf"
    };

    // References a map created by loadIcons that contains icons, retrieved using font names
    private static Map<String, ImageIcon> icons;

    // Flag set once the fonts have been loaded, to ensure they only load once
    private static boolean fontsLoaded;

    /**
     * Loads applications used by the application, defined in {@link ResourceManager#APPLICATION_ICONS}
     * @return A {@link Map <String,  Icon >} object that maps icon names to Icon objects
     */
    public static Map<String, ImageIcon> loadIcons() {
        if(icons != null)
            return icons;
        icons = new HashMap<>();
        for(String s : APPLICATION_ICONS) {
            try {
                ImageIcon icon = new ImageIcon(ImageIO.read(
                        ResourceManager.class.getResource("icons/" + s + ".png")));
                icons.put(s, icon);
            } catch (IOException e) {
                System.err.println("Unable to load icon " + s);
            }
        }
        return icons;
    }

    /**
     * Loads fonts defined in {@link ResourceManager#APPLICATION_FONTS} from the fonts folder, allowing them to be
     * used when creating {@link Font} objects.
     */
    public static void loadFonts() {
        if(fontsLoaded)
            return;
        fontsLoaded = true;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for(String file : APPLICATION_FONTS) {
            try {
                Font f = Font.createFont(Font.TRUETYPE_FONT,
                        AppMainFrame.class.getResource("fonts/" + file).openStream());
                ge.registerFont(f);
            } catch (FontFormatException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

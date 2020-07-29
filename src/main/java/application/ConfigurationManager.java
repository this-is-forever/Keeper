package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * A class that loads and saves application configuration from and to a specified configuration file. Includes methods
 * that allow reading and writing integer and boolean values from the configuration file.
 */
public class ConfigurationManager extends Properties {

    // References the file which the configuration is loaded from and saved to
    private File file;

    /**
     * Instantiates a {@link ConfigurationManager} object with a given file
     * @param f A reference to a {@link File} object representing the file to fetch and save config from/to
     */
    public ConfigurationManager(File f) {
        file = f;
    }

    /**
     * Attempts to load configuration data from {@link ConfigurationManager#file}
     * @return true on success, otherwise false
     */
    public boolean load() {
        if(!file.exists()) {
            System.err.println("Configuration file did not exist.");
            return false;
        }
        try(FileInputStream in = new FileInputStream(file)) {
            load(in);
            return true;
        } catch (IOException e) {
            System.err.println("Error reading config file.");
            return false;
        }
    }

    /**
     * Saves this {@link ConfigurationManager}'s configuration information to {@link ConfigurationManager#file}
     */
    public void store() {
        try(FileOutputStream out = new FileOutputStream(file)) {
            store(out, "Application settings");
        } catch(IOException e) {
            e.printStackTrace();
            System.out.println("Unable to save configuration.");
        }
    }

    /**
     * Gets a boolean property from the configuration information, or null if that key was not defined
     * @param key A key with which to retrieve the configuration information
     * @return a {@link Boolean} object with true or false value based on the configuration, or null if the key
     * did not exist.
     */
    public Boolean getBooleanProperty(String key) {
        String prop = getProperty(key);
        if(prop == null)
            return null;
        return Boolean.parseBoolean(prop);
    }

    /**
     * Retrieves a boolean value from the configuration, with a default value in the event the given key did not exist
     * @param key The key with which to retrieve configuration info
     * @param defaultValue A value to default to in the event the key did not exist
     * @return true if the configuration's setting for the given key was true, otherwise false. If the configuration
     * did not specify the given setting, returns the given default value
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        Boolean b = getBooleanProperty(key);
        if(b == null)
            return defaultValue;
        return b;
    }

    /**
     * Attempts to read an integer value from the current configuration.
     * @param key The setting to retrieve from the configuration
     * @return an int value with the given parsed value, or null in the event the given setting key was not defined
     * or a non-integer value was defined in the configuration
     */
    public Integer getIntProperty(String key) {
        String prop = getProperty(key);
        if(prop == null)
            return null;
        try {
            return Integer.parseInt(prop);
        } catch(NumberFormatException e) {
            return null;
        }
    }

    /**
     * Reads an integer value from the current configuration, defaulting to a given value in the event the configuration
     * contains an invalid, non-integer value or the key does not exist
     * @param key The setting to retrieve
     * @param defaultValue The value to default to
     * @return the default value in the event the key didn't exist or the setting was a non-integer value, otherwise
     * returns the integer value of the given setting
     */
    public int getIntProperty(String key, int defaultValue) {
        Integer i = getIntProperty(key);
        if(i == null)
            return defaultValue;
        return i;
    }

    /**
     * Adds an integer value to the configuration
     * @param key The setting to add
     * @param i The value to add
     */
    public void putIntProperty(String key, int i) {
        put(key, Integer.toString(i));
    }

    /**
     * Adds a boolean value to the configuration
     * @param key The setting to add
     * @param b The value to add
     */
    public void putBooleanProperty(String key, boolean b) {
        put(key, Boolean.toString(b));
    }

}

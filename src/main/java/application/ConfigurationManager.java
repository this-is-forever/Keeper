package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationManager extends Properties {

    private File file;

    public ConfigurationManager(File f) {
        file = f;
    }

    public void load() {
        if(!file.exists()) {
            System.out.println("Configuration file did not exist.");
            return;
        }
        try(FileInputStream in = new FileInputStream(file)) {
            load(in);
        } catch (IOException e) {
            System.out.println("Error reading config file.");
        }
    }

    public void store() {
        try(FileOutputStream out = new FileOutputStream(file)) {
            store(out, "Application settings");
        } catch(IOException e) {
            e.printStackTrace();
            System.out.println("Unable to save configuration.");
        }
    }

    public Boolean getBooleanProperty(String key) {
        String prop = getProperty(key);
        if(prop == null)
            return null;
        return Boolean.parseBoolean(prop);
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        Boolean b = getBooleanProperty(key);
        if(b == null)
            return defaultValue;
        return b;
    }

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

    public int getIntProperty(String key, int defaultValue) {
        Integer i = getIntProperty(key);
        if(i == null)
            return defaultValue;
        return i;
    }

    public void putIntProperty(String key, int i) {
        put(key, Integer.toString(i));
    }

    public void putBooleanProperty(String key, boolean b) {
        put(key, Boolean.toString(b));
    }

}

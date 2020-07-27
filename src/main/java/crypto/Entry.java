package crypto;

import java.util.Arrays;

public class Entry implements Comparable {

    private String website, username;

    private byte[] passwordData;

    public Entry(String website, String username, byte[] passwordData) {
        this.website = website;
        this.username = username;
        this.passwordData = passwordData;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public byte[] getPasswordData() {
        return passwordData;
    }

    public void setPassword(PasswordArchiveManager manager, char[] password) {
        if(password.length == 0)
            passwordData = null;
        else
            passwordData = manager.encryptPassword(password);
    }

    public String getWebsite() {
        return website;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword(PasswordArchiveManager manager) {
        if(passwordData == null)
            return "";
        return manager.decryptPassword(passwordData);
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof Entry) {
            Entry e = (Entry) o;
            int comp = website.compareTo(e.getWebsite());
            if(comp == 0)
                comp = username.compareTo(e.getUsername());
            return comp;
        }
        return 0;
    }
}

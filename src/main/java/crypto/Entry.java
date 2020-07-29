package crypto;

/**
 * Defines a password entry, with website, username and password information
 */
public class Entry implements Comparable<Entry> {

    // References the entry's website and username
    private String website, username;
    // References the entry's encrypted password data
    private byte[] passwordData;

    /**
     * Constructs a new {@link Entry} object with given website, username and encrypted password data
     * @param website
     * @param username
     * @param passwordData
     */
    public Entry(String website, String username, byte[] passwordData) {
        this.website = website;
        this.username = username;
        this.passwordData = passwordData;
    }

    /**
     * Changes the entry's website
     * @param website The new website
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * Gets this entry's website information
     * @return a {@link String} with the entry's website
     */
    public String getWebsite() {
        return website;
    }

    /**
     * Changes the entry's username
     * @param username The new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets this entry's username
     * @return a {@link String} with the entry's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Changes the entry's password to a new one, encrypting the new password in the process
     * @param manager A {@link PasswordArchiveManager} which will be used to encrypt the password
     * @param password the char[] containing the new password
     */
    public void setPassword(PasswordArchiveManager manager, char[] password) {
        if(password.length == 0) {
            passwordData = null;
        } else {
            passwordData = manager.encryptPassword(password);
        }
    }

    /**
     * Retrieves this entry's encrypted password data
     * @return a byte[] with the encrypted password data
     */
    public byte[] getPasswordData() {
        return passwordData;
    }

    /**
     * Decrypts this entry's password and returns a reference
     * @param manager A {@link PasswordArchiveManager} to be used for decryption
     * @return a {@link String} object containing the decrypted password, or an empty {@link String} if this
     * entry does not have any password data
     */
    public String getPassword(PasswordArchiveManager manager) {
        if(passwordData == null) {
            return "";
        }
        return manager.decryptPassword(passwordData);
    }

    /**
     * Compares one {@link Entry} object to another, first by their website and then their username
     * @param e The object to compare to
     * @return an integer value that can be used to determine whether this {@link Entry} object comes before,
     * after or is equal to another
     */
    @Override
    public int compareTo(Entry e) {
        int comp = website.compareTo(e.getWebsite());
        if(comp == 0) {
            comp = username.compareTo(e.getUsername());
        }
        return comp;
    }
}

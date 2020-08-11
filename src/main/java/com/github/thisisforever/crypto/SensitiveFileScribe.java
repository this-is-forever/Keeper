package com.github.thisisforever.crypto;

import java.io.*;

public class SensitiveFileScribe {

    private Cryptographer cryptographer;

    public SensitiveFileScribe(Cryptographer c) {
        this.cryptographer = c;
    }

    public void encryptAndWrite(File f, byte[] data) throws IOException {
        try(FileOutputStream out = new FileOutputStream(f)) {
            out.write(cryptographer.encrypt(data));
        }
    }

    public byte[] readAndDecrypt(File f) throws IOException, CryptographicFailureException {
        try(FileInputStream in = new FileInputStream(f)) {
            return cryptographer.decrypt(in.readAllBytes());
        }
    }

    public void destroy() {
        cryptographer.destroy();
        cryptographer = null;
    }

}

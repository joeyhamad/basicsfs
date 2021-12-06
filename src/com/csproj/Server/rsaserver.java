
package com.csproj.Server;

import javax.crypto.Cipher;
import java.io.*;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


public class rsaserver {

    private PrivateKey privateKey;
    private PublicKey publicKey;

    private String PRIVATE_KEY_STRING = "";
    private String PUBLIC_KEY_STRING = "";
    private static final String pvtKeyPath = System.getProperty("user.dir") + "/private.pem";
    private static final String pubKeyPath = System.getProperty("user.dir") + "/public.pub";
    private static final String known_hosts_path = System.getProperty("user.dir") + "/known_hosts";


    public rsaserver() throws IOException {
        init();
    }

    public void rsaPublicKeyExchange(String clientIP, int clientPort) throws IOException {
        int port = clientPort;
        String clientPublicKey = "";

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
        Socket server = serverSocket.accept();
        System.out.println("Just connected to " + server.getRemoteSocketAddress());

        String known_hosts_file = readFileContents(known_hosts_path);
        OutputStream outToClient = server.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToClient);
        DataInputStream in = new DataInputStream(server.getInputStream());

        // First the client will tell me if it needs my public key.

        String clientResp = in.readUTF();
        System.out.println("From Client: " + clientResp);
        if (clientResp.contains("SEND PUBLIC KEY")) {
            out.writeUTF(PUBLIC_KEY_STRING);
        }

        // Do I need the client's host key?
        if (known_hosts_file.length() < 3) {
            // I need server's public key
            out.writeUTF("SEND PUBLIC KEY");
            System.out.println("Requested public key from the client");

            // Now server will send me their public key
            clientResp = in.readUTF();
            System.out.println("From Client: Public Key = " + clientResp);
            writeKnownHosts(clientIP, clientResp);
        } else {
            // Tell server not to send me the public key
            out.writeUTF("I DONT NEED YOUR PUBLIC KEY");
        }
        in.close();
        out.close();
        server.close();
        serverSocket.close();
    }

    public void init() throws IOException {
        // Read key file contents

        // Read private and public key files
        String pvtKeyContents = readFileContents(pvtKeyPath);
        String pubKeyContents = readFileContents(pubKeyPath);
        boolean writePvtKey = false;
        boolean writePubKey = false;

        // Instantiate keys if none of them are found. If they are found, assign the strings. If they are not found, generate the keys.
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            KeyPair pair = generator.generateKeyPair();

            if (pvtKeyContents.length() < 3) {
                this.privateKey = pair.getPrivate();
                String pKeyString = encode(privateKey.getEncoded());
                System.err.println("Private key\n" + pKeyString);
                PRIVATE_KEY_STRING = pKeyString;
                writePvtKey = true;

            } else {
                PRIVATE_KEY_STRING = pvtKeyContents;
            }

            if (pubKeyContents.length() < 3) {
                this.publicKey = pair.getPublic();
                String pKeyString = encode(publicKey.getEncoded());
                System.err.println("Public key\n" + pKeyString);
                PUBLIC_KEY_STRING = pKeyString;
                writePubKey = true;
            } else {
                PUBLIC_KEY_STRING = pubKeyContents;
            }

            writeKeys(writePvtKey, writePubKey);
        } catch (Exception ignored) {
        }
    }

    public void initFromStrings() {
        try {
            //X509EncodedKeySpec keySpecPublic = new X509EncodedKeySpec(decode(PUBLIC_KEY_STRING));
            PKCS8EncodedKeySpec keySpecPrivate = new PKCS8EncodedKeySpec(decode(PRIVATE_KEY_STRING));

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            // publicKey = keyFactory.generatePublic(keySpecPublic);
            privateKey = keyFactory.generatePrivate(keySpecPrivate);
        } catch (Exception ignored) {
        }
    }

    public void writeKnownHosts(String IP, String publicKey) throws IOException {
        String known_hosts_path = System.getProperty("user.dir") + "/known_hosts";
        String stringToWrite = IP + " " + publicKey + "\n";

        FileOutputStream os = new FileOutputStream(known_hosts_path);
        byte[] strToBytes = stringToWrite.getBytes();
        os.write(strToBytes);
        os.close();
    }

    public void writeKeys(boolean writePvtKey, boolean writePublicKey) throws IOException {
        String pvtKeyFile = System.getProperty("user.dir") + "/private.pem";
        String pubKeyFile = System.getProperty("user.dir") + "/public.pub";

        if (writePvtKey) {
            FileOutputStream os = new FileOutputStream(pvtKeyFile);
            byte[] strToBytes = PRIVATE_KEY_STRING.getBytes();
            os.write(strToBytes);
            os.close();
        }
        if (writePublicKey) {
            FileOutputStream os = new FileOutputStream(pubKeyFile);
            byte[] strToBytes = PUBLIC_KEY_STRING.getBytes();
            os.write(strToBytes);
            os.close();
        }
    }

    public String readFileContents(String filePath) throws IOException {
        File file = new File(filePath);
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line = "";
        String fileContents = "";

        while ((line = br.readLine()) != null) {
            fileContents += line + "\n";
        }

        br.close();
        fr.close();

        return fileContents;
    }

    public String encrypt(String message) throws Exception {
        byte[] messageToBytes = message.getBytes();
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(messageToBytes);
        return encode(encryptedBytes);
    }

    private static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private static byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    public String decrypt(String encryptedMessage) throws Exception {
        byte[] encryptedBytes = decode(encryptedMessage);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedMessage = cipher.doFinal(encryptedBytes);
        return new String(decryptedMessage, "UTF8");
    }

    public static void main(String[] args) {

    }
}
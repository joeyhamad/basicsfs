package com.csproj.Client;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class validator {


    private PrivateKey myPrivateKey;
    private PublicKey myPublicKey;
    private PublicKey serverPublicKey;
    public boolean isConnected = false;

    private static final String known_hosts_path = System.getProperty("user.dir") + "/known_hosts";
    private static final String private_key_path = System.getProperty("user.dir") + "/private.pem";
    private static final String public_key_path = System.getProperty("user.dir") + "/public.pub";
    static BigInteger currentChallenge;

    public void challengeExchange(BigInteger challenge, String serverIP, int serverPort) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InterruptedException {
        String serverName = serverIP;
        int port = serverPort;
        Socket client;

        currentChallenge = challenge;

        // Established the connection
        try {
            System.out.println("Connecting to " + serverName
                    + " on port " + port);
            client = new Socket(serverName, port);
            System.out.println("Just connected to "
                    + client.getRemoteSocketAddress());
            this.isConnected = true;
        } catch (ConnectException e) {
            throw new ConnectException("Please ensure that server is running on port 8808 and retry the connection.");
        }

        // Read host file for server's public key and initiate data streams
        String known_hosts_entry = readSpecificLine(known_hosts_path, serverIP);

        OutputStream outToServer = client.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);
        DataInputStream in = new DataInputStream(client.getInputStream());
        BufferedInputStream bin = new BufferedInputStream(client.getInputStream());

        String publicKeyString = known_hosts_entry.split(" ")[1];
        System.out.println("You found the public key: " + publicKeyString);
        initPublicKeyFromString(publicKeyString);


        System.out.println("Generated challenge is " + challenge.toString());

        // Send a challenge to the server using its public key
        out.writeUTF(challenge.toString());

        // Server replies with challenge encrypted using their private key
        int bufferLength = in.readInt();
        byte[] serverResp = new byte[bufferLength];
        bin.readNBytes(serverResp, 0, bufferLength);

        System.out.println("Size is " + serverResp.length);
        System.out.println("I'm receiving: " + new String(serverResp));

        Cipher cipher = Cipher.getInstance("RSA"); //setting algo to RSA
        cipher.init(Cipher.DECRYPT_MODE, serverPublicKey); // Decrypt using server's public key

        byte[] decryptedData = cipher.doFinal(serverResp);   // Decrypting using server public key

        String finalDecryptedChallenge = new String(decryptedData, StandardCharsets.UTF_8);

        System.out.println("Received challenge is: " + finalDecryptedChallenge);

        if(challenge.toString().compareTo(finalDecryptedChallenge) == 0){
            System.out.println("Server challenge successful");
        } else{
            System.err.println("SERVER CHALLENGE F A I L E D! SOMEONE MAY BE ALTERING THE CONNECTION!");
        }

        in.close();
        out.close();
        client.close();
        this.isConnected = false;
    }

    public void sendTimestamp(DataOutputStream dos, String serverIP) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InterruptedException {

        // Read host file for server's public key and initiate data streams
        String known_hosts_entry = readSpecificLine(known_hosts_path, serverIP);
        String PRIVATE_KEY_STRING = readLine(private_key_path);

        String publicKeyString = known_hosts_entry.split(" ")[1];
        System.out.println("You found the public key: " + publicKeyString);
        initPublicKeyFromString(publicKeyString);
        initPrviateKeyFromString(PRIVATE_KEY_STRING);

        SimpleDateFormat date = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss");
        String timeStamp = date.format(new Date());
        System.out.println("Generated timestamp is " + timeStamp);

        Cipher cipher = Cipher.getInstance("RSA"); //setting algo to RSA
        cipher.init(Cipher.ENCRYPT_MODE, myPrivateKey);
        byte[] encryptedTimestamp = cipher.doFinal(timeStamp.getBytes());

        dos.writeInt(encryptedTimestamp.length);
        System.out.println("Tiemstamp length is " + encryptedTimestamp.length);
        dos.write(encryptedTimestamp);
    }

    public String readSpecificLine(String filePath, String lineOfString) throws IOException {
        File file = new File(filePath);
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line = "";
        String fileContents = "";

        while ((line = br.readLine()) != null) {
            if(line.contains(lineOfString)){
                fileContents += line;
            }
        }

        br.close();
        fr.close();

        return fileContents;
    }

    public String readLine(String filePath) throws IOException {
        byte[] line = Files.readAllBytes(Paths.get(filePath));
        String keyDataString = new String(line, StandardCharsets.UTF_8);
        return keyDataString;
    }

    private static byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    public void initPublicKeyFromString(String PUBLIC_KEY_STRING) {
        try {
            X509EncodedKeySpec keySpecPublic = new X509EncodedKeySpec(decode(PUBLIC_KEY_STRING));
//            PKCS8EncodedKeySpec keySpecPrivate = new PKCS8EncodedKeySpec(decode(PRIVATE_KEY_STRING));

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

             serverPublicKey = keyFactory.generatePublic(keySpecPublic);
//            myPrivateKey = keyFactory.generatePrivate(keySpecPrivate);
        } catch (Exception ignored) {
        }
    }

    public void initPrviateKeyFromString(String PRIVATE_KEY_STRING) {
        try {
//            X509EncodedKeySpec keySpecPublic = new X509EncodedKeySpec(decode(PUBLIC_KEY_STRING));
            PKCS8EncodedKeySpec keySpecPrivate = new PKCS8EncodedKeySpec(decode(PRIVATE_KEY_STRING));

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

//            serverPublicKey = keyFactory.generatePublic(keySpecPublic);*/
            myPrivateKey = keyFactory.generatePrivate(keySpecPrivate);
        } catch (Exception ignored) {
        }
    }
}

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
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;

public class validator {


    private PrivateKey myPrivateKey;
    private PublicKey myPublicKey;
    private PublicKey serverPublicKey;
    public boolean isConnected = false;

    private static final String known_hosts_path = System.getProperty("user.dir") + "/known_hosts";


    public void challengeExchange(String serverIP, int serverPort) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InterruptedException {
        String serverName = serverIP;
        int port = serverPort;
        Socket client;


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
        initFromStrings(publicKeyString);

        BigInteger challenge = BigInteger.probablePrime(128, new Random());
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

    private static byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    public void initFromStrings(String PUBLIC_KEY_STRING) {
        try {
            X509EncodedKeySpec keySpecPublic = new X509EncodedKeySpec(decode(PUBLIC_KEY_STRING));
//            PKCS8EncodedKeySpec keySpecPrivate = new PKCS8EncodedKeySpec(decode(PRIVATE_KEY_STRING));

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

             serverPublicKey = keyFactory.generatePublic(keySpecPublic);
//            myPrivateKey = keyFactory.generatePrivate(keySpecPrivate);
        } catch (Exception ignored) {
        }
    }
}

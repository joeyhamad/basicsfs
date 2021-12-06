package com.csproj.Server;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class validator {

    private PrivateKey myPrivateKey;
    private PublicKey myPublicKey;
    private PublicKey clientPublicKey;

    public void challengeExchange(String clientIP, int clientPort) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        int port = clientPort;

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
        Socket server = serverSocket.accept();
        System.out.println("Just connected to " + server.getRemoteSocketAddress());

        OutputStream outToClient = server.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToClient);
        DataInputStream in = new DataInputStream(server.getInputStream());

        serverutils Server = new serverutils();

        Cipher cipher = Cipher.getInstance("RSA"); //setting algo to RSA

        String publicKeyString = readLine(System.getProperty("user.dir") + "/public.pub");
        String privateKeyString = readLine(System.getProperty("user.dir") + "/private.pem");

        initFromStrings(privateKeyString, publicKeyString);

        cipher.init(Cipher.ENCRYPT_MODE, myPrivateKey); // telling to encrypt using private key

//        int challengelength = in.readInt();
//        byte[] challenge = new byte[challengelength];
//        in.readNBytes(challenge, 0, challengelength);
//        System.out.println("Size is " + challenge.length);

        String challenge = in.readUTF();

        System.out.println("I'm encrypting " + challenge);

        byte[] digitalSignature = cipher.doFinal(challenge.getBytes());
        System.out.println("Length of signature is " + digitalSignature.length);

        System.out.println("I'm sending " + new String(digitalSignature));

        //signing messageHash
        out.writeInt(digitalSignature.length);
        out.write(digitalSignature);

        out.close();
        in.close();
        server.close();
        serverSocket.close();


//        out.writeInt(digitalSignature.length);
//        // Send digital signature response
//        out.write(digitalSignature);

    }


    private static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public void initFromStrings(String PRIVATE_KEY_STRING, String PUBLIC_KEY_STRING) {
        try {

            //X509EncodedKeySpec keySpecPublic = new X509EncodedKeySpec(decode(PUBLIC_KEY_STRING));
            PKCS8EncodedKeySpec keySpecPrivate = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIVATE_KEY_STRING));

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            // publicKey = keyFactory.generatePublic(keySpecPublic);
            myPrivateKey = keyFactory.generatePrivate(keySpecPrivate);
            System.out.println("Generating private key");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public String readLine(String filePath) throws IOException {
        byte[] line = Files.readAllBytes(Paths.get(filePath));
        String keyDataString = new String(line, StandardCharsets.UTF_8);
        return keyDataString;
    }
}

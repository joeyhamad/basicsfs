package com.csproj.Server;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Random;

public class validator {

    private PrivateKey myPrivateKey;
    private PublicKey myPublicKey;
    private PublicKey clientPublicKey;

    private static final String known_hosts_path = System.getProperty("user.dir") + "/known_hosts";
    private static final String private_key_path = System.getProperty("user.dir") + "/private.pem";
    private static final String public_key_path = System.getProperty("user.dir") + "/public.pub";

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

    public void receiveTimestamp( DataInputStream dis, String clientIP) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InterruptedException, ParseException {

        // Read host file for server's public key and initiate data streams
        String known_hosts_entry = readSpecificLine(known_hosts_path, clientIP);

        String publicKeyString = known_hosts_entry.split(" ")[1];
        System.out.println("You found the client public key: " + publicKeyString);
        initPublicKeyFromString(publicKeyString);

        int datalength = dis.readInt();
        byte[] encryptedTimestamp = dis.readNBytes(datalength);

        Cipher cipher = Cipher.getInstance("RSA"); //setting algo to RSA
        cipher.init(Cipher.DECRYPT_MODE, clientPublicKey);

        System.out.println("Trying to decrypt timestamp of length " + encryptedTimestamp.length);
        byte[] decryptedTimestamp = cipher.doFinal(encryptedTimestamp);

        String timestamp = new String(decryptedTimestamp);

        System.out.println("I got a timestamp! " + timestamp);

        SimpleDateFormat date = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss");
        String myTimestamp = date.format(new Date());
        System.out.println("My generated timestamp is " + myTimestamp);

        Date firstParsedDate = date.parse(timestamp);
        Date secondParsedDate = date.parse(myTimestamp);

        long diff = secondParsedDate.getTime() - firstParsedDate.getTime();

        System.out.println("Difference in timestamps is " + diff);

        if(diff > 5000){
            System.err.println("Received timestamp is off, someone could be tampering with the connection....");
        } else{
            System.out.println("Timestamps are good! Continue with downloads.");
        }

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

    public void initPublicKeyFromString(String PUBLIC_KEY_STRING) {
        try {
            X509EncodedKeySpec keySpecPublic = new X509EncodedKeySpec(decode(PUBLIC_KEY_STRING));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            clientPublicKey = keyFactory.generatePublic(keySpecPublic);
        } catch (Exception ignored) {
        }
    }

    public void initPrviateKeyFromString(String PRIVATE_KEY_STRING) {
        try {
            PKCS8EncodedKeySpec keySpecPrivate = new PKCS8EncodedKeySpec(decode(PRIVATE_KEY_STRING));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            myPrivateKey = keyFactory.generatePrivate(keySpecPrivate);
        } catch (Exception ignored) {
        }
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

    public String readLine(String filePath) throws IOException {
        byte[] line = Files.readAllBytes(Paths.get(filePath));
        String keyDataString = new String(line, StandardCharsets.UTF_8);
        return keyDataString;
    }
}

package com.csproj.Client;

import java.math.BigInteger;
import java.net.*;
import java.io.*;
import java.util.Random;

public class dhclient {

    public static String initConnection()
    {
        try {
            String serverName = "localhost";
            int port = 8088;
            int keyBitLength = 128;

            BigInteger p = BigInteger.probablePrime(keyBitLength, new Random());
            BigInteger g = BigInteger.probablePrime(keyBitLength, new Random());
            BigInteger privateKey = BigInteger.probablePrime(keyBitLength, new Random());

            String serverB;
            BigInteger sharedSecretKey;

            // Established the connection
            System.out.println("Connecting to " + serverName
                    + " on port " + port);
            Socket client = new Socket(serverName, port);
            System.out.println("Just connected to "
                    + client.getRemoteSocketAddress());

            // Sends the data to client
            OutputStream outToServer = client.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);

            out.writeUTF(p.toString());

            out.writeUTF(g.toString());

            BigInteger clientPublicKey = g.modPow(privateKey, p);
            out.writeUTF(clientPublicKey.toString());

            System.out.println("Sent values: P=" + p + " G=" + g + " publickey=" + clientPublicKey);
            System.out.println("Bit lengths of P, G, and private key are " + p.bitLength() + ", " + g.bitLength() + ", " + privateKey.bitLength());

            // Accepts the data
            DataInputStream in = new DataInputStream(client.getInputStream());

            serverB = in.readUTF();

            BigInteger BServerB = new BigInteger(serverB);
            System.out.println("From Server: Public Key = " + BServerB);

            sharedSecretKey = BServerB.modPow(privateKey, p);

            System.out.println("Secret Key to perform Symmetric Encryption = "
                    + sharedSecretKey);
            client.close();

            return sharedSecretKey.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return "empty";
        }
    }
}

package com.csproj.Server;
import java.math.BigInteger;
import java.net.*;
import java.io.*;
import java.util.Random;

public class dhserver {
    public static String initConnection(serverutils Server) throws IOException
    {
        try {
            int port = 8088;
            int keyBitLength = 128;

            BigInteger b = BigInteger.probablePrime(keyBitLength, new Random());
            System.out.println("Bit length of b is " + b.bitLength());

            // Client p, g, and key
            String clientP, clientG, clientA;
            BigInteger serverPublicKey, sharedSecretKey;

            // Established the Connection
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
            Socket server = serverSocket.accept();
            System.out.println("Just connected to " + server.getRemoteSocketAddress());

            // Server's Private Key
            System.out.println("My private key = " + b);

            // Accepts the data from client
            DataInputStream in = new DataInputStream(server.getInputStream());

            clientP = in.readUTF();
            clientG = in.readUTF();
            clientA = in.readUTF();

            BigInteger bClientP = new BigInteger(clientP);
            BigInteger bClientG = new BigInteger(clientG);
            BigInteger clientPublicKey = new BigInteger(clientA);
            System.out.println("I received values from client P=" + bClientP + " G=" + bClientG + " clientPublicKey=" + clientPublicKey);

            serverPublicKey = (bClientG.modPow(b, bClientP));

            OutputStream outToclient = server.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToclient);

            System.out.println("Sending server public key: " + serverPublicKey);
            out.writeUTF(serverPublicKey.toString()); // Sending server public key

            sharedSecretKey = clientPublicKey.modPow(b, bClientP);

            System.out.println("Secret Key to perform Symmetric Encryption = "
                    + sharedSecretKey);
            server.close();
            serverSocket.close();
            Server.secretKeyBytes = sharedSecretKey.toByteArray();
            return sharedSecretKey.toString();
        }

        catch (SocketTimeoutException s) {
            System.out.println("Socket timed out!");
            return "empty";
        }
        catch (IOException e) {
            System.out.println("Something bad happened!");
            return "empty";
        }
    }
}

package com.csproj.Client;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.ConnectException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class main {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InterruptedException, ConnectException {

        String optionSelect = "3";

        while (!optionSelect.equals("1") && !optionSelect.equals("2")) {
            System.out.println("""
                    Select mode of operation:
                    1. Send files in ./toServer
                    2. Retrieve stored files in server and save in ./fromServer
                    """);

            Scanner scanner = new Scanner(System.in);
            optionSelect = scanner.nextLine();
        }

        switch (optionSelect) {
            case "1":
                System.out.println("Initiating file transfer of files in ./toServer to the server.");
                break;
            case "2":
                System.out.println("Transferring files from server to ./fromServer directory");
                break;
            default:
                System.out.println("No valid options selected. Terminating program...");
                return;
        }

        // Test RSA first
        int j = 1;
        rsaclient rsaclient = new rsaclient();
        rsaclient.rsaPublicKeyExchange("localhost", 8808);
//        while (!rsaclient.isConnected && j <= 10) {
//            try {
//                System.out.println("Connection attempt #" + j + "/10");
//                rsaclient.rsaPublicKeyExchange("localhost", 8808);
//                Thread.sleep(5000);
//                j++;
//            } catch (Exception e) {
//                System.out.println("Server not found...trying again...Attempt #" + j + "/10");
//                j++;
//                Thread.sleep(5000);
//            }
//        }
//
//        if (j > 10){
//            throw new ConnectException("Aborting program, no connection established.");
//        }

        String secretKeyClient = "empty";
        dhclient clientDH = new dhclient();
        j = 1;
        while (!clientDH.isConnected && j <= 10) {
            try {
                System.out.println("Connection attempt #" + j + "/10");
                secretKeyClient = clientDH.initConnection();
                Thread.sleep(5000);
                j++;
            } catch (Exception e) {
                System.out.println("Server not found...trying again...Attempt #" + j + "/10");
                Thread.sleep(5000);
            }
        }

        if (j > 10){
            throw new ConnectException("Aborting program, no connection established.");
        }

        String homedir = System.getProperty("user.home");
        String srcfile = homedir + "/Documents/testdocs/testdoc";
        String testdocuments = System.getProperty("user.dir") + "/testdocs/";
        String srcfolder = System.getProperty("user.dir") + "/toServer/";
        String destfolder = System.getProperty("user.dir") + "/fromServer/";

        // Find file to test
        File testdir = new File(testdocuments);
        String[] filepaths = testdir.list();

        for (String filename : filepaths) {
            System.out.println("Working on file: " + filename);
            File testdoc = new File(testdocuments + filename);
            String outputEncryptedfile = filename + "_encrypted";
            String outputDecryptedfile = filename + "_decrypted";

            RandomAccessFile tr = new RandomAccessFile(testdoc, "rw");

            // Create client instance
            clientutils Client = new clientutils();
            BigInteger sharedSecret = new BigInteger(secretKeyClient);
            Client.sharedKey = sharedSecret.toByteArray();

            // Read test file to byte array
            byte[] testarray = Client.readFileToByteArray(tr);

            // Pad array with zeroes to have a whole number amount of 32 byte blocks
            int paddedArrayLength = testarray.length + (32 - (testarray.length % 32));
            byte[] paddedArray = new byte[paddedArrayLength];
            // pad zeros
            System.arraycopy(testarray, 0, paddedArray, 0, testarray.length);

            // Debug - show some useful information
            System.out.println("Data byte array length: " + testarray.length);
            System.out.println("Padded data array length: " + paddedArray.length);
            testArrayCopyMechanism(Client, paddedArray);

            // Encrypt the byte array and read the encrypted array to a file
            byte[] encryptedBytes = Client.encryptByteArray(paddedArray);
            Client.readByteArrayToFile(encryptedBytes, srcfolder + outputEncryptedfile);

            // Decrypt the file
            byte[] decryptedBytes = Client.decryptByteArray(encryptedBytes, Client.sharedKey, Client.initVectorHash);

            // Remove padded bytes to match original file
            byte[] finalArray = new byte[testarray.length];
            System.arraycopy(decryptedBytes, 0, finalArray, 0, testarray.length);

            // Check if original and new byte array is equal
            System.out.println("Are original and decrypted file equal?:  " + compareByteArrays(testarray, finalArray));

            // Read bytes to new file
            Client.readByteArrayToFile(finalArray, destfolder + outputDecryptedfile);

        }

    }

    public static void testArrayCopyMechanism(clientutils Client, byte[] arrayToTest) {
        ArrayList<byte[]> splitByteArrayList = Client.splitByteArrayToArrayList(arrayToTest);
        byte[] joinedArray = Client.joinArrayListToByteArray(splitByteArrayList);

        System.out.println("Joined data array length: " + joinedArray.length);

        if (Arrays.equals(arrayToTest, joinedArray)) {
            System.out.println("Arrays are the same...");
        } else {
            System.out.println("Hmmm....they're not the same...");
        }
    }

    public static boolean compareByteArrays(byte[] file1, byte[] file2) throws NoSuchAlgorithmException {
        return Arrays.equals(file1, file2);
    }
}


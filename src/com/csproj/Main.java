package com.csproj;

import com.csproj.Client.clientutils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;


public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String homedir = System.getProperty("user.home");
	    String srcfile = homedir + "/Documents/testdocs/testdoc";
        String outputEncryptedfile = homedir + "/Documents/testdocs/encryptedDoc";
        String outputDecryptedfile = homedir + "/Documents/testdocs/decryptedDoc";

        // Find file to test
        File testdoc = new File(srcfile);
        RandomAccessFile tr = new RandomAccessFile(testdoc, "rw");

        // Create client instance
        clientutils Client = new clientutils();

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
        Client.readByteArrayToFile(encryptedBytes, outputEncryptedfile);

        // Decrypt the file
        byte[] decryptedBytes = Client.decryptByteArray(encryptedBytes, Client.sharedKey, Client.initVectorHash);

        // Remove padded bytes to match original file
        byte[] finalArray = new byte[testarray.length];
        System.arraycopy(decryptedBytes, 0, finalArray, 0, testarray.length);

        // Check if original and new byte array is equal
        System.out.println("Are original and decrypted file equal?:  "  + compareByteArrays(testarray, finalArray));

        // Read bytes to new file
        Client.readByteArrayToFile(finalArray, outputDecryptedfile);

    }

    public static void testArrayCopyMechanism(clientutils Client, byte[] arrayToTest){
        ArrayList<byte[]> splitByteArrayList = Client.splitByteArrayToArrayList(arrayToTest);
        byte[] joinedArray = Client.joinArrayListToByteArray(splitByteArrayList);

        System.out.println("Joined data array length: " + joinedArray.length);

        if(Arrays.equals(arrayToTest, joinedArray)){
            System.out.println("Arrays are the same...");
        } else {
            System.out.println("Hmmm....they're not the same...");
        }
    }

    public static boolean compareByteArrays(byte[] file1, byte[] file2) throws NoSuchAlgorithmException {
        return Arrays.equals(file1, file2);
    }
}
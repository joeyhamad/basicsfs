package com.csproj.Client;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.awt.*;
import java.util.Arrays;

public class FilesServer {

    static int id = 0;
    static int num = 0;
    static String hashlistpath = System.getProperty("user.dir") + "/hashes.list";


    public static void listfiles(ArrayList<String> fileNames, DataInputStream dis, DataOutputStream dos, clientutils Client) throws InterruptedException {

        String str = "";

        JFrame jList = new JFrame();
        jList.setSize(500, 500);
        jList.setLayout(new BoxLayout(jList.getContentPane(), BoxLayout.Y_AXIS));
        jList.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));

        JScrollPane jScroll = new JScrollPane(jPanel);
        jScroll.setVerticalScrollBarPolicy(jScroll.VERTICAL_SCROLLBAR_ALWAYS);

        JLabel jTitle = new JLabel("Files On the Server");
        jTitle.setFont(new Font("Arial", Font.BOLD, 25));
        jTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        jList.add(jTitle);
        jList.add(jScroll);
        jList.setVisible(true);

        JPanel jpFileRow = new JPanel();
        jpFileRow.setLayout(new BoxLayout(jpFileRow, BoxLayout.Y_AXIS));

        if(fileNames.size() == 0){
            String name = "No files....closing window.";
            JLabel jFileName = new JLabel(name);
            jFileName.setFont(new Font("Arial", Font.BOLD, 20));

            jpFileRow.setName("- ");
            jpFileRow.add(jFileName);
            jPanel.add(jpFileRow);
            jList.validate();

            Thread.sleep(5000);
            jList.dispose();
            return;
        }

        int fileId = 0;
        for (int i = 0; i < fileNames.size(); i++) {
            String name = (i + 1) + ". " + fileNames.get(i);
            JLabel jFileName = new JLabel(name);
            jFileName.setFont(new Font("Arial", Font.BOLD, 20));

            jpFileRow.setName(String.valueOf(fileId++));

            jpFileRow.add(jFileName);
            jPanel.add(jpFileRow);
        }

        JTextArea jTextArea = new JTextArea();
        JButton button = new JButton("Get File");

        // This is the download listener
        button.addActionListener(e -> {
            try {
                num = Integer.parseInt(jTextArea.getText());
                System.out.println(num);
                String filename = fileNames.get(num-1);
                System.out.println("I want to download file: " + filename);
                dos.writeUTF(filename);

                String incomingFileName = dis.readUTF();

                int initVectorLength = dis.readInt();
                byte[] fileInitVector = dis.readNBytes(initVectorLength);

                int fileByteLength = dis.readInt();
                byte[] fileBytes = dis.readNBytes(fileByteLength);;

                Client.readByteArrayToFile(fileBytes, System.getProperty("user.dir") + "/fromServer/" + incomingFileName + "_encrypted");
                System.out.println("Saved file: " + filename);
                System.out.println("Length of encrypted array is " + fileBytes.length);

                // Decrypt the file
                byte[] decryptedBytes = Client.decryptByteArray(fileBytes, Client.sharedKey, fileInitVector);

                System.out.println("Length of decrypted array is " + decryptedBytes.length);

                int i;
                for(i = decryptedBytes.length-1; i >= 0; i--){
                    if(decryptedBytes[i] != 0x00){
                        break;
                    }
                }

                byte[] unpaddedArray = Arrays.copyOfRange(decryptedBytes, 0, i+1);

                // Remove padded bytes to match original file
                byte[] finalArray = new byte[unpaddedArray.length];
                System.arraycopy(decryptedBytes, 0, finalArray, 0, unpaddedArray.length);

                // Check if original and new byte array is equal
                System.out.println("Are original and decrypted file equal?:  " + compareByteArrays(unpaddedArray, finalArray));

                // Verify that decrypted file hash matches original file hash
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                String hashlist = Client.readFileContents(hashlistpath);
                String decryptedFileHash = String.format("%032X", new BigInteger(1, sha.digest(finalArray)));

                System.out.println("HASH AFTER: "+ decryptedFileHash);

                String outputDecryptedfile = filename;
                String destfolder = System.getProperty("user.dir") + "/decrypted/";

                System.out.println("Size of final array is " + finalArray.length);

                if(hashlist.contains(decryptedFileHash)){
                    System.out.println("Hey, I recognize this file from somewhere!");
                    // Read bytes to new file
                    Client.readByteArrayToFile(finalArray, destfolder + outputDecryptedfile);
                } else{
                    System.out.println("Looks like we received some bad data....I won't save this file for you.");
                }

                fileNames.clear();
                jList.dispose();

            } catch(Exception ex){
                ex.printStackTrace();
            }
        });

        System.out.println("Came out of actionlistener");
        jpFileRow.add(jTextArea);
        jpFileRow.add(button);

        jList.validate();

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
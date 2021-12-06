package com.csproj.Server;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;


public class serverWindow {
    static ArrayList<MyFile> myFiles = new ArrayList<>();
    static String hashlistpath = System.getProperty("user.dir") + "/hashes.list";
    static String destfolder = System.getProperty("user.dir") + "/fromClient/";
    static int fileId = 0;
    static String sharedSecret;


    public static void main(String[] args) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

        serverutils Server = new serverutils();
        rsaserver rsaserver = new rsaserver();
        rsaserver.rsaPublicKeyExchange("localhost", 8088);

        validator Validator = new validator();
        Validator.challengeExchange("localhost", 8088);

        dhserver serverDH = new dhserver();
        String secretkeyServer = serverDH.initConnection(Server);
        sharedSecret = secretkeyServer;

        JFrame jF = new JFrame("Network Security Project: Server");
        jF.setSize(500, 500);
        jF.setDefaultCloseOperation(jF.EXIT_ON_CLOSE);

        JPanel jP = new JPanel();
        jP.setLayout(new BoxLayout(jP, BoxLayout.Y_AXIS));

        JScrollPane jScrollPane = new JScrollPane(jP);
        jScrollPane.setVerticalScrollBarPolicy(jScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JLabel jTitle = new JLabel("Files On the Server");
        jTitle.setFont(new Font("Arial", Font.BOLD, 25));
        jTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        jF.add(jTitle);
        jF.add(jScrollPane);
        jF.setVisible(true);

        ServerSocket ss = new ServerSocket(8088);

        JTextArea outputTextbox = new JTextArea(40, 40);
        jF.add(outputTextbox);

        while (true) {
            try {
                Socket s = ss.accept();

                DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
                int numberOfFiles = dataInputStream.readInt();
                System.out.println("Read data from the server. I just read: " + numberOfFiles);

                switch (numberOfFiles) {
                    case 0: sendFilesToClient(s, dataInputStream, new DataOutputStream((s.getOutputStream())), Server); break;
                    default: downloadIncomingFiles(s, dataInputStream, jP, jF, numberOfFiles, Server); break;
                }

                dataInputStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public static void downloadIncomingFiles(Socket s, DataInputStream ds, JPanel jP, JFrame jF, int numberOfFiles, serverutils Server) throws IOException {
        ArrayList<String> listOfFileNames = downloadIncomingFiles(s, ds, numberOfFiles, Server);

        for (String fileName : listOfFileNames) {
            JPanel jpFileRow = new JPanel();
            jpFileRow.setLayout(new BoxLayout(jpFileRow, BoxLayout.Y_AXIS));

            JLabel jFileName = new JLabel(fileName);
            jFileName.setFont(new Font("Arial", Font.BOLD, 20));

            jpFileRow.setName(String.valueOf(fileId));

            jpFileRow.add(jFileName);
            jP.add(jpFileRow);
            jF.validate();
        }
    }

    public static void sendFilesToClient(Socket s, DataInputStream ds, DataOutputStream os, serverutils Server) throws IOException, NoSuchAlgorithmException {
        //System.out.println("Download case entered");
        File files[] = new File(destfolder).listFiles();

        ArrayList<String> fileNames = new ArrayList<>();
        ArrayList<File> fileArrayList = new ArrayList<>(Arrays.asList(files));

        for( File file : files){
            fileNames.add(file.getName());
        }

        os.writeInt(files.length);

        FileInputStream fileInputStream;

        for (int i = 0; i < files.length; i++) {
            os.writeUTF(files[i].getName());
        }

        // Wait for filename from the server
        String fileToDownload = ds.readUTF();
        int index = fileNames.indexOf(fileToDownload);
        System.out.print("The client wants to download: " + fileToDownload);

        fileInputStream = new FileInputStream(files[index]);

        String fileName = files[index].getName();

        byte[] fileContentBytes = new byte[(int) files[index].length()];

        fileInputStream.read(fileContentBytes);

        System.out.println("Length of file content is " + fileContentBytes.length);

        // Pad array with zeroes to have a whole number amount of 32 byte blocks
        int paddedArrayLength = fileContentBytes.length + (32 - (fileContentBytes.length % 32));
        byte[] paddedArray = new byte[paddedArrayLength];
        // pad zeros
        System.arraycopy(fileContentBytes, 0, paddedArray, 0, fileContentBytes.length);

        byte[] encryptedByesToSend = Server.encryptByteArray(paddedArray);

        System.out.println("Length of encrypted bytes is " + encryptedByesToSend.length);

        os.writeUTF(fileName);




        os.writeInt(Server.currentInitVector.length);
        os.write(Server.currentInitVector);

        os.writeInt(encryptedByesToSend.length);
        os.write(encryptedByesToSend);

        fileInputStream.close();
        files[index].delete();
    }


//    public boolean sendFilesToClient() {
//
//    }

    public static ArrayList<String> downloadIncomingFiles(Socket s, DataInputStream dataInputStream, int numberOfFilesToDownload, serverutils Server) throws IOException {
        System.out.println(numberOfFilesToDownload);
        ArrayList listOfFilenames = new ArrayList<String>();

        int i = 0;
        try {
            for (i = 0; i < numberOfFilesToDownload; i++) {

                String fileName = dataInputStream.readUTF();
                int byteLength = dataInputStream.readInt();
                byte[] fileContentBytes = dataInputStream.readNBytes(byteLength);

                int vectorLength = dataInputStream.readInt();
                byte[] initVectorHash = dataInputStream.readNBytes(vectorLength);

                listOfFilenames.add(fileName);
                System.out.println("Receiving file from client: " + fileName);

                byte[] decryptedBytes = Server.decryptByteArray(fileContentBytes, Server.secretKeyBytes, initVectorHash);

                int j;
                for(j = decryptedBytes.length-1; j >= 0; j--){
                    if(decryptedBytes[j] != 0x00){
                        break;
                    }
                }

                byte[] unpaddedArray = Arrays.copyOfRange(decryptedBytes, 0, j+1);

                // Remove padded bytes to match original file
                byte[] finalArray = new byte[unpaddedArray.length];
                System.arraycopy(decryptedBytes, 0, finalArray, 0, unpaddedArray.length);

                Server.readByteArrayToFile(finalArray, destfolder + fileName.substring(0, fileName.length()-10));

            }

        } catch (Exception e) {
            System.out.println("Error occured with file number " + i);
        }
        return listOfFilenames;
    }


    public static String getFileExtension(String fileName) {

        int i = fileName.lastIndexOf('.');

        if (i > 0) {
            return fileName.substring(i + 1);
        } else {
            return "No extension found";
        }
    }
}
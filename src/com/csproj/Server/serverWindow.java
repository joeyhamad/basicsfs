package com.csproj.Server;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

import javax.swing.*;


public class serverWindow {
    static ArrayList<MyFile> myFiles = new ArrayList<>();
    static String hashlistpath = System.getProperty("user.dir") + "/hashes.list";
    static String destfolder = System.getProperty("user.dir") + "/fromClient/";

    public static void main(String[] args) throws IOException {

        rsaserver rsaserver = new rsaserver();
        rsaserver.rsaPublicKeyExchange("localhost", 8088);

        dhserver serverDH = new dhserver();
        String secretkeyServer = serverDH.initConnection();

        int fileId = 0;

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

                if (numberOfFiles > 0) {
                    ArrayList<String> listOfFileNames = downloadIncomingFiles(s, numberOfFiles);

                    for(String fileName : listOfFileNames) {
                        JPanel jpFileRow = new JPanel();
                        jpFileRow.setLayout(new BoxLayout(jpFileRow, BoxLayout.Y_AXIS));

                        JLabel jFileName = new JLabel(fileName);
                        jFileName.setFont(new Font("Arial", Font.BOLD, 20));

                        jpFileRow.setName(String.valueOf(fileId));

                        jpFileRow.add(jFileName);
                        jP.add(jpFileRow);
                        jF.validate();

//                        myFiles.add(new MyFile(fileId, fileName, fileContentBytes, getFileExtension(fileName)));
//                        String fileServer = "/Users/manasakandala/UTD/Net Sec/Project/FTP Connection/Server/" + fileName;
//                        File fileDownload = new File(fileServer);
//                        FileOutputStream fileOutputStream = new FileOutputStream(fileDownload);
//                        fileOutputStream.write(fileContentBytes);
//                        fileOutputStream.close();
                    }

                } else {
                    //System.out.println("Download case entered");
                    File files[] = new File("/Users/manasakandala/UTD/Net Sec/Project/FTP Connection/Server/").listFiles();

                    DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());
                    dataOutputStream.writeInt(files.length);

                    // for(int i=0; i<files.length; i++) {
                    //     System.out.println(files[i]);
                    // }


                    for (int i = 0; i < files.length; i++) {
                        FileInputStream fileInputStream = new FileInputStream(files[i]);

                        String fileName = files[i].getName();
                        byte[] fileNameBytes = fileName.getBytes();

                        byte[] fileContentBytes = new byte[(int) files[i].length()];
                        fileInputStream.read(fileContentBytes);

                        dataOutputStream.writeInt(fileNameBytes.length);
                        dataOutputStream.write(fileNameBytes);

                        dataOutputStream.writeInt(fileContentBytes.length);
                        dataOutputStream.write(fileContentBytes);

                    }
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

//    public boolean sendFilesToClient() {
//
//    }

    public static ArrayList<String> downloadIncomingFiles(Socket s, int numberOfFilesToDownload) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
        System.out.println(numberOfFilesToDownload);
        serverutils Server = new serverutils();
        ArrayList listOfFilenames = new ArrayList<String>();

        int i = 0;
        try {
            for (i=0; i < numberOfFilesToDownload; i++) {

                String fileName = dataInputStream.readUTF();
                int byteLength = dataInputStream.readInt();
                byte[] fileContentBytes = dataInputStream.readNBytes(byteLength);

                listOfFilenames.add(fileName);
                System.out.println("Receiving file from client: " + fileName);

                Server.readByteArrayToFile(fileContentBytes, destfolder + fileName);

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



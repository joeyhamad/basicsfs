package com.csproj.Client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Arrays;


public class clientWindow {
    static ArrayList<String> fileNames = new ArrayList<>();
    static ArrayList<MyFile> myFiles = new ArrayList<>();

    static String testdocuments = System.getProperty("user.dir") + "/testdocs/";
    static String hashlistpath = System.getProperty("user.dir") + "/hashes.list";
    static String srcfolder = System.getProperty("user.dir") + "/toServer/";
    static String destfolder = System.getProperty("user.dir") + "/fromServer/";

    public static void main(String[] args) throws IOException, InterruptedException {
        rsaclient rsaclient = new rsaclient();
        rsaclient.rsaPublicKeyExchange("localhost", 8088);

        String secretKeyClient = "empty";
        dhclient clientDH = new dhclient();
        int j = 1;
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

        final String finalSecretkey = secretKeyClient;

        int fileId = 0;
        final File[] file = new File[1];

        JFrame jF = new JFrame();
        jF.setTitle("Network Security Project: Client");
        jF.setSize(500, 500);
        jF.setLayout(new BoxLayout(jF.getContentPane(), BoxLayout.Y_AXIS));
        jF.setDefaultCloseOperation(jF.EXIT_ON_CLOSE);

        JPanel jP = new JPanel();
        jP.setBorder(new EmptyBorder(5, 5, 5, 5));

        JButton jSelect = new JButton("Select File");
        jSelect.setPreferredSize(new Dimension(100, 75));

        JButton jSend = new JButton("Send");
        jSend.setPreferredSize(new Dimension(100, 75));

        JButton jDownload = new JButton("Download");
        jDownload.setPreferredSize(new Dimension(100, 75));


        jP.add(jSelect);
        jP.add(jSend);
        jP.add(jDownload);


        jDownload.addActionListener(new ActionListener() {

            // This is the download listener
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Tell the server that we are downloading, not uploading.
                    Socket s = new Socket("localhost", 8088);
                    DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());
                    dataOutputStream.writeInt(0);

                    // Server tells me how many files are there
                    DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
                    int fileCount = dataInputStream.readInt();

                    // Get the file names from the server
                    System.out.println("Came in download.addActionListener; Count: " + fileCount);
                    for (int i = 0; i < fileCount; i++) {
                        int fileNameLength = dataInputStream.readInt();
                        byte[] fileNameBytes = new byte[fileNameLength];
                        dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                        String fileName = new String(fileNameBytes);

                        System.out.println("Name: " + fileName);
                        fileNames.add(fileName);

                        // int fileContentLength = dataInputStream.readInt();
                        // byte[] fileContentBytes = new byte[fileContentLength];
                        // dataInputStream.readFully(fileContentBytes, 0, fileContentLength);

                        // JPanel jpFileRow = new JPanel();
                        // jpFileRow.setLayout(new BoxLayout(jpFileRow, BoxLayout.Y_AXIS));

                        // JLabel jFileName = new JLabel("<html><br>" + fileName + "<br></html>");
                        // jFileName.setFont(new Font("Arial", Font.BOLD, 20));

                        // jpFileRow.setName(String.valueOf(fileId));

                        // jpFileRow.add(jFileName);
                        // jP.add(jpFileRow);
                        // jF.validate();


                        // myFiles.add(new MyFile(fileId, fileName, fileContentBytes, getFileExtension(fileName)));
                        // String fileClient = "/Users/manasakandala/UTD/Net Sec/Project/FTP Connection/Client/" + fileName;
                        // File fileDownload = new File(fileClient);
                        // FileOutputStream fileOutputStream = new FileOutputStream(fileDownload);
                        // fileOutputStream.write(fileContentBytes);
                        // fileOutputStream.close();
                    }

                    //ArrayList testlist = new ArrayList<String>();
                    FilesServer filesServer = new FilesServer();
                    String file = filesServer.listfiles(fileNames);

                } catch (IOException error) {
                    error.printStackTrace();
                }

            }
        });

        jSelect.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser jFile = new JFileChooser();
                    jFile.setDialogTitle("Choose the file to send..");

                    if (jFile.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        file[0] = jFile.getSelectedFile();
                    }

                    MessageDigest sha = MessageDigest.getInstance("SHA-256");

                    // Create client instance
                    clientutils Client = new clientutils();

                    String hashlist = null;

                    hashlist = Client.readFileContents(hashlistpath);

                    System.out.println("Working on file: " + file[0].getName());
                    File testdoc = file[0];
                    String outputEncryptedfile = testdoc.getName() + "_encrypted";

                    RandomAccessFile tr = null;

                    tr = new RandomAccessFile(testdoc, "rw");

                    BigInteger sharedSecret = new BigInteger(finalSecretkey);
                    Client.sharedKey = sharedSecret.toByteArray();

                    // Read test file to byte array
                    byte[] testarray = Client.readFileToByteArray(tr);

                    byte[] testarrayhash = sha.digest(testarray);

                    String hashstr = String.format("%032X", new BigInteger(1, testarrayhash));
                    System.out.println("HASH BEFORE: " + hashstr);
                    if (!hashlist.contains(hashstr)) {
                        System.out.println("Hey look, a new file! Let's write its hash to the hash list.");
                        Client.writeToFile(System.getProperty("user.dir") + "/hashes.list", hashstr);
                    }

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

                } catch (Exception f) {

                }
            }
        });

        // This is the upload listener
        jSend.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    File toServer = new File(srcfolder);
                    String[] filepaths = toServer.list();

                    // Number of files to send
                    int numFilesToSend = filepaths.length;

                    clientutils Client = new clientutils();

                    Socket s = new Socket("localhost", 8088);
                    DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());

                    // Send the number of files to the server
                    dataOutputStream.writeInt(numFilesToSend);

                    for(String filename : filepaths) {
                        System.out.println("Working on file: " + filename);

                        //FileInputStream fileInputStream = new FileInputStream(file[0].getAbsolutePath());

                        File workingFile = new File(srcfolder + filename);
                        RandomAccessFile fileToSend = new RandomAccessFile(workingFile, "rw");

                        byte[] fileContentBytes = Client.readFileToByteArray(fileToSend);

                        dataOutputStream.writeUTF(filename);
                        dataOutputStream.writeInt(fileContentBytes.length);
                        dataOutputStream.write(fileContentBytes);

                        // todo
                        // Send file hash signed with private key. Server receives hash from client
                        // If it can't verify the hash client
                        // tries to send the file again
                        workingFile.delete();
                    }

                } catch (FileNotFoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (UnknownHostException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });
        jF.add(jP);
        jF.setVisible(true);

    }

    public static String getFileExtension(String fileName) {

        int i = fileName.lastIndexOf('.');

        if (i > 0) {
            return fileName.substring(i + 1);
        } else {
            return "No extension found";
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


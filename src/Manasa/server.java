package Manasa;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

import javax.swing.*;

public class server {
    static ArrayList<MyFile> myFiles = new ArrayList<>();

    public static void main(String[] args) throws IOException{
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

        ServerSocket ss = new ServerSocket(4999);

        while(true) {
            try {
                Socket s = ss.accept();

                DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
                int fileNameLength = dataInputStream.readInt();

                if (fileNameLength > 0) {
                    byte[] fileNameBytes = new byte[fileNameLength];
                    dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                    String fileName = new String(fileNameBytes);

                    int fileContentLength = dataInputStream.readInt();

                    if(fileContentLength > 0) {
                        byte[] fileContentBytes = new byte[fileContentLength];
                        dataInputStream.readFully(fileContentBytes, 0, fileContentLength); 

                        JPanel jpFileRow = new JPanel();
                        jpFileRow.setLayout(new BoxLayout(jpFileRow, BoxLayout.Y_AXIS));

                        JLabel jFileName = new JLabel(fileName);
                        jFileName.setFont(new Font("Arial", Font.BOLD, 20));

                        jpFileRow.setName(String.valueOf(fileId));

                        jpFileRow.add(jFileName);
                        jP.add(jpFileRow);
                        jF.validate();
                        
                        myFiles.add(new MyFile(fileId, fileName, fileContentBytes, getFileExtension(fileName)));
                        String fileServer = "/Users/manasakandala/UTD/Net Sec/Project/FTP Connection/Server/" + fileName; 
                        File fileDownload = new File(fileServer);
                        FileOutputStream fileOutputStream = new FileOutputStream(fileDownload);
                        fileOutputStream.write(fileContentBytes);
                        fileOutputStream.close(); 


                    }
                }
                else {
                    //System.out.println("Download case entered");
                    File files[] = new File("/Users/manasakandala/UTD/Net Sec/Project/FTP Connection/Server/").listFiles();

                    DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());
		            dataOutputStream.writeInt(files.length);

                    // for(int i=0; i<files.length; i++) {
                    //     System.out.println(files[i]); 
                    // }


                    for(int i=0; i<files.length; i++) {
                        FileInputStream fileInputStream = new FileInputStream(files[i]);
                        
                        String fileName = files[i].getName();
                        byte[] fileNameBytes = fileName.getBytes();

                        byte[] fileContentBytes = new byte[(int)files[i].length()];
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
    

    public static String getFileExtension(String fileName) {
        
        int i = fileName.lastIndexOf('.');

        if (i>0) {
            return fileName.substring(i+1);
        }
        else {
            return "No extension found";
        }
    }
}


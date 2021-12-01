package Manasa;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.ArrayList;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class client {
    static ArrayList<MyFile> myFiles = new ArrayList<>();
    public static void main(String[] args) throws IOException{
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
        

        jDownload.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Socket s = new Socket("localhost", 4999);
                    DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());
                    dataOutputStream.writeInt(0);

                    DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
                    int fileCount = dataInputStream.readInt();

                    //System.out.println("Came in download.addActionListener; Count: "+fileCount);
                    for (int i=0; i<fileCount; i++) {
                        int fileNameLength = dataInputStream.readInt();
                        byte[] fileNameBytes = new byte[fileNameLength];
                        dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                        String fileName = new String(fileNameBytes);
                        int fileContentLength = dataInputStream.readInt();
                        byte[] fileContentBytes = new byte[fileContentLength];
                        dataInputStream.readFully(fileContentBytes, 0, fileContentLength); 

                        JPanel jpFileRow = new JPanel();
                        jpFileRow.setLayout(new BoxLayout(jpFileRow, BoxLayout.Y_AXIS));

                        JLabel jFileName = new JLabel("<html><br>" + fileName + "<br></html>");
                        jFileName.setFont(new Font("Arial", Font.BOLD, 20));

                        jpFileRow.setName(String.valueOf(fileId));

                        jpFileRow.add(jFileName);
                        jP.add(jpFileRow);
                        jF.validate();
                        
                        myFiles.add(new MyFile(fileId, fileName, fileContentBytes, getFileExtension(fileName)));
                        String fileClient = "/Users/manasakandala/UTD/Net Sec/Project/FTP Connection/Client/" + fileName; 
                        File fileDownload = new File(fileClient);
                        FileOutputStream fileOutputStream = new FileOutputStream(fileDownload);
                        fileOutputStream.write(fileContentBytes);
                        fileOutputStream.close();
                    }
                    

                } catch (IOException error) {
                    error.printStackTrace();
                }

                
            }   
        });
        
        jSelect.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jFile = new JFileChooser();
                jFile.setDialogTitle("Choose the file to send..");

                if(jFile.showOpenDialog(null)== JFileChooser.APPROVE_OPTION) {
                    file[0] = jFile.getSelectedFile();
                }
                
            }   
        });

        jSend.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                
                    try {
                        Socket s = new Socket("localhost", 4999);
                        FileInputStream fileInputStream = new FileInputStream(file[0].getAbsolutePath());
                        
                        DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());

                        String fileName = file[0].getName();
                        byte[] fileNameBytes = fileName.getBytes();

                        byte[] fileContentBytes = new byte[(int)file[0].length()];
                        fileInputStream.read(fileContentBytes); 

                        dataOutputStream.writeInt(fileNameBytes.length);
                        dataOutputStream.write(fileNameBytes);

                        dataOutputStream.writeInt(fileContentBytes.length);
                        dataOutputStream.write(fileContentBytes);

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

        if (i>0) {
            return fileName.substring(i+1);
        }
        else {
            return "No extension found";
        }
    }
}


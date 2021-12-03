package com.csproj.Client;

import javax.swing.*;
// import javax.swing.border.EmptyBorder;
// import java.util.ArrayList;

import java.util.ArrayList;
import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
// import java.io.*;
// import java.net.*;


public class FilesServer {
    public static String listfiles() {
        String str = "";

        JFrame jList = new JFrame();
        jList.setSize(500, 500);
        jList.setLayout(new BoxLayout(jList.getContentPane(), BoxLayout.Y_AXIS));
        jList.setDefaultCloseOperation(jList.EXIT_ON_CLOSE);

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

        // for(int i=0; i<fileNames.size(); i++) {
        //     System.out.println(fileNames);
        // }

        // JPanel jpFileRow = new JPanel();
        // jpFileRow.setLayout(new BoxLayout(jpFileRow, BoxLayout.Y_AXIS));


        // JLabel jFileName = new JLabel(fileName);
        // jFileName.setFont(new Font("Arial", Font.BOLD, 20));

        // jpFileRow.setName(String.valueOf(fileId));

        // jpFileRow.add(jFileName);
        // jP.add(jpFileRow);
        // jF.validate();

        return str;
    }
}

package com.csproj.Server;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class main {

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        rsaserver rsaserver = new rsaserver();
        rsaserver.rsaPublicKeyExchange("localhost", 8808);
        dhserver serverDH = new dhserver();
        String secretkeyServer = serverDH.initConnection();
    }
}

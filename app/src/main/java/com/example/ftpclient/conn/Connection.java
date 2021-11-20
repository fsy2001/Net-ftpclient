package com.example.ftpclient.conn;

import java.io.IOException;
import java.io.InputStream;

public class Connection {
    public String status = "";

    public Connection(String host, String port, boolean passive) {

    }

    public void connect(String username, String password) {

    }



    public void sendFile(String filename, InputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

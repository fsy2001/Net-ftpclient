package com.example.ftpclient.conn;

import android.annotation.SuppressLint;

import com.example.ftpclient.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Connection {
    private static final int localDataPort = 8083;

    /* 服务器网络信息 */
    public String serverIp;
    public int serverPort;
    private String serverDataIp;
    private int serverDataPort; // 远程服务器的端口，通过PASV命令取得

    /* 本机网络信息 */
    private final Socket socket;
    private ServerSocket dataListener;
    private final PrintWriter out;
    private final BufferedReader in;

    /* 上下文变量 */
    public boolean passive = true;
    public boolean binaryMode = true;


    public Connection(String ip, int port) throws IOException {
        this.socket = new Socket(ip, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        this.serverIp = ip;
        this.serverPort = port;
    }

    public void close() {
        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean login(String username, String password) {
        try {
            out.println(String.format("USER %s", username));
            String response = in.readLine();
            if (response.startsWith("230")) { // 无须密码直接登录
                return true;
            } else if (response.startsWith("331")) { // 需要密码
                out.println(String.format("PASS %s", password));
                response = in.readLine();
                return response.startsWith("230");
            } else return false; // 其它错误

        } catch (IOException e) {
            return false;
        }
    }

    public boolean setPassive() {
        try {
            out.println("PASV");
            String response = in.readLine();
            String[] parts = response.split("\\s+");
            if (parts.length < 2 || !parts[0].equals("227")) return false;

            String message = parts[1];
            String[] addressParts = message.split(","); // 6个数字，前4个为IP，后2个为端口号
            if (addressParts.length != 6) return false;

            /* 检查IP地址格式是否合法 */
            int[] ipArray = {Integer.parseInt(addressParts[0]),
                    Integer.parseInt(addressParts[1]),
                    Integer.parseInt(addressParts[2]),
                    Integer.parseInt(addressParts[3])};
            for (Integer i : ipArray) {
                if (i < 0 || i >= 256) return false;
            }
            @SuppressLint("DefaultLocale")
            String ip = String.format("%d.%d.%d.%d", ipArray[0], ipArray[1], ipArray[2], ipArray[3]);

            /* 检查端口号是否合法 */
            int port = Integer.parseInt(addressParts[4]) * 256 + Integer.parseInt(addressParts[5]);
            if (port < 0) return false;

            serverDataIp = ip;
            serverDataPort = port;
            passive = true;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean sendPort(String ipAddress) {
        try {
            @SuppressLint("DefaultLocale")
            String port = String.format("%d,%d", localDataPort / 256, localDataPort % 256);
            String ip = ipAddress.replace('.', ',');
            passive = false;
            out.println(String.format("PORT %s,%s", ip, port));
            String response = in.readLine();
            return response.startsWith("200");
        } catch (IOException e) {
            return false;
        }
    }

    public int setType(boolean binary) { // binary为true，则是binary；false为ASCII
        try {
            out.println(String.format("TYPE: %s", binary ? "I" : "A"));
            String response = in.readLine();
            if (response.startsWith("200")) {
                this.binaryMode = binary;
                return 0;
            } else return R.string.alert_transfer_mode_not_set;
        } catch (IOException e) {
            return R.string.alert_net_error;
        }
    }

    private Socket createDataConnection() throws IOException {
        if (!passive) {
            dataListener = new ServerSocket(localDataPort);
            return dataListener.accept();
        } else {
            return new Socket(serverDataIp, serverDataPort);
        }
    }

    public int uploadFile(String filename, InputStream fileStream) {
        try {
            Socket dataConn = createDataConnection();
            out.println(String.format("STOR %s", filename));

            String response = in.readLine();
            if (!response.startsWith("150")) { // 服务器拒绝传输
                dataConn.close();
                if (dataListener != null) { // 关闭监听端口
                    dataListener.close();
                    dataListener = null;
                }
                return R.string.alert_server_rejection;
            }

            if (binaryMode) { // 二进制模式
                int bufferSize = 1460; // 一个TCP payload的大小
                byte[] buffer = new byte[bufferSize];
                BufferedOutputStream dataOut = new BufferedOutputStream(dataConn.getOutputStream(), bufferSize);
                BufferedInputStream fileIn = new BufferedInputStream(fileStream, bufferSize);
                int size;
                while ((size = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, size);
                }
                dataOut.close();
                fileIn.close();
            } else { // 文本模式
                PrintWriter dataOut = new PrintWriter(dataConn.getOutputStream(), true);
                BufferedReader fileIn = new BufferedReader(new InputStreamReader(fileStream));
                String line;
                while ((line = fileIn.readLine()) != null) {
                    dataOut.println(line);
                }
                dataOut.close();
                fileIn.close();
            }

            dataConn.close();
            if (dataListener != null) { // 关闭监听端口
                dataListener.close();
                dataListener = null;
            }

            String completeResponse = in.readLine();
            return completeResponse.startsWith("200") ? 0 : R.string.alert_upload_fail;
        } catch (IOException e) {
            return R.string.alert_net_error;
        }
    }

    // R.string.alert_download_error
    public int downloadFile(String filename, OutputStream fileStream) {
        try {
            Socket dataConn = createDataConnection();
            out.println(String.format("RETR %s", filename));

            String response = in.readLine();
            if (!response.startsWith("150")) {
                dataConn.close();
                if (dataListener != null) { // 关闭监听端口
                    dataListener.close();
                    dataListener = null;
                }
                return R.string.alert_server_rejection;
            }

            if (binaryMode) { // 二进制模式
                int bufferSize = 1460; // 一个TCP payload的大小
                byte[] buffer = new byte[bufferSize];
                BufferedOutputStream fileOut = new BufferedOutputStream(fileStream, bufferSize);
                BufferedInputStream dataIn = new BufferedInputStream(dataConn.getInputStream(), bufferSize);
                int size;
                while ((size = dataIn.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, size);
                }
                fileOut.close();
                dataIn.close();
            } else { // 文本模式
                PrintWriter fileOut = new PrintWriter(fileStream);
                BufferedReader dataIn = new BufferedReader(new InputStreamReader(dataConn.getInputStream()));
                String line;
                while ((line = dataIn.readLine()) != null) {
                    fileOut.println(line);
                }
                fileOut.close();
                dataIn.close();
            }

            dataConn.close();
            if (dataListener != null) { // 关闭监听端口
                dataListener.close();
                dataListener = null;
            }

            String completeResponse = in.readLine();
            return completeResponse.startsWith("200") ? 0 : R.string.alert_download_fail;
        } catch (IOException e) {
            return R.string.alert_net_error;
        }
    }

    public void quit() {
        try {
            out.println("QUIT");
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
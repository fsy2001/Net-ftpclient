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
    private final Socket controlSocket;
    private ServerSocket dataListener;
    private final PrintWriter out;
    private final BufferedReader in;


    public Connection(String ip, int port) throws IOException {
        this.controlSocket = new Socket(ip, port);
        this.out = new PrintWriter(controlSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));

        this.serverIp = ip;
        this.serverPort = port;
    }

    public void close() {
        try {
            out.close();
            in.close();
            controlSocket.close();
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

    public boolean setPassive() throws IOException {
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
        for (Integer i : ipArray)
            if (i < 0 || i >= 256) return false;
        @SuppressLint("DefaultLocale")
        String ip = String.format("%d.%d.%d.%d", ipArray[0], ipArray[1], ipArray[2], ipArray[3]);

        /* 检查端口号是否合法 */
        int port = Integer.parseInt(addressParts[4]) * 256 + Integer.parseInt(addressParts[5]);
        if (port < 0) return false;

        serverDataIp = ip;
        serverDataPort = port;
        return true;
    }

    public boolean sendPort(String ipAddress) {
        try {
            @SuppressLint("DefaultLocale")
            String port = String.format("%d,%d", localDataPort / 256, localDataPort % 256);
            String ip = ipAddress.replace('.', ',');
            out.println(String.format("PORT %s,%s", ip, port));
            String response = in.readLine();
            return response.startsWith("200");
        } catch (IOException e) {
            return false;
        }
    }

    public int setType(boolean binary) throws IOException { // binary为true，则是binary；false为ASCII
        out.println(String.format("TYPE %s", binary ? "I" : "A"));
        String response = in.readLine();
        if (response.startsWith("200")) return 0;
        else return R.string.alert_transfer_mode_not_set;
    }

    private Socket createDataConnection(boolean passive) throws IOException {
        if (!passive) {
            return dataListener.accept();
        } else {
            return new Socket(serverDataIp, serverDataPort);
        }
    }

    public int uploadFile(String filename, InputStream fileStream,
                          boolean binary, boolean passive, String ipAddress) {
        try {
            int errorCode;
            if ((errorCode = setType(binary)) != 0) // 设置传输模式
                return errorCode;

            if (!(passive ? setPassive() : sendPort(ipAddress))) // 设置连接模式
                return R.string.alert_connection_mode_not_set;

            if (!passive)
                dataListener = new ServerSocket(localDataPort);

            out.println(String.format("STOR %s", filename)); // 发送传输指令
            Socket dataConn = createDataConnection(passive); // 建立连接 FIXME: 可能的时序问题：服务器没有建立连接，阻塞在这里

            String startResponse = in.readLine(); // 服务器准备传输
            if (!startResponse.startsWith("150")) { // 服务器拒绝传输
                dataConn.close();
                if (!passive) { // 关闭监听端口
                    dataListener.close();
                    dataListener = null;
                }
                return R.string.alert_server_rejection;
            }

            /* 传输数据 */
            if (binary)
                binaryDump(fileStream, dataConn.getOutputStream());
            else
                textDump(fileStream, dataConn.getOutputStream());

            /* 收尾工作 */
            dataConn.close();
            if (!passive) { // 关闭监听端口
                dataListener.close();
                dataListener = null;
            }

            String completeResponse = in.readLine();
            return completeResponse.startsWith("226") ? 0 : R.string.alert_upload_fail;
        } catch (IOException e) {
            return R.string.alert_net_error;
        }
    }

    public int downloadFile(String filename, OutputStream fileStream,
                            boolean binary, boolean passive, String ipAddress) {
        try {
            int errorCode;
            if ((errorCode = setType(binary)) != 0) // 设置传输模式
                return errorCode;

            if (!(passive ? setPassive() : sendPort(ipAddress))) // 设置连接模式
                return R.string.alert_connection_mode_not_set;

            if (!passive)
                dataListener = new ServerSocket(localDataPort);

            out.println(String.format("RETR %s", filename));
            Socket dataConn = createDataConnection(passive); // FIXME

            String startResponse = in.readLine();
            if (!startResponse.startsWith("150")) {
                dataConn.close();
                if (!passive) { // 关闭监听端口
                    dataListener.close();
                    dataListener = null;
                }
                return R.string.alert_server_rejection;
            }

            /* 传输数据 */
            if (binary)
                binaryDump(dataConn.getInputStream(), fileStream);
            else
                textDump(dataConn.getInputStream(), fileStream);

            /* 收尾工作 */
            dataConn.close();
            if (!passive) { // 关闭监听端口
                dataListener.close();
                dataListener = null;
            }

            String completeResponse = in.readLine();
            return completeResponse.startsWith("226") ? 0 : R.string.alert_download_fail;
        } catch (IOException e) {
            return R.string.alert_net_error;
        }
    }

    private void binaryDump(InputStream in, OutputStream out) throws IOException {
        int bufferSize = 1460; // 一个TCP payload的大小
        byte[] buffer = new byte[bufferSize];
        BufferedInputStream src = new BufferedInputStream(in, bufferSize);
        BufferedOutputStream target = new BufferedOutputStream(out, bufferSize);
        int size;
        while ((size = src.read(buffer)) != -1)
            target.write(buffer, 0, size);
        src.close();
        target.close();
    }

    private void textDump(InputStream in, OutputStream out) throws IOException {
        BufferedReader src = new BufferedReader(new InputStreamReader(in));
        PrintWriter target = new PrintWriter(out);
        String line;
        while ((line = src.readLine()) != null)
            target.write(line);
        src.close();
        target.close();
    }

    public void quit() {
        try {
            out.println("QUIT");
            in.close();
            out.close();
            controlSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
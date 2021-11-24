package com.example.ftpclient.ui;

import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.example.ftpclient.R;
import com.example.ftpclient.conn.Connection;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class FileExplorer extends AppCompatActivity {
    private static final int PICK_FILE_TO_UPLOAD = 0x2222;
    private static final int SELECT_DOWNLOAD_POS = 0x3333;

    private Connection connection;
    private Context context;

    private EditText filenameText;
    private RadioButton passiveButton, binaryButton;

    private Integer errorMsgCode;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        this.context = FileExplorer.this;
        connection = MainActivity.connection;
        TextView host = findViewById(R.id.host), port = findViewById(R.id.port);
        host.setText(connection.serverIp);
        port.setText(Integer.toString(connection.serverPort));

        filenameText = findViewById(R.id.download_filename);
        passiveButton = findViewById(R.id.connection_passive);
        binaryButton = findViewById(R.id.transfer_binary);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Thread thread = new Thread(() -> connection.quit());
        thread.start();
    }

    private void showAlert(Integer title, Integer message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog alert = builder.setIcon(R.mipmap.ic_launcher_round)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .create();
        alert.show();
    }

    public void disconnect(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog alert = builder.setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.alert_disconnect_title)
                .setMessage(R.string.alert_disconnect_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Thread thread = new Thread(() -> connection.quit());
                    thread.start();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    finish();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .create();
        alert.show();
    }

    public void selectDownloadPosition(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, SELECT_DOWNLOAD_POS);
    }

    public void handleDownload(Intent resultData, int resultCode) {
        if (resultCode != RESULT_OK || resultData == null) {
            showAlert(R.string.alert_title, R.string.alert_dir_not_exist);
            return;
        }

        Uri path = resultData.getData();
        DocumentFile targetPos = DocumentFile.fromTreeUri(this, path);
        if (targetPos == null) {
            showAlert(R.string.alert_title, R.string.alert_dir_not_exist);
            return;
        }

        String filename = filenameText.getText().toString();
        if (targetPos.findFile(filename) != null) {
            showAlert(R.string.alert_title, R.string.alert_conflict_filename);
            return;
        }
        DocumentFile downloadFile;
        if ((downloadFile = targetPos.createFile("*/*", filename)) == null) {
            showAlert(R.string.alert_title, R.string.alert_cannot_create_file);
            return;
        }

        try {
            OutputStream outputStream = getContentResolver().openOutputStream(downloadFile.getUri());

            boolean passive = passiveButton.isChecked(),
                    binary = binaryButton.isChecked();

            Thread thread = new Thread(() -> {
                /* 设置传输模式 */
                if ((errorMsgCode = connection.setType(binary)) != 0) {
                    runOnUiThread(() -> showAlert(R.string.alert_title, errorMsgCode));
                    return;
                }

                /* 设置连接模式 */
                boolean success = passive ?
                        connection.setPassive() : connection.sendPort(getLocalIpAddress());
                if (!success) {
                    runOnUiThread(() -> showAlert(R.string.alert_title, R.string.alert_connection_mode_not_set));
                    return;
                }

                /* 开始传输 */
                runOnUiThread(() -> Snackbar
                        .make(findViewById(R.id.file_upload), R.string.message_start_transfer, LENGTH_SHORT)
                        .show());
                errorMsgCode = connection.downloadFile(filename, outputStream);

                /* 传输结束 */
                runOnUiThread(() -> {
                    if (errorMsgCode == 0) { // 成功提示
                        Snackbar
                                .make(findViewById(R.id.file_upload), R.string.message_finish_transfer, LENGTH_SHORT)
                                .show();
                    } else showAlert(R.string.alert_title, errorMsgCode); // 错误提示
                });
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            thread.start();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void pickFileUpload(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        startActivityForResult(intent, PICK_FILE_TO_UPLOAD);
    }

    public void handleUpload(Intent resultData, int resultCode) {
        if (resultCode != RESULT_OK || resultData == null) {
            showAlert(R.string.alert_title, R.string.alert_file_not_exist);
            return;
        }

        Uri uri = resultData.getData();

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Cursor cursor = getContentResolver()
                    .query(uri, null, null, null, null, null);
            cursor.moveToFirst();
            String fileName = cursor.getString(
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

            boolean passive = passiveButton.isChecked(),
                    binary = binaryButton.isChecked();

            Thread thread = new Thread(() -> {
                /* 设置传输模式 */
                if ((errorMsgCode = connection.setType(binary)) != 0) {
                    runOnUiThread(() -> showAlert(R.string.alert_title, errorMsgCode));
                    return;
                }

                /* 设置连接模式 */
                boolean success = passive ?
                        connection.setPassive() : connection.sendPort(getLocalIpAddress());
                if (!success) {
                    runOnUiThread(() -> showAlert(R.string.alert_title, R.string.alert_connection_mode_not_set));
                    return;
                }

                /* 开始传输 */
                runOnUiThread(() -> Snackbar
                        .make(findViewById(R.id.file_upload), R.string.message_start_transfer, LENGTH_SHORT)
                        .show());
                errorMsgCode = connection.uploadFile(fileName, inputStream);

                /* 传输结束 */
                runOnUiThread(() -> {
                    if (errorMsgCode == 0) { // 成功提示
                        Snackbar
                                .make(findViewById(R.id.file_upload), R.string.message_finish_transfer, LENGTH_SHORT)
                                .show();
                    } else showAlert(R.string.alert_title, errorMsgCode); // 错误提示
                });
                try {
                    inputStream.close();
                    cursor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            thread.start();
        } catch (Exception e) {
            showAlert(R.string.alert_title, R.string.alert_file_not_exist);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == PICK_FILE_TO_UPLOAD)
            handleUpload(resultData, resultCode);

        if (requestCode == SELECT_DOWNLOAD_POS)
            handleDownload(resultData, resultCode);
    }

    public String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            @SuppressLint("MissingPermission") WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return ipAddressToString(wifiInfo.getIpAddress());
        }
        return "";
    }

    public static String ipAddressToString(int ipAddress) {

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray)
                    .getHostAddress();
        } catch (UnknownHostException ex) {
            ipAddressString = "NaN";
        }

        return ipAddressString;
    }
}
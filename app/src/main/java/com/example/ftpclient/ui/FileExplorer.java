package com.example.ftpclient.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ftpclient.R;
import com.example.ftpclient.conn.Connection;

import java.io.InputStream;

public class FileExplorer extends AppCompatActivity {
    private static final int PICK_FILE_TO_UPLOAD = 2;

    private Connection connection;
    private Context context;

    private EditText filenameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        this.context = FileExplorer.this;
        connection = MainActivity.connection;
        TextView host = findViewById(R.id.host), port = findViewById(R.id.port);
        host.setText(connection.host);
        port.setText(connection.port);

        filenameText = findViewById(R.id.download_filename);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TODO: 断开连接
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
                .setPositiveButton(R.string.ok, (dialog, which) -> finish())
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .create();
        alert.show();
    }

    public void download(View view) {
        String filename = filenameText.getText().toString();
        // TODO: 交付FTP模块下载
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


        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             Cursor cursor = getContentResolver()
                     .query(uri, null, null, null, null, null)) {
            cursor.moveToFirst();
            String fileName = cursor.getString(
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            // TODO: 将输出流、文件名交付FTP模块发送
        } catch (Exception ignored) {
            showAlert(R.string.alert_title, R.string.alert_file_not_exist);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == PICK_FILE_TO_UPLOAD)
            handleUpload(resultData, resultCode);
    }
}
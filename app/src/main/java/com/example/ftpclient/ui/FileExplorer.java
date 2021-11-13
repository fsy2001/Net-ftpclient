package com.example.ftpclient.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ftpclient.R;
import com.example.ftpclient.databinding.ActivityFileExplorerBinding;

public class FileExplorer extends AppCompatActivity {
    private Context context;

    private EditText filenameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        ActivityFileExplorerBinding binding = ActivityFileExplorerBinding.inflate(getLayoutInflater());
        this.context = FileExplorer.this;

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
                .setPositiveButton(R.string.ok, (dialog, which) -> {})
                .setNegativeButton(R.string.cancel, (dialog, which) -> {})
                .create();
        alert.show();
    }

    public void disconnect(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog alert = builder.setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.alert_disconnect_title)
                .setMessage(R.string.alert_disconnect_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> finish())
                .setNegativeButton(R.string.cancel, (dialog, which) -> {})
                .create();
        alert.show();
    }

    public void download(View view) {
        String filename = filenameText.getText().toString();
    }

    public void upload(View view) {

    }
}
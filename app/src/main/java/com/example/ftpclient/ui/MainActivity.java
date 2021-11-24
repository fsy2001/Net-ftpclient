package com.example.ftpclient.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ftpclient.R;
import com.example.ftpclient.conn.Connection;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    public static Connection connection = null;

    private Context context;

    private EditText hostText;
    private EditText portText;
    private EditText usernameText;
    private EditText passwordText;
    private CheckBox anonymousBox;

    /* 供连接时线程共享的变量 */
    private boolean connectSuccess;
    private Integer errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.context = MainActivity.this;

        hostText = findViewById(R.id.input_host);
        portText = findViewById(R.id.input_port);
        usernameText = findViewById(R.id.input_username);
        passwordText = findViewById(R.id.input_password);
        anonymousBox = findViewById(R.id.checkbox_passive);

        anonymousBox.setOnClickListener(view -> {
            boolean anonymous = anonymousBox.isChecked();
            usernameText.setEnabled(!anonymous);
            passwordText.setEnabled(!anonymous);
        });
    }

    public void showAlert(Integer message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog alert = builder.setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.alert_title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                })
                .create();
        alert.show();
    }

    public void login(View view) {
        connectSuccess = false;

        String host = hostText.getText().toString();
        String port = portText.getText().toString();
        if (!Patterns.IP_ADDRESS.matcher(host).matches() || !port.matches("\\d+")) {
            showAlert(R.string.alert_format_error);
            return;
        }


        Thread thread = new Thread(() -> {
            try {
                connection = new Connection(host, Integer.parseInt(port));
            } catch (IOException e) {
                if (connection != null) connection.close();
                errorMessage = R.string.alert_connection_error;
                return;
            }

            boolean anonymous = anonymousBox.isChecked();

            String username = usernameText.getText().toString();
            String password = passwordText.getText().toString();
            if (!anonymous && !connection.login(username, password)) {
                errorMessage = R.string.alert_credential_error;
                connection.close();
                return;
            }
            connectSuccess = true;
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            return;
        }


        if (connectSuccess) {
            Intent connectIntent = new Intent(this, FileExplorer.class);
            startActivity(connectIntent);
        } else {
            showAlert(errorMessage);
        }
    }
}
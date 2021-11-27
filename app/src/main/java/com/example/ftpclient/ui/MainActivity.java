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
            usernameText.setText(anonymous ? "anonymous" : "");
            passwordText.setText("");
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
        String host = hostText.getText().toString();
        String port = portText.getText().toString();
        if (!Patterns.IP_ADDRESS.matcher(host).matches() || !port.matches("\\d+")) {
            showAlert(R.string.alert_format_error);
            return;
        }

        view.setEnabled(false);

        Thread thread = new Thread(() -> {
            try {
                connection = new Connection(host, Integer.parseInt(port));
            } catch (IOException e) {
                if (connection != null) connection.close();
                runOnUiThread(() -> {
                    view.setEnabled(true);
                    showAlert(R.string.alert_connection_error);
                });
                return;
            }

            String username = usernameText.getText().toString();
            String password = passwordText.getText().toString();
            if (!connection.login(username, password)) {
                runOnUiThread(() -> {
                    view.setEnabled(true);
                    showAlert(R.string.alert_credential_error);
                });
                connection.close();
                return;
            }

            /* 登录成功 */
            runOnUiThread(() -> {
                view.setEnabled(true);
                Intent connectIntent = new Intent(this, FileExplorer.class);
                startActivity(connectIntent);
            });
        });

        thread.start();
    }
}
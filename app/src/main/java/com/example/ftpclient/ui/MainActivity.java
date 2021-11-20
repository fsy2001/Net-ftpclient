package com.example.ftpclient.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.example.ftpclient.R;
import com.example.ftpclient.conn.Connection;


public class MainActivity extends AppCompatActivity {

    private Context context;

    private EditText hostText;
    private EditText portText;
    private EditText usernameText;
    private EditText passwordText;
    private CheckBox passiveBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.context = MainActivity.this;

        hostText = findViewById(R.id.input_host);
        portText = findViewById(R.id.input_port);
        usernameText = findViewById(R.id.input_username);
        passwordText = findViewById(R.id.input_password);
        passiveBox = findViewById(R.id.checkbox_passive);
    }

    public void showAlert(Integer message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog alert = builder.setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.alert_title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {})
                .create();
        alert.show();
    }

    public void login(View view) {
        String host = hostText.getText().toString();
        String port = portText.getText().toString();
        boolean passive = passiveBox.isChecked();
        Connection connection = new Connection(host, port, passive);
        // TODO: 如果无法连接，弹窗报错
        // showAlert(R.string.alert_connection_error);


        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();
        connection.connect(username, password);
        // TODO: 如果登录错误，弹窗报错
        // showAlert(R.string.alert_credential_error);


        // TODO: 将Connection对象的引用传递给下一个Activity
        Intent connectIntent = new Intent(this, FileExplorer.class);
        startActivity(connectIntent);
    }
}
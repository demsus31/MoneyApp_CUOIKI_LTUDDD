package com.example.moneyapp;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText edtUsername, edtPassword;
    Button btnLogin, btnRegister;
    TextView tvForgotPassword; // Khai báo nút quên mật khẩu
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword); // Ánh xạ

        dbHelper = new DatabaseHelper(this);

        // --- NÚT ĐĂNG KÝ ---
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // --- NÚT ĐĂNG NHẬP ---
        btnLogin.setOnClickListener(v -> {
            String user = edtUsername.getText().toString();
            String pass = edtPassword.getText().toString();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Vui lòng nhập đầy đủ thông tin đăng nhập!", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username=? AND password=?", new String[]{user, pass});

            if (cursor.getCount() > 0) {
                SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("current_user", user);
                editor.apply();

                Toast.makeText(MainActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
            }
            cursor.close();
        });

        // --- NÚT QUÊN MẬT KHẨU ---
        tvForgotPassword.setOnClickListener(v -> {
            String user = edtUsername.getText().toString();

            if (user.isEmpty()) {
                Toast.makeText(MainActivity.this, "Vui lòng nhập Tên đăng nhập vào ô phía trên để khôi phục!", Toast.LENGTH_LONG).show();
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();


            Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username=?", new String[]{user});
            if (cursor.getCount() > 0) {
                String newPassword = user + "_khoiphuc";

                ContentValues values = new ContentValues();
                values.put("password", newPassword);
                db.update("users", values, "username=?", new String[]{user});

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Khôi phục thành công");
                builder.setMessage("Mật khẩu mới của bạn là:\n\n" + newPassword + "\n\nHãy dùng mật khẩu này để đăng nhập và đổi lại mật khẩu mới nhé!");
                builder.setPositiveButton("Đã hiểu", null);
                builder.show();

                edtPassword.setText(newPassword);
            } else {
                Toast.makeText(MainActivity.this, "Tài khoản không tồn tại!", Toast.LENGTH_SHORT).show();
            }
            cursor.close();
        });
    }
}
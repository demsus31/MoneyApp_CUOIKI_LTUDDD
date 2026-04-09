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
            String user = edtUsername.getText().toString();
            String pass = edtPassword.getText().toString();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
            } else {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("username", user);
                values.put("password", pass);

                long result = db.insert("users", null, values);
                if (result == -1) {
                    Toast.makeText(MainActivity.this, "Tài khoản đã tồn tại!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Đăng ký thành công! Hãy bấm Đăng Nhập.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // --- NÚT ĐĂNG NHẬP ---
        btnLogin.setOnClickListener(v -> {
            String user = edtUsername.getText().toString();
            String pass = edtPassword.getText().toString();

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

            // Ép người dùng phải gõ cái tên tài khoản vào ô Username trước
            if (user.isEmpty()) {
                Toast.makeText(MainActivity.this, "Vui lòng nhập Tên đăng nhập vào ô phía trên để khôi phục!", Toast.LENGTH_LONG).show();
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Kiểm tra xem tên đăng nhập này có tồn tại trong hệ thống không
            Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username=?", new String[]{user});
            if (cursor.getCount() > 0) {
                // Tạo mật khẩu khôi phục theo công thức em yêu cầu
                String newPassword = user + "_khoiphuc";

                // Cập nhật mật khẩu mới vào Database (dùng hàm UPDATE)
                ContentValues values = new ContentValues();
                values.put("password", newPassword);
                db.update("users", values, "username=?", new String[]{user});

                // Hiện cái bảng thông báo cho người dùng thấy
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Khôi phục thành công");
                builder.setMessage("Mật khẩu mới của bạn là:\n\n" + newPassword + "\n\nHãy dùng mật khẩu này để đăng nhập và đổi lại mật khẩu mới nhé!");
                builder.setPositiveButton("Đã hiểu", null);
                builder.show();

                // Tự động điền luôn mật khẩu mới vào ô cho tiện
                edtPassword.setText(newPassword);
            } else {
                Toast.makeText(MainActivity.this, "Tài khoản không tồn tại!", Toast.LENGTH_SHORT).show();
            }
            cursor.close();
        });
    }
}
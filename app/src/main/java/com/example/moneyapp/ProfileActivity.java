package com.example.moneyapp;

import android.annotation.SuppressLint;
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
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    TextView tvUsername;
    EditText edtCurrentPass, edtNewPass;
    Button btnChangePass, btnLogout, btnBackToHome;
    DatabaseHelper dbHelper;

    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvUsername = findViewById(R.id.tvUsername);
        edtCurrentPass = findViewById(R.id.edtCurrentPass);
        edtNewPass = findViewById(R.id.edtNewPass);
        btnChangePass = findViewById(R.id.btnChangePass);
        btnLogout = findViewById(R.id.btnLogout);
        btnBackToHome = findViewById(R.id.btnBackToHome);

        dbHelper = new DatabaseHelper(this);

        // Lấy tên tài khoản đang dùng
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUser = sharedPref.getString("current_user", "");
        tvUsername.setText("Xin chào, " + currentUser + "!");

        // --- TỰ ĐỘNG HIỆN MẬT KHẨU HIỆN TẠI ---
        SQLiteDatabase dbRead = dbHelper.getReadableDatabase();
        Cursor cursor = dbRead.rawQuery("SELECT password FROM users WHERE username=?", new String[]{currentUser});
        if (cursor.moveToFirst()) {
            String currentPassword = cursor.getString(0);
            edtCurrentPass.setText(currentPassword); // Ném mật khẩu vào ô hiển thị
        }
        cursor.close();

        // --- XỬ LÝ NÚT ĐỔI MẬT KHẨU ---
        btnChangePass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = edtNewPass.getText().toString();

                if (newPassword.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, "Vui lòng nhập mật khẩu mới!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Cập nhật Database
                SQLiteDatabase dbWrite = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("password", newPassword);

                dbWrite.update("users", values, "username=?", new String[]{currentUser});

                Toast.makeText(ProfileActivity.this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();

                // Cập nhật lại giao diện cho người dùng thấy
                edtCurrentPass.setText(newPassword);
                edtNewPass.setText(""); // Xóa ô nhập đi
            }
        });

        // --- NÚT ĐĂNG XUẤT ---
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.apply();

                Toast.makeText(ProfileActivity.this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        // --- NÚT QUAY LẠI ---
        btnBackToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
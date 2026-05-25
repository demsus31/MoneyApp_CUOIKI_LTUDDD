package com.example.moneyapp;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {

    Button btnBack, btnPickDate, btnSave;
    EditText edtAmount, edtNote, edtDate;
    RadioGroup rgType;
    RadioButton rbThu;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Ánh xạ
        btnBack = findViewById(R.id.btnBack);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnSave = findViewById(R.id.btnSave);
        edtAmount = findViewById(R.id.edtAmount);
        edtNote = findViewById(R.id.edtNote);
        edtDate = findViewById(R.id.edtDate);
        rgType = findViewById(R.id.rgType);
        rbThu = findViewById(R.id.rbThu);

        dbHelper = new DatabaseHelper(this);

        // --- 1. NÚT QUAY LẠI ---
        btnBack.setOnClickListener(v -> finish());

        // --- 2. TỰ ĐỘNG ĐIỀN NGÀY HÔM NAY ---
        String homNay = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        edtDate.setText(homNay);

        // --- 3. ẢO THUẬT TỰ ĐIỀN DẤU GẠCH CHÉO (/) ---
        edtDate.addTextChangedListener(new TextWatcher() {
            boolean isUpdating = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdating) return;
                isUpdating = true;
                String str = s.toString().replace("/", "");
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < str.length(); i++) {
                    if (i == 2 || i == 4) formatted.append("/");
                    formatted.append(str.charAt(i));
                }
                if (formatted.length() > 10) formatted.delete(10, formatted.length());
                edtDate.setText(formatted.toString());
                edtDate.setSelection(formatted.length());
                isUpdating = false;
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // --- 4. SỰ KIỆN BẤM NÚT LỊCH CHỌN NGÀY ---
        btnPickDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String pickedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                edtDate.setText(pickedDate);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // --- 5. SỰ KIỆN LƯU VÀO DATABASE ---
        btnSave.setOnClickListener(v -> {
            String amountStr = edtAmount.getText().toString();
            String note = edtNote.getText().toString();
            String date = edtDate.getText().toString();

            if (amountStr.isEmpty() || note.isEmpty() || date.length() < 10) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            int type = rbThu.isChecked() ? 1 : 2; // 1 là Thu, 2 là Chi

            // Lấy user đang đăng nhập
            SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String currentUser = sharedPref.getString("current_user", "");

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("username", currentUser);
            values.put("type", type);
            values.put("amount", amount);
            values.put("date", date);
            values.put("note", note);

            long result = db.insert("transactions", null, values);
            if (result != -1) {
                Toast.makeText(this, "Thêm giao dịch thành công!", Toast.LENGTH_SHORT).show();
                finish(); // Thêm xong tự động đóng trang, quay về màn hình chính
            } else {
                Toast.makeText(this, "Lỗi khi lưu dữ liệu!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
package com.example.moneyapp;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {

    RadioButton rbThu, rbChi;
    EditText edtAmount, edtNote, edtDate;
    Button btnSaveTransaction, btnBack; // KHAI BÁO THÊM NÚT BACK
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Ánh xạ giao diện
        rbThu = findViewById(R.id.rbThu);
        rbChi = findViewById(R.id.rbChi);
        edtAmount = findViewById(R.id.edtAmount);
        edtNote = findViewById(R.id.edtNote);
        edtDate = findViewById(R.id.edtDate);
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction);
        btnBack = findViewById(R.id.btnBack); // ÁNH XẠ NÚT BACK

        dbHelper = new DatabaseHelper(this);

        // --- SỰ KIỆN KHI BẤM NÚT QUAY LẠI ---
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Lệnh này sẽ đóng màn hình hiện tại và tự động văng về màn hình trước đó
            }
        });

        // --- 1. ẢO THUẬT AUTO-FORMAT SỐ TIỀN ---
        edtAmount.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    edtAmount.removeTextChangedListener(this);
                    String cleanString = s.toString().replaceAll("[.]", "");
                    if (!cleanString.isEmpty()) {
                        double parsed = Double.parseDouble(cleanString);
                        DecimalFormat formatter = new DecimalFormat("#,###");
                        String formatted = formatter.format(parsed).replace(",", ".");
                        current = formatted;
                        edtAmount.setText(formatted);
                        edtAmount.setSelection(formatted.length());
                    } else {
                        current = "";
                        edtAmount.setText("");
                    }
                    edtAmount.addTextChangedListener(this);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // --- 2. ẢO THUẬT AUTO-FORMAT NGÀY THÁNG ---
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

        // --- 3. XỬ LÝ LƯU GIAO DỊCH VÀO DATABASE ---
        btnSaveTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountStr = edtAmount.getText().toString();
                String note = edtNote.getText().toString();
                String date = edtDate.getText().toString();

                if (amountStr.isEmpty() || note.isEmpty() || date.isEmpty()) {
                    Toast.makeText(AddTransactionActivity.this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!kiemTraNgayHopLe(date)) return;

                int type = rbThu.isChecked() ? 1 : 2;
                String cleanAmountStr = amountStr.replace(".", "");
                double amount = Double.parseDouble(cleanAmountStr);

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
                    Toast.makeText(AddTransactionActivity.this, "Đã lưu giao dịch!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddTransactionActivity.this, "Lỗi! Không thể lưu.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // --- HÀM BẢO VỆ CHỐNG NHẬP NGÀY THÁNG SAI ---
    private boolean kiemTraNgayHopLe(String inputDate) {
        if (inputDate.length() < 10) {
            Toast.makeText(this, "Vui lòng nhập đủ ngày (VD: 05/09/2026)", Toast.LENGTH_SHORT).show();
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false);

        try {
            Date parsedDate = sdf.parse(inputDate);
            Date today = new Date();

            if (parsedDate.after(today)) {
                Toast.makeText(this, "Không thể nhập giao dịch ở tương lai!", Toast.LENGTH_LONG).show();
                return false;
            }
            return true;

        } catch (ParseException e) {
            Toast.makeText(this, "Ngày tháng không hợp lệ (sai số ngày tối đa)!", Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
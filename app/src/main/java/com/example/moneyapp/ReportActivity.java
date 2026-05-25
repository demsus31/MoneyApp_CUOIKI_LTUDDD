package com.example.moneyapp;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    TextView tvBCThu, tvBCChi, tvCanhBaoBC;
    Button btnChonThangBC, btnBackReport; // Thêm biến cho nút Back
    View barThu, barThuEmpty, barChi, barChiEmpty;
    DatabaseHelper dbHelper;
    String thangHienTai = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        tvBCThu = findViewById(R.id.tvBCThu);
        tvBCChi = findViewById(R.id.tvBCChi);
        tvCanhBaoBC = findViewById(R.id.tvCanhBaoBC);
        btnChonThangBC = findViewById(R.id.btnChonThangBC);
        btnBackReport = findViewById(R.id.btnBackReport); // Ánh xạ nút Back
        barThu = findViewById(R.id.barThu);
        barThuEmpty = findViewById(R.id.barThuEmpty);
        barChi = findViewById(R.id.barChi);
        barChiEmpty = findViewById(R.id.barChiEmpty);

        dbHelper = new DatabaseHelper(this);

        // --- SỰ KIỆN NÚT QUAY LẠI ---
        btnBackReport.setOnClickListener(v -> {
            finish(); // Đóng màn hình Báo cáo, tự động quay về trang trước đó
        });

        // Lấy tháng hiện tại làm mặc định
        thangHienTai = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date());
        btnChonThangBC.setText("Tháng: " + thangHienTai);

        // Bấm vào nút để chọn tháng khác
        btnChonThangBC.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                thangHienTai = String.format(Locale.getDefault(), "%02d/%04d", month + 1, year);
                btnChonThangBC.setText("Tháng: " + thangHienTai);
                veBieuDo(); // Vẽ lại biểu đồ theo tháng mới
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        veBieuDo(); // Vẽ biểu đồ lần đầu tiên khi mở trang
    }

    @SuppressLint("Range")
    private void veBieuDo() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUser = sharedPref.getString("current_user", "");

        double tongThu = 0, tongChi = 0;
        DecimalFormat formatter = new DecimalFormat("#,###");

        Cursor cursor = db.rawQuery("SELECT * FROM transactions WHERE username = '" + currentUser + "'", null);

        if (cursor.moveToFirst()) {
            do {
                int type = cursor.getInt(cursor.getColumnIndex("type"));
                double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
                String date = cursor.getString(cursor.getColumnIndex("date"));

                if (date.endsWith(thangHienTai)) {
                    if (type == 1) tongThu += amount;
                    else tongChi += amount;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        tvBCThu.setText("Tổng thu: " + formatter.format(tongThu).replace(",", ".") + " đ");
        tvBCChi.setText("Tổng chi: " + formatter.format(tongChi).replace(",", ".") + " đ");

        // --- ẢO THUẬT VẼ BIỂU ĐỒ ---
        double maxTien = Math.max(tongThu, tongChi);
        if (maxTien == 0) maxTien = 1;

        float phanTramThu = (float) ((tongThu / maxTien) * 100);
        float phanTramChi = (float) ((tongChi / maxTien) * 100);

        barThu.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, phanTramThu));
        barThuEmpty.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 100 - phanTramThu));

        barChi.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, phanTramChi));
        barChiEmpty.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 100 - phanTramChi));

        // --- XỬ LÝ CẢNH BÁO VƯỢT CHI ---
        if (tongChi > tongThu && tongChi > 0) {
            tvCanhBaoBC.setVisibility(View.VISIBLE);
        } else {
            tvCanhBaoBC.setVisibility(View.GONE);
        }
    }
}
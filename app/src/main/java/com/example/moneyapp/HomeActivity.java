package com.example.moneyapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    // Khai báo công cụ giao diện
    Button btnThemGiaoDich, btnLocTatCa, btnLocThu, btnLocChi, btnHomNay, btnThangNay, btnProfile;
    TextView tvTongThu, tvTongChi, tvSoDu;
    DatabaseHelper dbHelper;

    // Khai báo công cụ để làm Danh sách (ListView)
    ListView lvLichSu;
    ArrayList<String> danhSachGiaoDich;
    ArrayAdapter<String> adapter;

    // Biến lưu trạng thái người dùng đang chọn Nút Lọc nào
    int cheDoLocLoai = 0;     // 0: Tất cả, 1: Thu, 2: Chi
    int cheDoLocThoiGian = 0; // 0: Mọi lúc, 1: Hôm nay, 2: Tháng này

    // Cờ (Flag) chặn spam: Đảm bảo cảnh báo vượt chi chỉ hiện 1 lần duy nhất khi mở app
    boolean daCanhBao = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Ánh xạ
        btnThemGiaoDich = findViewById(R.id.btnThemGiaoDich);
        btnLocTatCa = findViewById(R.id.btnLocTatCa);
        btnLocThu = findViewById(R.id.btnLocThu);
        btnLocChi = findViewById(R.id.btnLocChi);
        btnHomNay = findViewById(R.id.btnHomNay);
        btnThangNay = findViewById(R.id.btnThangNay);
        btnProfile = findViewById(R.id.btnProfile); // Nút chuyển sang trang Tài khoản

        tvTongThu = findViewById(R.id.tvTongThu);
        tvTongChi = findViewById(R.id.tvTongChi);
        tvSoDu = findViewById(R.id.tvSoDu);
        lvLichSu = findViewById(R.id.lvLichSu);

        // Cài đặt cầu nối (Adapter) cho Danh sách
        dbHelper = new DatabaseHelper(this);
        danhSachGiaoDich = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, danhSachGiaoDich);
        lvLichSu.setAdapter(adapter);

        // --- CÀI ĐẶT SỰ KIỆN CHO CÁC NÚT BẤM ---

        // Chuyển sang trang Profile
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // NÚT TẤT CẢ LÀM MASTER RESET: Nhấn vào là reset sạch sành sanh các bộ lọc
        btnLocTatCa.setOnClickListener(v -> { cheDoLocLoai = 0; cheDoLocThoiGian = 0; capNhatThongKe(); });

        // Hàng 1: Lọc Loại (Thu/Chi)
        btnLocThu.setOnClickListener(v -> { cheDoLocLoai = 1; capNhatThongKe(); });
        btnLocChi.setOnClickListener(v -> { cheDoLocLoai = 2; capNhatThongKe(); });

        // Hàng 2: Lọc Thời Gian. (Tự động reset Lọc Loại về Tất Cả để tránh kẹt logic)
        btnHomNay.setOnClickListener(v -> { cheDoLocThoiGian = 1; cheDoLocLoai = 0; capNhatThongKe(); });
        btnThangNay.setOnClickListener(v -> { cheDoLocThoiGian = 2; cheDoLocLoai = 0; capNhatThongKe(); });

        // Nút Thêm Giao Dịch
        btnThemGiaoDich.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddTransactionActivity.class);
            startActivity(intent);
        });
    }

    // Hàm onResume LUÔN LUÔN CHẠY mỗi khi người dùng quay lại màn hình này
    @Override
    protected void onResume() {
        super.onResume();
        daCanhBao = false; // Reset lại trạng thái cảnh báo
        capNhatThongKe();  // Vẽ lại toàn bộ dữ liệu
    }

    // --- BỘ NÃO TÍNH TOÁN VÀ ĐIỀU HƯỚNG GIAO DIỆN CHÍNH ---
    @SuppressLint("Range")
    private void capNhatThongKe() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1. Lấy chuỗi Ngày/Tháng hiện tại của điện thoại
        String ngayHienTai = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String thangHienTai = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date());

        // 2. Lấy tên tài khoản đang đăng nhập để lọc đúng dữ liệu
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUser = sharedPref.getString("current_user", "");

        // 3. Xây dựng câu lệnh SQL
        String query = "SELECT * FROM transactions WHERE username = '" + currentUser + "'";

        // Lọc thời gian
        if (cheDoLocThoiGian == 1) query += " AND date = '" + ngayHienTai + "'";
        else if (cheDoLocThoiGian == 2) query += " AND date LIKE '%" + thangHienTai + "'";

        query += " ORDER BY id DESC"; // Mới nhất lên đầu

        Cursor cursor = db.rawQuery(query, null);
        double tongThu = 0, tongChi = 0;
        danhSachGiaoDich.clear();
        DecimalFormat formatter = new DecimalFormat("#,###"); // Định dạng có dấu chấm

        // 4. Quét qua dữ liệu trong Database
        if (cursor.moveToFirst()) {
            do {
                int type = cursor.getInt(cursor.getColumnIndex("type"));
                double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                String note = cursor.getString(cursor.getColumnIndex("note"));

                // BƯỚC TÍNH TIỀN: Luôn luôn cộng tổng tiền
                if (type == 1) tongThu += amount;
                else tongChi += amount;

                // BƯỚC VẼ DANH SÁCH: Khớp bộ lọc Loại mới cho vào danh sách
                if (cheDoLocLoai == 0 || cheDoLocLoai == type) {
                    String formattedAmount = formatter.format(amount).replace(",", ".");
                    danhSachGiaoDich.add(((type == 1) ? "[THU] " : "[CHI] ") + note + ": " + formattedAmount + " đ (" + date + ")");
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        // 5. Tính số dư và cập nhật màn hình
        double soDu = tongThu - tongChi;

        // Nhãn thời gian linh hoạt
        String labelThoiGian = (cheDoLocThoiGian == 0) ? "MỌI LÚC" : (cheDoLocThoiGian == 1) ? "HÔM NAY" : "THÁNG NÀY";

        tvTongThu.setText("Tổng thu:\n" + formatter.format(tongThu).replace(",", ".") + " đ");
        tvTongChi.setText("Tổng chi:\n" + formatter.format(tongChi).replace(",", ".") + " đ");
        tvSoDu.setText("SỐ DƯ (" + labelThoiGian + "): " + formatter.format(soDu).replace(",", ".") + " đ");

        // --- 6. ẢO THUẬT UX/UI: ẨN HIỆN THÀNH PHẦN GIAO DIỆN ---
        if (cheDoLocLoai == 1) {
            // Bấm CHỈ THU -> Ẩn Tổng Chi
            tvTongThu.setVisibility(View.VISIBLE);
            tvTongChi.setVisibility(View.GONE);
        } else if (cheDoLocLoai == 2) {
            // Bấm CHỈ CHI -> Ẩn Tổng Thu
            tvTongThu.setVisibility(View.GONE);
            tvTongChi.setVisibility(View.VISIBLE);
        } else {
            // Bấm TẤT CẢ -> Hiện hết
            tvTongThu.setVisibility(View.VISIBLE);
            tvTongChi.setVisibility(View.VISIBLE);

            // CHỈ CẢNH BÁO KHI ĐANG XEM "TẤT CẢ" VÀ CHỈ 1 LẦN
            if (soDu < 0 && !daCanhBao) {
                hienCanhBaoVuotChi();
                daCanhBao = true;
            }
        }

        // LUÔN BÁM TRỤ SỐ DƯ VÀ ĐỔI MÀU NẾU ÂM TIỀN
        tvSoDu.setVisibility(View.VISIBLE);
        if (soDu < 0) {
            tvSoDu.setTextColor(Color.RED);
        } else {
            tvSoDu.setTextColor(Color.parseColor("#1565C0"));
        }

        // Báo cho ListView cập nhật giao diện
        adapter.notifyDataSetChanged();
    }

    // Hàm bung Pop-up Cảnh báo
    private void hienCanhBaoVuotChi() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ CẢNH BÁO");
        builder.setMessage("Bạn đã chi tiêu vượt quá thu nhập!");
        builder.setPositiveButton("Đã hiểu", null);
        builder.show();
    }
}
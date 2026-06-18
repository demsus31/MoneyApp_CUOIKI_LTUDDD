package com.example.moneyapp;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    Button btnThemGiaoDich, btnLocTatCa, btnLocThu, btnLocChi, btnTimKiemNgay, btnTimKiemKhoangNgay, btnProfile, btnBaoCao;
    TextView tvTongThu, tvTongChi, tvSoDu;
    DatabaseHelper dbHelper;
    ListView lvLichSu;
    ArrayList<String> danhSachGiaoDich;
    ArrayAdapter<String> adapter;

    int cheDoLocLoai = 0;     // 0: Tất cả, 1: Thu, 2: Chi
    int cheDoLocThoiGian = 0; // 0: Mọi lúc, 1: Ngày cụ thể, 2: Khoảng ngày

    String ngayTimKiem = "";  // Dùng cho chế độ 1 (Tìm 1 ngày)
    Date ngayBatDau = null;   // Dùng cho chế độ 2 (Từ ngày)
    Date ngayKetThuc = null;  // Dùng cho chế độ 2 (Đến ngày)

    boolean daCanhBao = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnThemGiaoDich = findViewById(R.id.btnThemGiaoDich);
        btnLocTatCa = findViewById(R.id.btnLocTatCa);
        btnLocThu = findViewById(R.id.btnLocThu);
        btnLocChi = findViewById(R.id.btnLocChi);
        btnTimKiemNgay = findViewById(R.id.btnTimKiemNgay);
        btnTimKiemKhoangNgay = findViewById(R.id.btnTimKiemKhoangNgay);
        btnProfile = findViewById(R.id.btnProfile);
        btnBaoCao = findViewById(R.id.btnBaoCao);

        tvTongThu = findViewById(R.id.tvTongThu);
        tvTongChi = findViewById(R.id.tvTongChi);
        tvSoDu = findViewById(R.id.tvSoDu);
        lvLichSu = findViewById(R.id.lvLichSu);

        dbHelper = new DatabaseHelper(this);
        danhSachGiaoDich = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, danhSachGiaoDich);
        lvLichSu.setAdapter(adapter);

        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUser = sharedPref.getString("current_user", "Admin");
        if (currentUser != null && !currentUser.isEmpty()) {
            // Cắt chữ cái đầu tiên và viết hoa lên
            String initial = currentUser.substring(0, 1).toUpperCase();
            btnProfile.setText(initial);
        }

        // chuyển trang
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnBaoCao.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReportActivity.class);
            startActivity(intent);
        });

        btnThemGiaoDich.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddTransactionActivity.class);
            startActivity(intent);
        });

        // các nút lọc
        btnLocTatCa.setOnClickListener(v -> { cheDoLocLoai = 0; cheDoLocThoiGian = 0; capNhatThongKe(); });
        btnLocTatCa.setTextColor(Color.parseColor("#FFFFFF"));
        btnLocThu.setOnClickListener(v -> { cheDoLocLoai = 1; capNhatThongKe(); });
        btnLocChi.setOnClickListener(v -> { cheDoLocLoai = 2; capNhatThongKe(); });

        btnTimKiemNgay.setOnClickListener(v -> hienBangTimKiemNgay());
        btnTimKiemKhoangNgay.setOnClickListener(v -> hienBangTimKiemKhoangNgay());
    }

    @Override
    protected void onResume() {
        super.onResume();
        daCanhBao = false; // reset cảnh báo khi quay lại trang chủ
        capNhatThongKe();
    }

    // gạch chéo cho ngày/tháng/năm
    private void ganHieuUngGachCheo(EditText edt) {
        edt.addTextChangedListener(new TextWatcher() {
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
                edt.setText(formatted.toString());
                edt.setSelection(formatted.length());
                isUpdating = false;
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // tìm kiếm 1 ngày cụ thể
    private void hienBangTimKiemNgay() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📅 Chọn ngày");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(50, 30, 50, 10);

        EditText edtInputDate = new EditText(this);
        edtInputDate.setHint("Nhập ngày (dd/mm/yyyy)");
        edtInputDate.setInputType(InputType.TYPE_CLASS_DATETIME);
        edtInputDate.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ganHieuUngGachCheo(edtInputDate);

        Button btnCalendar = new Button(this);
        btnCalendar.setText("📅");
        btnCalendar.setTextSize(20);
        btnCalendar.setBackgroundColor(Color.TRANSPARENT);

        btnCalendar.setOnClickListener(view -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (dView, y, m, d) -> {
                edtInputDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        layout.addView(edtInputDate);
        layout.addView(btnCalendar);
        builder.setView(layout);

        builder.setPositiveButton("TÌM", (dialog, which) -> {
            String nhapVao = edtInputDate.getText().toString();
            if (nhapVao.length() == 10) {
                ngayTimKiem = nhapVao;
                cheDoLocThoiGian = 1;
                capNhatThongKe();
            } else {
                Toast.makeText(this, "Nhập đủ định dạng dd/mm/yyyy", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("HỦY", null);
        builder.show();
    }

    // tìm khoảng ngày
    private void hienBangTimKiemKhoangNgay() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🗓 Tùy chỉnh khoảng ngày");

        LinearLayout layoutTong = new LinearLayout(this);
        layoutTong.setOrientation(LinearLayout.VERTICAL);
        layoutTong.setPadding(50, 30, 50, 10);

        // Hàng 1: Từ ngày
        LinearLayout layoutTu = new LinearLayout(this);
        layoutTu.setOrientation(LinearLayout.HORIZONTAL);
        EditText edtTuNgay = new EditText(this);
        edtTuNgay.setHint("Từ ngày (dd/mm/yyyy)");
        edtTuNgay.setInputType(InputType.TYPE_CLASS_DATETIME);
        edtTuNgay.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ganHieuUngGachCheo(edtTuNgay);

        Button btnCalTu = new Button(this);
        btnCalTu.setText("📅");
        btnCalTu.setBackgroundColor(Color.TRANSPARENT);
        btnCalTu.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (dView, y, m, d) -> {
                edtTuNgay.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        layoutTu.addView(edtTuNgay);
        layoutTu.addView(btnCalTu);

        // Hàng 2: Đến ngày
        LinearLayout layoutDen = new LinearLayout(this);
        layoutDen.setOrientation(LinearLayout.HORIZONTAL);
        EditText edtDenNgay = new EditText(this);
        edtDenNgay.setHint("Đến ngày (dd/mm/yyyy)");
        edtDenNgay.setInputType(InputType.TYPE_CLASS_DATETIME);
        edtDenNgay.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ganHieuUngGachCheo(edtDenNgay);

        Button btnCalDen = new Button(this);
        btnCalDen.setText("📅");
        btnCalDen.setBackgroundColor(Color.TRANSPARENT);
        btnCalDen.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (dView, y, m, d) -> {
                edtDenNgay.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        layoutDen.addView(edtDenNgay);
        layoutDen.addView(btnCalDen);

        layoutTong.addView(layoutTu);
        layoutTong.addView(layoutDen);
        builder.setView(layoutTong);

        builder.setPositiveButton("TÌM KIẾM", (dialog, which) -> {
            String strTu = edtTuNgay.getText().toString();
            String strDen = edtDenNgay.getText().toString();

            if (strTu.length() == 10 && strDen.length() == 10) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    ngayBatDau = sdf.parse(strTu);
                    ngayKetThuc = sdf.parse(strDen);

                    if (ngayKetThuc.before(ngayBatDau)) {
                        Toast.makeText(this, "Ngày KẾT THÚC phải sau ngày BẮT ĐẦU!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    cheDoLocThoiGian = 2; // Bật chế độ lọc khoảng ngày
                    capNhatThongKe();

                } catch (Exception e) {
                    Toast.makeText(this, "Ngày không hợp lệ", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Vui lòng nhập đủ định dạng 2 ô", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("HỦY", null);
        builder.show();
    }

    // quét dữ liệu + cập nhật giao diện
    @SuppressLint("Range")
    private void capNhatThongKe() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUser = sharedPref.getString("current_user", "");

        // lấy dữ liệu của user
        String query = "SELECT * FROM transactions WHERE username = '" + currentUser + "' ORDER BY id DESC";

        Cursor cursor = db.rawQuery(query, null);
        double tongThu = 0, tongChi = 0;
        danhSachGiaoDich.clear();
        DecimalFormat formatter = new DecimalFormat("#,###");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (cursor.moveToFirst()) {
            do {
                int type = cursor.getInt(cursor.getColumnIndex("type"));
                double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                String note = cursor.getString(cursor.getColumnIndex("note"));

                boolean thoaManThoiGian = true;

                // kiểm tra xem hóa đơn này có khớp với bộ lọc ngày không
                if (cheDoLocThoiGian == 1) { // Chọn 1 ngày
                    if (!date.equals(ngayTimKiem)) thoaManThoiGian = false;
                } else if (cheDoLocThoiGian == 2) { // Chọn khoảng ngày
                    try {
                        Date d = sdf.parse(date);
                        if (d.before(ngayBatDau) || d.after(ngayKetThuc)) thoaManThoiGian = false;
                    } catch (Exception e) { thoaManThoiGian = false; }
                }

                if (thoaManThoiGian) {
                    if (type == 1) tongThu += amount;
                    else tongChi += amount;

                    if (cheDoLocLoai == 0 || cheDoLocLoai == type) {
                        String formattedAmount = formatter.format(amount).replace(",", ".");

                        // Xử lý chuỗi ghi chú (Viết hoa chữ cái đầu)
                        String ghiChu = note;
                        if (note != null && !note.isEmpty()) {
                            ghiChu = note.substring(0, 1).toUpperCase() + note.substring(1).toLowerCase();
                        }

                        // Định dạng Icon và dấu tiền tệ
                        String icon = (type == 1) ? "📈" : "📉";
                        String dau = (type == 1) ? "+" : "-";

                        // Ghép chuỗi hiển thị lên ListView
                        danhSachGiaoDich.add(icon + " " + ghiChu + ": " + dau + " " + formattedAmount + " đ   (" + date + ")");
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        double soDu = tongThu - tongChi;

        // Cập nhật Nhãn thời gian (Tinh chỉnh lại câu chữ)
        String labelThoiGian = "Hiện tại";
        if (cheDoLocThoiGian == 1) {
            labelThoiGian = ngayTimKiem;
        } else if (cheDoLocThoiGian == 2 && ngayBatDau != null && ngayKetThuc != null) {
            labelThoiGian = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(ngayBatDau) + " -> " +
                    new SimpleDateFormat("dd/MM", Locale.getDefault()).format(ngayKetThuc);
        }

        // Cập nhật UI với mã màu chuẩn Material Design
        tvTongThu.setText("📈 Tổng thu:\n" + formatter.format(tongThu).replace(",", ".") + " đ");
        tvTongThu.setTextColor(Color.parseColor("#10B981")); // Màu Xanh Ngọc (Emerald)

        tvTongChi.setText("📉 Tổng chi:\n" + formatter.format(tongChi).replace(",", ".") + " đ");
        tvTongChi.setTextColor(Color.parseColor("#EF4444")); // Màu Đỏ San Hô (Coral)

        tvSoDu.setText("Số dư (" + labelThoiGian + "): " + formatter.format(soDu).replace(",", ".") + " đ");

        // Ẩn hiện các mục dựa vào việc người dùng đang lọc Thu hay lọc Chi
        if (cheDoLocLoai == 1) {
            tvTongThu.setVisibility(View.VISIBLE);
            tvTongChi.setVisibility(View.GONE);
        } else if (cheDoLocLoai == 2) {
            tvTongThu.setVisibility(View.GONE);
            tvTongChi.setVisibility(View.VISIBLE);
        } else {
            tvTongThu.setVisibility(View.VISIBLE);
            tvTongChi.setVisibility(View.VISIBLE);
            if (soDu < 0 && !daCanhBao) {
                hienCanhBaoVuotChi();
                daCanhBao = true;
            }
        }

        tvSoDu.setVisibility(View.VISIBLE);

        // Đổi màu Số dư dựa trên tình trạng âm/dương
        if (soDu < 0) {
            tvSoDu.setTextColor(Color.parseColor("#EF4444")); // Âm: Màu Đỏ
        } else {
            tvSoDu.setTextColor(Color.parseColor("#1565C0")); // Dương: Màu Xanh Dương An Toàn
        }

        adapter.notifyDataSetChanged();
    }

    private void hienCanhBaoVuotChi() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ CẢNH BÁO");
        builder.setMessage("Bạn đã chi tiêu vượt quá thu nhập!");
        builder.setPositiveButton("Đã hiểu", null);
        builder.show();
    }
}
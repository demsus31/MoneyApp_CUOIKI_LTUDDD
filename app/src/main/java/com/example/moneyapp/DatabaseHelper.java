package com.example.moneyapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Khai báo tên cơ sở dữ liệu và phiên bản
    private static final String DATABASE_NAME = "QuanLyTaiChinh.db";
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Hàm onCreate chỉ chạy 1 lần duy nhất khi App được cài đặt lần đầu
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Tạo bảng USERS để lưu thông tin tài khoản đăng nhập
        String createTableUsers = "CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, password TEXT)";
        db.execSQL(createTableUsers);

        // 2. Tạo bảng TRANSACTIONS để lưu lịch sử thu chi
        // LƯU Ý: Có thêm cột 'username' để phân biệt giao dịch này là của ai nhập
        String createTableTransactions = "CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, type INTEGER, amount REAL, date TEXT, note TEXT)";
        db.execSQL(createTableTransactions);
    }

    // Hàm onUpgrade chạy khi ta tăng DATABASE_VERSION (VD: khi muốn cập nhật App)
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS transactions");
        onCreate(db);
    }
}
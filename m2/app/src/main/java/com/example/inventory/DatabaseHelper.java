package com.example.inventory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_INVENTORY = "inventory";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, password TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_INVENTORY + " (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, quantity INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_inventory_name ON " + TABLE_INVENTORY + " (name)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        onCreate(db);
    }

    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        long result = db.insert(TABLE_USERS, null, values);

        if (result == -1) {
            Log.e("DB_ERROR", "User registration failed for: " + username);
            return false;
        } else {
            Log.d("DB_SUCCESS", "User registered: " + username);
            return true;
        }
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE username=? COLLATE NOCASE AND password=?", new String[]{username.trim(), password.trim()});

        boolean exists = cursor.getCount() > 0;
        if (exists) {
            Log.d("DB_SUCCESS", "User found: " + username);
        } else {
            Log.e("DB_ERROR", "User not found: " + username);
        }

        cursor.close();
        return exists;
    }

    public boolean addInventoryItem(String itemName, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", itemName);
        values.put("quantity", quantity);
        long result = db.insert(TABLE_INVENTORY, null, values);

        if (result == -1) {
            Log.e("DB_ERROR", "Failed to add inventory item: " + itemName);
            return false;
        } else {
            Log.d("DB_SUCCESS", "Item added to inventory: " + itemName);
            return true;
        }
    }

    public Cursor getInventoryItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_INVENTORY, null);

        if (cursor.getCount() == 0) {
            Log.e("DB_ERROR", "No inventory items found in database.");
        } else {
            Log.d("DB_SUCCESS", "Inventory items retrieved: " + cursor.getCount());
        }

        return cursor;
    }
    // NEW: map all inventory rows to List<Item>
    public java.util.List<com.example.inventory.model.Item> getAllItems() {
        java.util.List<com.example.inventory.model.Item> list = new java.util.ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor c = db.rawQuery("SELECT id, name, quantity FROM " + TABLE_INVENTORY, null)) {

            int iId = c.getColumnIndexOrThrow("id");
            int iName = c.getColumnIndexOrThrow("name");
            int iQty = c.getColumnIndexOrThrow("quantity");

            while (c.moveToNext()) {
                list.add(new com.example.inventory.model.Item(
                        c.getLong(iId),
                        c.getString(iName),
                        c.getInt(iQty)
                ));
            }
        }
        return list;
    }

    // NEW: search by name (parameterized) -> List<Item>
    public java.util.List<com.example.inventory.model.Item> searchByName(String query) {
        java.util.List<com.example.inventory.model.Item> list = new java.util.ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor c = db.rawQuery(
                     "SELECT id, name, quantity FROM " + TABLE_INVENTORY + " WHERE name LIKE ?",
                     new String[]{"%" + query + "%"})) {

            int iId = c.getColumnIndexOrThrow("id");
            int iName = c.getColumnIndexOrThrow("name");
            int iQty = c.getColumnIndexOrThrow("quantity");

            while (c.moveToNext()) {
                list.add(new com.example.inventory.model.Item(
                        c.getLong(iId),
                        c.getString(iName),
                        c.getInt(iQty)
                ));
            }
        }
        return list;
    }
}

package com.example.inventory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 2; // was 1 → bump for migration

    private static final String TABLE_USERS = "users";
    private static final String TABLE_INVENTORY = "inventory";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, password TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS inventory (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, quantity INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_inventory_name ON inventory(name)");

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS transactions (" +
                        " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " item_id INTEGER NOT NULL," +
                        " change_type TEXT NOT NULL," +     // INSERT / UPDATE / DELETE
                        " old_quantity INTEGER," +
                        " new_quantity INTEGER," +
                        " changed_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))," +
                        " FOREIGN KEY(item_id) REFERENCES inventory(id) ON DELETE CASCADE)"
        );

        db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS trg_inventory_insert AFTER INSERT ON inventory " +
                        "BEGIN INSERT INTO transactions(item_id,change_type,old_quantity,new_quantity) " +
                        "VALUES (NEW.id,'INSERT',NULL,NEW.quantity); END;"
        );
        db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS trg_inventory_update AFTER UPDATE ON inventory " +
                        "BEGIN INSERT INTO transactions(item_id,change_type,old_quantity,new_quantity) " +
                        "VALUES (NEW.id,'UPDATE',OLD.quantity,NEW.quantity); END;"
        );
        db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS trg_inventory_delete AFTER DELETE ON inventory " +
                        "BEGIN INSERT INTO transactions(item_id,change_type,old_quantity,new_quantity) " +
                        "VALUES (OLD.id,'DELETE',OLD.quantity,NULL); END;"
        );
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // add index in case it was missing
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_inventory_name ON inventory(name)");

            // create transactions table and triggers
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS transactions (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "item_id INTEGER NOT NULL," +
                            "change_type TEXT NOT NULL, " +
                            "old_quantity INTEGER, " +
                            "new_quantity INTEGER, " +
                            "changed_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))," +
                            "FOREIGN KEY(item_id) REFERENCES inventory(id) ON DELETE CASCADE" +
                            ")"
            );
            db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS trg_inventory_insert " +
                            "AFTER INSERT ON inventory " +
                            "BEGIN " +
                            "  INSERT INTO transactions(item_id, change_type, old_quantity, new_quantity) " +
                            "  VALUES (NEW.id, 'INSERT', NULL, NEW.quantity); " +
                            "END;"
            );
            db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS trg_inventory_update " +
                            "AFTER UPDATE ON inventory " +
                            "BEGIN " +
                            "  INSERT INTO transactions(item_id, change_type, old_quantity, new_quantity) " +
                            "  VALUES (NEW.id, 'UPDATE', OLD.quantity, NEW.quantity); " +
                            "END;"
            );
            db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS trg_inventory_delete " +
                            "AFTER DELETE ON inventory " +
                            "BEGIN " +
                            "  INSERT INTO transactions(item_id, change_type, old_quantity, new_quantity) " +
                            "  VALUES (OLD.id, 'DELETE', OLD.quantity, NULL); " +
                            "END;"
            );
        }
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
    // --- Algorithms & DS Enhancement: Batch insert in a single transaction ---
    public void insertItemsBatch(java.util.List<com.example.inventory.model.Item> items) {
        if (items == null || items.isEmpty()) return;

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (com.example.inventory.model.Item it : items) {
                ContentValues v = new ContentValues();
                v.put("name", it.getName());
                v.put("quantity", it.getQuantity());
                db.insert("inventory", null, v); // insert each row
            }
            db.setTransactionSuccessful(); // one fsync for the whole batch
        } finally {
            db.endTransaction();
        }
    }
    public java.util.List<com.example.inventory.model.Item> getItemsPage(int limit, int offset) {
        java.util.List<com.example.inventory.model.Item> list = new java.util.ArrayList<>();
        if (limit <= 0) limit = 50;
        if (offset < 0) offset = 0;

        // try-with-resources to ensure cursor closes
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor c = db.rawQuery(
                     "SELECT id, name, quantity FROM inventory ORDER BY name LIMIT ? OFFSET ?",
                     new String[]{ String.valueOf(limit), String.valueOf(offset) })) {

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
    // Get a single item by ID (parameterized + try-with-resources)
    public com.example.inventory.model.Item getItemById(long id) {
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor c = db.rawQuery(
                     "SELECT id, name, quantity FROM inventory WHERE id = ? LIMIT 1",
                     new String[]{ String.valueOf(id) })) {

            if (c.moveToFirst()) {
                int iId = c.getColumnIndexOrThrow("id");
                int iName = c.getColumnIndexOrThrow("name");
                int iQty = c.getColumnIndexOrThrow("quantity");
                return new com.example.inventory.model.Item(
                        c.getLong(iId),
                        c.getString(iName),
                        c.getInt(iQty)
                );
            }
            return null;
        }
    }

    // Save item: update if id>0, else insert and return new rowId
    public long saveItem(com.example.inventory.model.Item item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("name", item.getName());
        v.put("quantity", item.getQuantity());

        if (item.getId() > 0) {
            int updated = db.update("inventory", v, "id = ?", new String[]{ String.valueOf(item.getId()) });
            // If nothing was updated (shouldn’t happen unless id disappeared), fall back to insert:
            if (updated > 0) return item.getId();
        }
        return db.insert("inventory", null, v);
    }

    public void upsertItemsBatch(java.util.List<com.example.inventory.model.Item> items) {
        if (items == null || items.isEmpty()) return;

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (com.example.inventory.model.Item it : items) {
                ContentValues v = new ContentValues();
                v.put("name", it.getName());
                v.put("quantity", it.getQuantity());

                int updated = db.update("inventory", v, "id = ?", new String[]{ String.valueOf(it.getId()) });
                if (updated == 0) {
                    db.insert("inventory", null, v);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    public java.util.List<String> getTransactionLog(int limit, int offset) {
        java.util.List<String> log = new java.util.ArrayList<>();
        if (limit <= 0) limit = 50;
        if (offset < 0) offset = 0;

        String sql =
                "SELECT t.id, t.item_id, i.name AS item_name, t.change_type, " +
                        "       t.old_quantity, t.new_quantity, t.changed_at " +
                        "FROM transactions t " +
                        "LEFT JOIN inventory i ON i.id = t.item_id " +
                        "ORDER BY t.changed_at DESC " +
                        "LIMIT ? OFFSET ?";

        try (SQLiteDatabase db = getReadableDatabase();
             android.database.Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(limit), String.valueOf(offset) })) {

            int iId = c.getColumnIndexOrThrow("id");
            int iItemId = c.getColumnIndexOrThrow("item_id");
            int iName = c.getColumnIndexOrThrow("item_name");
            int iType = c.getColumnIndexOrThrow("change_type");
            int iOld = c.getColumnIndexOrThrow("old_quantity");
            int iNew = c.getColumnIndexOrThrow("new_quantity");
            int iTs  = c.getColumnIndexOrThrow("changed_at");

            while (c.moveToNext()) {
                String line = "#" + c.getLong(iId) +
                        " • item=" + c.getLong(iItemId) +
                        " (" + c.getString(iName) + ")" +
                        " • " + c.getString(iType) +
                        " • old=" + c.getString(iOld) +
                        " • new=" + c.getString(iNew) +
                        " • ts=" + c.getLong(iTs);
                log.add(line);
            }
        }
        return log;
    }

    public java.io.File exportInventoryToCsv(android.content.Context ctx) throws java.io.IOException {
        java.io.File dir = ctx.getExternalFilesDir(null); // app-specific external
        if (dir == null) throw new java.io.IOException("No external files dir available");
        java.io.File out = new java.io.File(dir, "inventory_export.csv");

        try (java.io.FileWriter fw = new java.io.FileWriter(out);
             SQLiteDatabase db = getReadableDatabase();
             Cursor c = db.rawQuery("SELECT id, name, quantity FROM inventory ORDER BY name", null)) {

            fw.write("id,name,quantity\n");
            int iId = c.getColumnIndexOrThrow("id");
            int iName = c.getColumnIndexOrThrow("name");
            int iQty = c.getColumnIndexOrThrow("quantity");

            while (c.moveToNext()) {
                long id = c.getLong(iId);
                String name = c.getString(iName);
                int qty = c.getInt(iQty);
                // basic CSV escaping for commas/quotes
                String safeName = name == null ? "" : "\"" + name.replace("\"", "\"\"") + "\"";
                fw.write(id + "," + safeName + "," + qty + "\n");
            }
            fw.flush();
        }
        return out;
    }

    // Delete a row by id
    public int deleteItem(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("inventory", "id = ?", new String[]{ String.valueOf(id) });
    }


}


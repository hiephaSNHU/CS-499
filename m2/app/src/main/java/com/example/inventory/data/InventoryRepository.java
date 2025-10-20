package com.example.inventory.data;

import android.content.Context;

import com.example.inventory.DatabaseHelper;
import com.example.inventory.model.Item;

import java.util.List;

public class InventoryRepository {
    private final DatabaseHelper db;

    public InventoryRepository(Context context) {
        // Use app context to avoid leaking an Activity
        this.db = new DatabaseHelper(context.getApplicationContext());
    }

    // ---- Inventory operations ----

    public boolean addItem(String name, int quantity) {
        // Delegates to your existing helper
        return db.addInventoryItem(name, quantity);
    }

    public List<Item> getAllItems() {
        return db.getAllItems();
    }

    public List<Item> searchByName(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllItems();
        }
        return db.searchByName(query.trim());
    }

    // ---- (Optional) User operations you already have ----

    public boolean registerUser(String username, String password) {
        return db.registerUser(username, password);
    }

    public boolean checkUser(String username, String password) {
        return db.checkUser(username, password);
    }
}

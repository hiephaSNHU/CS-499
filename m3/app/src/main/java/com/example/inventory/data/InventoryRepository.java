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
    public void insertItemsBatch(java.util.List<com.example.inventory.model.Item> items) {
        db.insertItemsBatch(items);
    }
    // --- CS-499 Algorithms & DS: small LRU-ish cache for O(1) average lookups by ID ---
    private final java.util.LinkedHashMap<Long, com.example.inventory.model.Item> cacheById =
            new java.util.LinkedHashMap<Long, com.example.inventory.model.Item>(128, 0.75f, true) {
                private static final int MAX = 256; // cap memory use
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Long, com.example.inventory.model.Item> eldest) {
                    return size() > MAX;
                }
            };

    // Read-through cache
    public com.example.inventory.model.Item getById(long id) {
        com.example.inventory.model.Item cached = cacheById.get(id);
        if (cached != null) return cached;

        com.example.inventory.model.Item fromDb = db.getItemById(id);
        if (fromDb != null) cacheById.put(id, fromDb);
        return fromDb;
    }

    // Save and refresh cache
    public long save(com.example.inventory.model.Item item) {
        long id = db.saveItem(item);
        // If insert returned a new id, mirror it; if update, keep same id.
        long finalId = (item.getId() > 0) ? item.getId() : id;

        // Create a cached copy reflecting the saved state
        com.example.inventory.model.Item cached = new com.example.inventory.model.Item(
                finalId, item.getName(), item.getQuantity()
        );
        cacheById.put(finalId, cached);
        return finalId;
    }

    // Optional helpers
    public void clearCache() { cacheById.clear(); }
    public java.util.List<com.example.inventory.model.Item> getItemsPage(int page, int pageSize) {
        if (pageSize <= 0) pageSize = 50;
        if (page < 0) page = 0;
        int offset = page * pageSize;
        return db.getItemsPage(pageSize, offset);
    }

    public void upsertItemsBatch(java.util.List<Item> items) {
        db.upsertItemsBatch(items);
        for (Item it : items) {
            cacheById.put(it.getId(), it); // refresh cache
        }
    }

}

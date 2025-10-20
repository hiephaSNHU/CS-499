package com.example.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;

import com.example.inventory.data.InventoryRepository;
import com.example.inventory.model.Item;
import com.example.inventory.ui.ItemAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class DatabaseDisplayActivity extends AppCompatActivity {

    private InventoryRepository repo;
    private ItemAdapter adapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    private static final int REQ_SMS = 42;

    private final ActivityResultLauncher<Intent> addItemLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadAll(); // reload after adding
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        repo = new InventoryRepository(this);

        // RecyclerView
        RecyclerView rv = findViewById(R.id.rvInventory); // ensure this ID exists in activity_display.xml
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ItemAdapter(new ItemAdapter.Listener() {
            @Override public void onClick(com.example.inventory.model.Item item) {
                showEditDialog(item);   // tap to edit
            }
            @Override public void onLongClick(com.example.inventory.model.Item item) {
                confirmDelete(item);    // long-press to delete
            }
        });
        rv.setAdapter(adapter);

        refreshList();

        // Initial page load
        List<Item> page0 = repo.getItemsPage(0, 50);
        adapter.submitList(page0);

        // Buttons
        Button btnAddNewItem = findViewById(R.id.btnAddNewItem);
        if (btnAddNewItem != null) {
            btnAddNewItem.setOnClickListener(v -> {
                Intent intent = new Intent(DatabaseDisplayActivity.this, AddInventoryActivity.class);
                addItemLauncher.launch(intent);
            });
        }

        Button btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v ->
                    startActivity(new Intent(DatabaseDisplayActivity.this,
                            com.example.inventory.ui.SettingsActivity.class)));
        }

        // Optional: full reload & alerts (kept from your original)
        loadAll(); // this will also send SMS alerts based on prefs

        // Search with debounce
        EditText etSearch = findViewById(R.id.etSearch); // make sure this exists in activity_display.xml
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String q = s.toString().trim();
                    if (pendingSearch != null) handler.removeCallbacks(pendingSearch);

                    pendingSearch = () -> {
                        List<Item> results = q.isEmpty()
                                ? repo.getItemsPage(0, 50)
                                : repo.searchByName(q);
                        adapter.submitList(results);
                    };
                    handler.postDelayed(pendingSearch, 350); // debounce
                }
            });
        }
    }

    private void loadAll() {
        // If you have repo.getAllItems(), use it; otherwise keep a page to bound work.
        List<Item> items;
        try {
            items = repo.getAllItems(); // if this exists in your repo
        } catch (Throwable t) {
            items = repo.getItemsPage(0, 100); // fallback: a larger first page
        }
        adapter.submitList(items);
        sendLowStockAlerts(items);
    }

    // ----- Menu (unchanged) -----
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_display, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        // ⬇️ ADD THIS under your other if/else menu cases:
        if (id == R.id.action_export_csv) {
            try {
                android.net.Uri uri = repo.exportInventoryToDownloads(this);
                String msg = "Exported to Downloads: " + uri.getLastPathSegment();
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();

                // Optional: let user open it right away
                android.content.Intent view = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                view.setDataAndType(uri, "text/csv");
                view.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(view);
                } catch (Exception ignored) { }
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Export failed: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            }
            return true;
        }
        else if (id == R.id.action_view_log) {
            java.util.List<String> log = repo.getTransactionLog(0, 10); // first 10 entries
            String msg = log.isEmpty()
                    ? "No changes logged yet."
                    : android.text.TextUtils.join("\n\n", log);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Recent Changes")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ----- Preferences for alerts -----
    private String getAlertPhone() {
        return androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString("alert_phone", ""); // empty default
    }

    private int getLowStockThreshold() {
        String val = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString("low_stock_threshold", "5");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 5; // safe fallback
        }
    }

    private boolean ensureSmsPermission() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.SEND_SMS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.SEND_SMS},
                    REQ_SMS
            );
            return false;
        }
        return true;
    }

    private void sendLowStockAlerts(List<Item> items) {
        String phone = getAlertPhone();
        int threshold = getLowStockThreshold();

        if (phone.isEmpty()) return;
        if (!ensureSmsPermission()) return;

        SmsHelper smsHelper = new SmsHelper(this);
        for (Item it : items) {
            if (it.getQuantity() < threshold) {
                try {
                    smsHelper.sendSMS(phone, "Low stock alert: " + it.getName() + " is running low!");
                } catch (Exception ignored) { }
            }
        }
    }
    private void showEditDialog(com.example.inventory.model.Item item) {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_item, null);
        EditText etName = view.findViewById(R.id.etName);  // <-- note: view.findViewById
        EditText etQty  = view.findViewById(R.id.etQty);

        etName.setText(item.getName());
        etQty.setText(String.valueOf(item.getQuantity()));

        new AlertDialog.Builder(this)                      // <-- androidx.appcompat.app.AlertDialog
                .setTitle("Edit Item")
                .setView(view)                                 // <-- setView(View)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String qStr = etQty.getText().toString().trim();
                    if (name.isEmpty()) {
                        android.widget.Toast.makeText(this, "Name required", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int qty;
                    try { qty = Integer.parseInt(qStr); }
                    catch (NumberFormatException e) {
                        android.widget.Toast.makeText(this, "Quantity must be a number", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

                    com.example.inventory.model.Item updated =
                            new com.example.inventory.model.Item(item.getId(), name, qty);

                    long res = repo.saveItem(updated);         // <-- needs wrapper (step 3)
                    if (res > 0) {
                        refreshList();
                        android.widget.Toast.makeText(this, "Saved", android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        android.widget.Toast.makeText(this, "Save failed", android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(com.example.inventory.model.Item item) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Delete \"" + item.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    boolean ok = repo.deleteItem(item.getId());
                    if (ok) {
                        refreshList();
                        android.widget.Toast.makeText(this, "Deleted", android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        android.widget.Toast.makeText(this, "Delete failed", android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshList() {
        java.util.List<com.example.inventory.model.Item> items = repo.getAllItems();
        // submit a new list instance so DiffUtil notices changes
        adapter.submitList(new java.util.ArrayList<>(items));
    }

}

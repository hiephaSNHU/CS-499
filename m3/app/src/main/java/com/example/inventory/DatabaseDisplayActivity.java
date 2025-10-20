package com.example.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;

import com.example.inventory.data.InventoryRepository;
import com.example.inventory.model.Item;
import com.example.inventory.ui.ItemAdapter;

import java.util.List;

public class DatabaseDisplayActivity extends AppCompatActivity {

    private InventoryRepository repo;
    private ItemAdapter adapter;

    private Handler handler = new Handler(Looper.getMainLooper());
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

        repo = new InventoryRepository(this);

        // RecyclerView
        RecyclerView rv = findViewById(R.id.rvInventory); // ensure this ID exists in activity_display.xml
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ItemAdapter(item -> {
            // handle row click if desired
        });
        rv.setAdapter(adapter);

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
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, com.example.inventory.ui.SettingsActivity.class));
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
}

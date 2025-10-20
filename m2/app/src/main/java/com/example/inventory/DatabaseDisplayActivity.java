package com.example.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory.data.InventoryRepository;
import com.example.inventory.model.Item;
import com.example.inventory.ui.ItemAdapter;

import java.util.List;

public class DatabaseDisplayActivity extends AppCompatActivity {
    private InventoryRepository repo;
    private ItemAdapter adapter;

    private final ActivityResultLauncher<Intent> addItemLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadAll();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        // Repo + RecyclerView setup
        repo = new InventoryRepository(this);
        RecyclerView rv = findViewById(R.id.rvInventory);
        adapter = new ItemAdapter();
        rv.setAdapter(adapter);

        // Add button (kept from your original)
        Button btnAddNewItem = findViewById(R.id.btnAddNewItem);
        btnAddNewItem.setOnClickListener(v -> {
            Intent intent = new Intent(DatabaseDisplayActivity.this, AddInventoryActivity.class);
            addItemLauncher.launch(intent);
        });

        Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(DatabaseDisplayActivity.this,
                        com.example.inventory.ui.SettingsActivity.class)));
        // Initial load
        loadAll();
    }

    private void loadAll() {
        List<Item> items = repo.getAllItems();
        adapter.submitList(items);
        // send alerts based on user settings
        sendLowStockAlerts(items);
    }
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
    private String getAlertPhone() {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
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
    private static final int REQ_SMS = 42;

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

    private void sendLowStockAlerts(java.util.List<com.example.inventory.model.Item> items) {
        String phone = getAlertPhone();
        int threshold = getLowStockThreshold();

        if (phone.isEmpty()) {
            // no phone configured → skip alerts
            return;
        }
        if (!ensureSmsPermission()) return;

        SmsHelper smsHelper = new SmsHelper(this); // your existing helper
        for (com.example.inventory.model.Item it : items) {
            if (it.quantity < threshold) {
                try {
                    smsHelper.sendSMS(phone, "Low stock alert: " + it.name + " is running low!");
                } catch (Exception e) {
                    // keep it quiet; you can add a Toast if you want
                }
            }
        }
    }

}

package com.example.inventory;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventory.data.InventoryRepository;

public class AddInventoryActivity extends AppCompatActivity {
    private EditText etItemName, etItemQuantity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_inventory);

        etItemName = findViewById(R.id.etItemName);
        etItemQuantity = findViewById(R.id.etItemQuantity);
        Button btnAddItem = findViewById(R.id.btnAddItem);

        // Single, validated save path
        btnAddItem.setOnClickListener(v -> onSaveClicked());
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void onSaveClicked() {
        String name = etItemName.getText().toString().trim();
        String qtyStr = etItemQuantity.getText().toString().trim();

        if (name.isEmpty()) {
            showError("Item name is required");
            return;
        }
        if (qtyStr.isEmpty()) {
            showError("Quantity is required");
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            showError("Please enter a valid number");
            return;
        }

        if (qty < 0) {
            showError("Quantity must be non-negative");
            return;
        }

        // Use the repository to insert
        InventoryRepository repo = new InventoryRepository(this);
        boolean ok = repo.addItem(name, qty);

        if (!ok) {
            showError("Failed to add item");
            return;
        }

        Toast.makeText(this, "Item added successfully!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}

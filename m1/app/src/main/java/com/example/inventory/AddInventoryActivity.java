package com.example.inventory;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddInventoryActivity extends AppCompatActivity {
    private EditText etItemName, etItemQuantity;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_inventory);

        etItemName = findViewById(R.id.etItemName);
        etItemQuantity = findViewById(R.id.etItemQuantity);
        Button btnAddItem = findViewById(R.id.btnAddItem);
        dbHelper = new DatabaseHelper(this);

        btnAddItem.setOnClickListener(v -> {
            String itemName = etItemName.getText().toString().trim();
            String quantityStr = etItemQuantity.getText().toString().trim();

            if (itemName.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(AddInventoryActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int quantity = Integer.parseInt(quantityStr);
            boolean inserted = dbHelper.addInventoryItem(itemName, quantity);

            if (inserted) {
                Toast.makeText(AddInventoryActivity.this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(AddInventoryActivity.this, "Failed to add item", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

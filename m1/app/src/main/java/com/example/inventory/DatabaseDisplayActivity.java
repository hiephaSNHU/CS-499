package com.example.inventory;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class DatabaseDisplayActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private TextView tvInventoryList;

    private final ActivityResultLauncher<Intent> addItemLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    displayInventory();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        tvInventoryList = findViewById(R.id.tvInventoryList);
        Button btnAddNewItem = findViewById(R.id.btnAddNewItem);
        dbHelper = new DatabaseHelper(this);

        displayInventory();

        btnAddNewItem.setOnClickListener(v -> {
            Intent intent = new Intent(DatabaseDisplayActivity.this, AddInventoryActivity.class);
            addItemLauncher.launch(intent);
        });
    }

    private void displayInventory() {
        Cursor cursor = dbHelper.getInventoryItems();
        StringBuilder sb = new StringBuilder();
        SmsHelper smsHelper = new SmsHelper(this);

        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                int quantity = cursor.getInt(2);

                sb.append("ID: ").append(id)
                        .append(", Name: ").append(name)
                        .append(", Quantity: ").append(quantity)
                        .append("\n");

                if (quantity < 5) {  // Adjust this threshold as needed
                    smsHelper.sendSMS("+1234567890", "Low stock alert: " + name + " is running low!");
                }
            }
        } else {
            sb.append("No inventory items found.");
        }

        cursor.close();
        tvInventoryList.setText(sb.toString());
    }
}

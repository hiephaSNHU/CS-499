package com.example.inventory;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class SmsHelper {
    private final Context context;

    public SmsHelper(Context context) {
        this.context = context;
    }

    public void sendSMS(String phoneNumber, String message) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager;
            smsManager = context.getSystemService(SmsManager.class);
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(context, "SMS Sent!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "SMS Permission Not Granted", Toast.LENGTH_SHORT).show();
        }
    }
}

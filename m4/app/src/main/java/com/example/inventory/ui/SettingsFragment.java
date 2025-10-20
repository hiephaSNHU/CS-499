package com.example.inventory.ui;

import android.os.Bundle;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import com.example.inventory.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);

        // Make threshold numeric
        EditTextPreference threshold = findPreference("low_stock_threshold");
        if (threshold != null) {
            threshold.setOnBindEditTextListener(et ->
                    et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER));
        }
    }
}

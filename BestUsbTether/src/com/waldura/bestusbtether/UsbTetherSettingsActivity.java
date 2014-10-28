package com.waldura.bestusbtether;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * From
 * https://github.com/android/platform_packages_apps_settings/blob/master/src/com/android/settings/Settings.java 
 * 
 * @author ren+android@waldura.com
 */
public class UsbTetherSettingsActivity extends PreferenceActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new UsbTetherSettings())
                .commit();
    }
}

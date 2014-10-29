package com.waldura.bestusbtether;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

/**
 * Mostly lifted from
 * https://github.com/android/platform_packages_apps_settings/blob/master/src/com/android/settings/TetherSettings.java
 * 
 * @author ren+android@waldura.com
 */
public class UsbTetherSettings extends PreferenceFragment
{
    private static final String TAG = "UsbTetherSettings";

    private static final String USB_TETHER_SETTINGS = "usb_tether_settings";
    private static final String USB_TETHER_AUTOSTART = "usb_tether_auto";
    
    private UsbConnectivityManager ucm;

    private boolean mMassStorageActive;
    private boolean mUsbConnected;

    private String[] mUsbRegexs = new String[] {};
    private TetherChangeReceiver mTetherChangeReceiver;

    private CheckBoxPreference mUsbTether;
    private CheckBoxPreference mUsbAutoStart;

    /**
     * Called when activity is first initialized.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.tether_prefs);
        
        // initialize preference default values
        PreferenceManager.setDefaultValues(getActivity().getApplicationContext(), R.xml.tether_prefs, false);

        mUsbTether = (CheckBoxPreference) findPreference(USB_TETHER_SETTINGS);
        mUsbAutoStart = (CheckBoxPreference) findPreference(USB_TETHER_AUTOSTART);        

        // initialize our wrapper
        ucm = new UsbConnectivityManager(getActivity().getApplicationContext());
        if (ucm.hasAllTetheringPermissions(getActivity().getApplicationContext()))
        {
            Log.i(TAG, "all required permissions obtained, good to go");
        }
        else
        {
            Log.e(TAG, "do not have required permissions, will fail");
            // TODO signal this somehow 
        }

        if (!isUsbAvailable())
        {
            // there's nothing to do here; this device has no USB capability
            // getPreferenceScreen().removePreference(mUsbTether);
            mUsbTether.setEnabled(false);
            mUsbTether.setSummary(R.string.usb_tethering_nousbavailable_subtext);
            Log.e(TAG, "no USB available");             
        }
    }

    private boolean isUsbAvailable()
    {
        boolean usbAvailable = false;
        
        try
        {
            mUsbRegexs = ucm.getTetherableUsbRegexs();
            usbAvailable = mUsbRegexs.length > 0;
        }
        catch (NoSuchMethodException e)
        {
            Log.w(TAG, "getTetherableUsbRegexs failed", e);
        }
        catch (IllegalAccessException e)
        {
            Log.w(TAG, "getTetherableUsbRegexs failed", e);
        }
        catch (IllegalArgumentException e)
        {
            Log.w(TAG, "getTetherableUsbRegexs failed", e);
        }
        catch (InvocationTargetException e)
        {
            Log.w(TAG, "getTetherableUsbRegexs failed", e.getTargetException());
        }
        
        return usbAvailable;
    }

    private void setUsbTethering(boolean enabled)
    {        
        Integer subtext = R.string.usb_tethering_errored_subtext;        
        try
        {
            if (ucm.setUsbTethering(enabled) == UsbConnectivityManager.TETHER_ERROR_NO_ERROR)
            {
                mUsbTether.setChecked(enabled);
                subtext = null;
                Log.i(TAG, "USB tethering set to " + enabled);
            }                
        }
        catch (NoSuchMethodException e)
        {
            Log.e(TAG, "setUsbTethering failed", e);
        }
        catch (IllegalAccessException e)
        {
            Log.e(TAG, "setUsbTethering failed", e);
        }
        catch (IllegalArgumentException e)
        {
            Log.e(TAG, "setUsbTethering failed", e);
        }
        catch (InvocationTargetException e)
        {            
            Log.e(TAG, "setUsbTethering failed", e.getTargetException());                

            if (e.getTargetException() instanceof SecurityException)
            {
                subtext = R.string.usb_tethering_errored_noperm;
            }
        }

        if (subtext != null)
        {
            mUsbTether.setSummary(subtext);            
        }
        else
        {
            // success; tethering can sometime take a while
            mUsbTether.setSummary("...");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference)
    {
        if (preference == mUsbTether)
        {
            Log.i(TAG, "user changed USB tethering to " + mUsbTether.isChecked());
            setUsbTethering(mUsbTether.isChecked());
        }
        else if (preference == mUsbAutoStart)
        {
            Log.d(TAG, "user changed auto start to " + mUsbAutoStart.isChecked());
        }
        
        return super.onPreferenceTreeClick(screen, preference);
    }
 
    private class TetherChangeReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context content, Intent intent)
        {
            Log.d(TAG, "received new intent " + intent.getAction());

            if (intent.getAction().equals(UsbConnectivityManager.ACTION_TETHER_STATE_CHANGED))
            {
                // TODO - this should understand the interface types
                ArrayList<String> available = intent.getStringArrayListExtra(UsbConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(UsbConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent.getStringArrayListExtra(UsbConnectivityManager.EXTRA_ERRORED_TETHER);
                
                updateState(
                        available.toArray(new String[available.size()]), 
                        active.toArray(new String[active.size()]), 
                        errored.toArray(new String[errored.size()]));
            }
            else if (intent.getAction().equals(UsbConnectivityManager.ACTION_MEDIA_SHARED))
            {
                mMassStorageActive = true;
                updateState(null, null, null);
            }
            else if (intent.getAction().equals(UsbConnectivityManager.ACTION_MEDIA_UNSHARED))
            {
                mMassStorageActive = false;
                updateState(null, null, null);
            }
            else if (intent.getAction().equals(UsbConnectivityManager.ACTION_USB_STATE))
            {                
                boolean usbConnected = intent.getBooleanExtra(UsbConnectivityManager.USB_CONNECTED, false);                
                Log.i(TAG, "intent " + intent.getAction() + " usbConnect is " + usbConnected);
                
                if (usbConnected // no point in auto-stopping USB tethering: it already does
                        && !mUsbTether.isChecked() // not already started
                        && mUsbAutoStart.isChecked())
                {
                    Log.w(TAG, "auto-starting USB tethering");
                    setUsbTethering(true);
                }
                
                mUsbConnected = usbConnected;
                updateState(null, null, null);
            }
        }
    }

    /**
     * Activity is shown to user. 
     */
    @Override
    public void onStart()
    {
        super.onStart();
        
        mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());

        mTetherChangeReceiver = new TetherChangeReceiver();

        IntentFilter filter = new IntentFilter(UsbConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        Intent intent = getActivity().registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(UsbConnectivityManager.ACTION_USB_STATE);
        getActivity().registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(UsbConnectivityManager.ACTION_MEDIA_SHARED);
        filter.addAction(UsbConnectivityManager.ACTION_MEDIA_UNSHARED);
        filter.addDataScheme("file");
        getActivity().registerReceiver(mTetherChangeReceiver, filter);

        if (intent != null)
            mTetherChangeReceiver.onReceive(getActivity(), intent);

        // init the UI with actual, current values
        updateState(null, null, null);
    }

    @Override
    public void onStop()
    {
        getActivity().unregisterReceiver(mTetherChangeReceiver);
        mTetherChangeReceiver = null;        
        super.onStop();
    }

    private void updateState(String[] available, String[] tethered, String[] errored)
    {
        try
        {
            if (available == null && tethered == null && errored == null)
            {
                available = ucm.getTetherableIfaces();
                tethered = ucm.getTetheredIfaces();
                errored = ucm.getTetheringErroredIfaces();
            }
                        
            updateUsbState(available, tethered, errored);
        }
        catch (NoSuchMethodException e)
        {
            Log.e(TAG, "failed to update state", e);
        }
        catch (IllegalAccessException e)
        {
            Log.e(TAG, "failed to update state", e);
        }
        catch (IllegalArgumentException e)
        {
            Log.e(TAG, "failed to update state", e);
        }
        catch (InvocationTargetException e)
        {
            Log.e(TAG, "failed to update state", e.getTargetException());
        }
    }

    private void updateUsbState(String[] available, String[] tethered, String[] errored) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        boolean usbAvailable = mUsbConnected && !mMassStorageActive;
        int usbError = UsbConnectivityManager.TETHER_ERROR_NO_ERROR;
        for (String s : available)
        {
            for (String regex : mUsbRegexs)
            {
                if (s.matches(regex))
                {
                    if (usbError == UsbConnectivityManager.TETHER_ERROR_NO_ERROR)
                    {
                        usbError = ucm.getLastTetherError(s);
                    }
                }
            }
        }
        boolean usbTethered = false;
        for (String s : tethered)
        {
            for (String regex : mUsbRegexs)
            {
                if (s.matches(regex))
                    usbTethered = true;
            }
        }
        boolean usbErrored = false;
        for (String s : errored)
        {
            for (String regex : mUsbRegexs)
            {
                if (s.matches(regex))
                    usbErrored = true;
            }
        }
        if (usbTethered)
        {
            mUsbTether.setSummary(R.string.usb_tethering_active_subtext);
            mUsbTether.setEnabled(true);
            mUsbTether.setChecked(true);
        }
        else if (usbAvailable)
        {
            if (usbError == UsbConnectivityManager.TETHER_ERROR_NO_ERROR)
            {
                mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
            }
            else
            {
                mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            }
            mUsbTether.setEnabled(true);
            mUsbTether.setChecked(false);
        }
        else if (usbErrored)
        {
            mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        }
        else if (mMassStorageActive)
        {
            mUsbTether.setSummary(R.string.usb_tethering_storage_active_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        }
        else
        {
            mUsbTether.setSummary(R.string.usb_tethering_unavailable_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        }
    }

}

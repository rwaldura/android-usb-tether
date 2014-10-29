package com.waldura.bestusbtether;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import com.waldura.bestusbtether.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
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
    static final String TAG = "bestusbtether";

    private static final String USB_TETHER_SETTINGS = "usb_tether_settings";
    
    private UsbConnectivityManager ucm;

    private boolean mMassStorageActive;
    private boolean mUsbConnected;

    private String[] mUsbRegexs = new String[] {};
    private TetherChangeReceiver mTetherChangeReceiver;

    private CheckBoxPreference mUsbTether;

    /**
     * Called when activity is first initialized.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tether_prefs);
        
        ucm = new UsbConnectivityManager(getActivity().getApplicationContext());

        boolean usbAvailable = false;
        try
        {
            mUsbRegexs = ucm.getTetherableUsbRegexs();
            usbAvailable = mUsbRegexs.length != 0;
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

        if (!usbAvailable)
        {
            // there's nothing to do here; this device has no USB tethering capability
            // getPreferenceScreen().removePreference(mUsbTether);
            // TODO signal this somehow 
        }
        
        mUsbTether = (CheckBoxPreference) findPreference(USB_TETHER_SETTINGS);
        
        if (ucm.hasAllRequiredPermissions(getActivity().getApplicationContext()))
        {
            Log.i(TAG, "all required permissions obtained, good to go");
        }
        else
        {
            Log.e(TAG, "do not have required permissions, will fail");
            // TODO signal this somehow 
        }
    }

    private void setUsbTethering(boolean enabled)
    {        
        Integer summary = R.string.usb_tethering_errored_subtext;        
        try
        {
            if (ucm.setUsbTethering(enabled) == UsbConnectivityManager.TETHER_ERROR_NO_ERROR)
            {
                mUsbTether.setChecked(enabled);
                summary = null;
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
                summary = R.string.usb_tethering_errored_noperm;
            }
        }
        
        if (summary != null)
        {
            mUsbTether.setSummary(summary);            
        }
        else
        {
            mUsbTether.setSummary("");                        
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference)
    {
        if (preference == mUsbTether)
        {
            boolean newState = mUsbTether.isChecked();
            setUsbTethering(newState);
            Log.i(TAG, "successsfully set usb tethering to " + newState);
        }
        
        return super.onPreferenceTreeClick(screen, preference);
    }
 
    private class TetherChangeReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context content, Intent intent)
        {
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
                mUsbConnected = intent.getBooleanExtra(UsbConnectivityManager.USB_CONNECTED, false);
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

        updateState(null, null, null);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        getActivity().unregisterReceiver(mTetherChangeReceiver);
        mTetherChangeReceiver = null;
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

package com.waldura.bestusbtether;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * Provide methods and constants hidden from the public API.
 * 
 * @author ren+android@waldura.com
 */
public class UsbConnectivityManager
{
    // ConnectivityManager
    // https://github.com/android/platform_frameworks_base/blob/master/core/java/android/net/ConnectivityManager.java
    public static final int TETHER_ERROR_NO_ERROR = 0;
    public static final int TETHER_ERROR_UNKNOWN_IFACE = 1;
    public static final int TETHER_ERROR_SERVICE_UNAVAIL = 2;
    public static final int TETHER_ERROR_UNSUPPORTED = 3;
    public static final int TETHER_ERROR_UNAVAIL_IFACE = 4;
    public static final int TETHER_ERROR_MASTER_ERROR = 5;
    public static final int TETHER_ERROR_TETHER_IFACE_ERROR = 6;
    public static final int TETHER_ERROR_UNTETHER_IFACE_ERROR = 7;
    public static final int TETHER_ERROR_ENABLE_NAT_ERROR = 8;
    public static final int TETHER_ERROR_DISABLE_NAT_ERROR = 9;
    public static final int TETHER_ERROR_IFACE_CFG_ERROR = 10;

    public static final String ACTION_TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED";
    public static final String EXTRA_AVAILABLE_TETHER = "availableArray";
    public static final String EXTRA_ACTIVE_TETHER = "activeArray";
    public static final String EXTRA_ERRORED_TETHER = "erroredArray";

    // Intent
    public static final String ACTION_MEDIA_SHARED = Intent.ACTION_MEDIA_SHARED;
    public static final String ACTION_MEDIA_UNSHARED = "android.intent.action.MEDIA_UNSHARED";
    
    // UsbManager
    // https://github.com/android/platform_frameworks_base/blob/master/core/java/android/hardware/usb/UsbManager.java
    public static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
    public static final String USB_CONNECTED = "connected";
    public static final String USB_CONFIGURED = "configured";
    
    private static final String TAG = UsbTetherSettings.TAG;
    
    private final ConnectivityManager cm;
    private boolean isUsbTetheringActive = false;

    public UsbConnectivityManager(Context ctx)
    {
        this((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE));
    }

    public UsbConnectivityManager(ConnectivityManager cm)
    {
        this.cm = cm;
    }

    public boolean hasAllRequiredPermissions(Context ctx)
    {
        return hasPermission(ctx, "android.permission.ACCESS_NETWORK_STATE") 
                && hasPermission(ctx, "android.permission.CHANGE_NETWORK_STATE") 
                && hasPermission(ctx, "android.permission.MANAGE_USB");
    }

    public boolean hasPermission(Context ctx, String perm)
    {
        if (ctx.checkCallingOrSelfPermission(perm) == PackageManager.PERMISSION_GRANTED)
        {
            Log.d(TAG, "permission is granted: " + perm);
            return true;
        }
        else
        {
            Log.d(TAG, "permission is denied: " + perm);
            return false;
        }
    }

    public int setUsbTethering(boolean flag) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        // call original method on the actual class
        Method m = cm.getClass().getMethod("setUsbTethering", Boolean.TYPE);
        Integer err = (Integer) m.invoke(cm, Boolean.valueOf(flag));

        isUsbTetheringActive = flag;

        Log.d(TAG, "setUsbTethering to " + flag + " returns " + err);
        return err;
    }

    public boolean flipUsbTethering() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        return setUsbTethering(!isUsbTetheringActive) == TETHER_ERROR_NO_ERROR;
    }

    private String[] invokeSimple(String name) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        // call original method on the actual class
        Method m = cm.getClass().getMethod(name);
        String[] result = (String[]) m.invoke(cm);

        Log.d(TAG, name + " returns " + result);
        return result;
    }

    public String[] getTetherableUsbRegexs() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        return invokeSimple("getTetherableUsbRegexs");
    }

    public String[] getTetheringErroredIfaces() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        return invokeSimple("getTetheringErroredIfaces");
    }

    public String[] getTetheredIfaces() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        return invokeSimple("getTetheredIfaces");
    }

    public String[] getTetherableIfaces() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        return invokeSimple("getTetherableIfaces");
    }

    public int getLastTetherError(String s) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        // call original method on the actual class
        Method m = cm.getClass().getMethod("getLastTetherError", String.class);
        Integer err = (Integer) m.invoke(cm, s);

        Log.d(TAG, "getLastTetherError for " + s + " returns " + err);
        return err;
    }

}

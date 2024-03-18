package eu.weischer.root.application;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.PowerManager;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Permission {
    public static final String manageExternalStorage = "android.permission.MANAGE_EXTERNAL_STORAGE";
    public static final String ignoreBatteryOptimization = "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS";
    public static final String multiplePemissions = "multiplePemissions";
    public static final String startText = "startText";
    private static Permission permission = null;
    private static Logger.LogAdapter log = Logger.getLogAdapter("Permission");
    public static Permission getPermission() {
        if (permission == null)
            permission = new Permission();
        return permission;
    }

    private List<String> required = new ArrayList<>();
    private List<String> granted = new ArrayList<>();
    private List<String> toBeGranted = new ArrayList<>();
    private boolean externalStorageManagerToBeGranted = false;
    private boolean requestIgnoreBatteryOptimizationsToBeGranted = false;

    private Permission() {
        log.v("Permission created");
    }

    public void initialize(Intent intent) {
        log.v ("Initialize");
        try {
            PackageInfo info = App.getPackageManager()
                    .getPackageInfo(App.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null)
                for (String permission : info.requestedPermissions) {
                    log.v("Permission requested " + permission);
                    required.add(permission);
                    if (permission.equals(manageExternalStorage))
                        externalStorageManagerToBeGranted = true;
                    else {
                        if (permission.equals(ignoreBatteryOptimization))
                            requestIgnoreBatteryOptimizationsToBeGranted = true;
                        else
                            toBeGranted.add(permission);
                    }
                }
            checkPermission(intent);
        } catch (Exception ex) {
            log.e(ex, "Error during initialization");
        }
    }

    public void checkPermission() {
        checkPermission(null);
    }
    private void checkPermission(Intent intent) {
        int missingPermissions = 0;
        if (externalStorageManagerToBeGranted)
            if (Environment.isExternalStorageManager()) {
                externalStorageManagerToBeGranted = false;
                log.v (manageExternalStorage + " granted");
            } else
                missingPermissions++;
        if (requestIgnoreBatteryOptimizationsToBeGranted) {
            PowerManager powerManager = (PowerManager) App.getSystemService(Context.POWER_SERVICE);
            if (powerManager.isIgnoringBatteryOptimizations(App.getPackageName())) {
                requestIgnoreBatteryOptimizationsToBeGranted = false;
                log.v (ignoreBatteryOptimization + " granted");
            } else
                missingPermissions++;
        }
        List<String> tempGranted = new ArrayList<>();
        for (String permission : toBeGranted)
            if (ContextCompat.checkSelfPermission(App.getApp(), permission) == PackageManager.PERMISSION_GRANTED) {
                tempGranted.add(permission);
                log.v (permission + " granted");
            }
        if (tempGranted.size() > 0) {
            toBeGranted.removeAll(tempGranted);
            granted.addAll(tempGranted);
        }
        missingPermissions += toBeGranted.size();

        if (intent != null)
            addToIntent(intent);
        App.getApp().permissionResult(missingPermissions);
        log.v("Missing permissions = " + missingPermissions);
    }

    private void addToIntent(Intent intent) {
        if (externalStorageManagerToBeGranted)
            intent.putExtra(manageExternalStorage, externalStorageManagerToBeGranted);
        if (requestIgnoreBatteryOptimizationsToBeGranted)
            intent.putExtra(ignoreBatteryOptimization, requestIgnoreBatteryOptimizationsToBeGranted);
        if (toBeGranted.size() != 0) {
            String[] permArray = Arrays.copyOf(toBeGranted.toArray(), toBeGranted.size(), String[].class);
            intent.putExtra(multiplePemissions, permArray);
        }
    }
}

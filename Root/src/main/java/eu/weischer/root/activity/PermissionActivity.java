package eu.weischer.root.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;
import java.util.Map;

import eu.weischer.root.application.App;
import eu.weischer.root.application.Logger;
import eu.weischer.root.application.Permission;
import eu.weischer.root.databinding.StartActivityBinding;

public class PermissionActivity extends AppCompatActivity {
    private static final Logger.LogAdapter log = Logger.getLogAdapter("PermissionActivity");
    private boolean externalStorageManager = false;
    private boolean ignoreBatteryOptimization = false;
    private String[] requiredPermissions = null;
    private TextView textView = null;
    private ActivityResultLauncher<Intent> resultLauncherExternal = null;
    private ActivityResultLauncher<Intent> resultLauncherBattery = null;
    private ActivityResultLauncher<String[]> stringResultLauncher = null;
    private ActivityResultCallback<ActivityResult> callBackExternal = (result) -> {
        log.v("callBack external storage manager" );
        Permission.getPermission().checkPermission();
    };
    private ActivityResultCallback<ActivityResult> callBackBattery = (result) -> {
        log.v("callBack ignore battery optimization" );
        Permission.getPermission().checkPermission();
    };
    private ActivityResultCallback<Map<String, Boolean>> callBackMultiple = (result) -> {
        log.v("callBack multiple requests" );
        Permission.getPermission().checkPermission();
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            log.v("onCreate");
            App.getApp().permissionActivityStarted(this);

            Method method = StartActivityBinding.class.getMethod("inflate", LayoutInflater.class);
            StartActivityBinding binder = (StartActivityBinding) method.invoke(null, getLayoutInflater());
            setContentView(binder.getRoot());
            textView = binder.startText;

            Intent intent = getIntent();
            externalStorageManager = intent.getBooleanExtra(Permission.manageExternalStorage, false);
            ignoreBatteryOptimization = intent.getBooleanExtra(Permission.ignoreBatteryOptimization, false);
            requiredPermissions = intent.getStringArrayExtra(Permission.multiplePemissions);
            Bundle extras = intent.getExtras();
            if(extras != null) {
                String string= extras.getString(Permission.startText);
                if ((string != null) && (!string.isEmpty()))
                    setText(string);
            }
            if (requiredPermissions != null)
                stringResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), callBackMultiple);
            if (externalStorageManager)
                resultLauncherExternal = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), callBackExternal);
            if (ignoreBatteryOptimization)
                resultLauncherBattery = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), callBackBattery);
        } catch (Exception ex) {
            log.e(ex, "Exception during onCreate");
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        try {
            log.v("onResume");
            if (externalStorageManager) {
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                resultLauncherExternal.launch(intent);
                externalStorageManager = false;
            }
            if (ignoreBatteryOptimization) {
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, uri);
                resultLauncherBattery.launch(intent);
                ignoreBatteryOptimization = false;
            }
            if (requiredPermissions != null) {
                stringResultLauncher.launch(requiredPermissions);
                requiredPermissions = null;
            }
            log.v("onResume finished");
        } catch (Exception ex) {
            log.e(ex, "Exception");
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        log.v ("onDestroy");
    }
    public void setText(String text) {
        try {
            if (textView != null)
                textView.setText(text);
        } catch (Exception ex) {}
    }
    public void action(View v) {
        log.v("cancel");
        App.kill();
    }
}

package eu.weischer.root.application;

import android.app.Application;
import android.content.Intent;
import android.content.res.Resources;

import androidx.work.WorkManager;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eu.weischer.root.R;
import eu.weischer.root.activity.PermissionActivity;
import eu.weischer.root.activity.RootActivity;

public class RootApplication extends Application {
    protected static Logger.LogAdapter log = Logger.getLogAdapter("RootApplication");
    private Resources resources = null;
    private String rootFolder = null;
    private String applicationName = "";
    private WorkManager workManager = null;
    private ThreadPoolExecutor threadPoolExecutor = null;
    private Class rootMainActivity = null;
    private boolean initialized = false;
    private boolean permissionActivityStarted = false;
    private PermissionActivity permissionActivity = null;
    private Intent permissionIntent = null;
    private Runnable initializationRunnable = null;
    private Runnable initializationFinished = () -> {
        initialized = true;
        log.i("Application initialized");
        if (rootMainActivity != null) {
            log.i("Restart main activity");
            try {
                Intent intent = new Intent(this, rootMainActivity);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                log.v("Main activity restarted");
            } catch (Exception ex) {
                log.e(ex, "Error restarting main activity");
            }
        }
        if (permissionActivity != null) {
            try {
                permissionActivity.finish();
                log.v("PermissionActivity finished");
                permissionActivity = null;
            } catch (Exception ex) {}
        }
    };

    public RootApplication() {
        super();
        log.i ("Constructor");
        App.setApp(this);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            log.v("onCreate");
            resources = getResources();
            applicationName = resources.getString(R.string.app_name);
            try {
                File file = getApplicationContext().getExternalFilesDir(null);
                while (!file.getName().contains("Android"))
                    file = file.getParentFile();
                rootFolder = file.getParent();
            } catch (Exception ex) {
                log.e(ex, "Error during find root folder");
            }
            registerActivityLifecycleCallbacks(new ActivityLifecycleHandler(this));
            workManager = WorkManager.getInstance(this);
            initializationRunnable = getInitializationRunnable();
            permissionIntent = new Intent(getApplicationContext(), PermissionActivity.class);
            permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Permission.getPermission().initialize(permissionIntent);
        } catch (Exception e) {
            log.e(e, "Exception during onCreate");
        }
    }
    @Override
    public void onLowMemory() {
        log.v ("onLowMemory");
        super.onLowMemory();
    }
    @Override
    public void onTerminate() {
        log.v ("onTerminate");
        if (threadPoolExecutor != null) {
            try {
                threadPoolExecutor.shutdownNow();
                threadPoolExecutor = null;
            } catch (Exception ex) {}
        }
        super.onTerminate();
    }
    @Override
    public void onTrimMemory(int level) {
        log.v ("onTrimMemory");
        super.onTrimMemory(level);
    }

    public void kill() {
        try {
            log.i("Application exit");
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);
        } catch (Exception ex) {};
    }

    public String getResourceString(int resourceId){
        return resources.getString(resourceId);
    }
    public Integer getResourceInt(int resourceId){
        return resources.getInteger(resourceId);
    }
    public int getResourceColor(int resourceId){
        return resources.getColor(resourceId);
    }
    public String getApplicationName() {
        return applicationName;
    }
    public String getRootFolder() {
        return rootFolder;
    }
    public WorkManager getWorkManager() {
        return workManager;
    }
    public ThreadPoolExecutor getThreadPoolExecutor() {
        if (threadPoolExecutor == null) {
            try {
                int maximumPoolsize = getResourceInt(R.integer.root_MaximumPoolSize);
                if (maximumPoolsize > 0) {
                    threadPoolExecutor = new ThreadPoolExecutor(
                            getResourceInt(R.integer.root_CorePoolSize),
                            maximumPoolsize,
                            getResourceInt(R.integer.root_KeepAliveTime),
                            TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>()
                    );
                }
            } catch (Exception ex) {
                log.e(ex, "Exception while creating thread pool");
            }
        }
        return threadPoolExecutor;
    }
    public Class getRootMainActvity() {
        return rootMainActivity;
    }
    public void permissionActivityStarted(PermissionActivity permissionActivity) {
        log.v("PermissionActivity created");
        if (this.permissionActivity==null) {
            if (initialized) {
                try {
                    permissionActivity.finish();
                } catch (Exception ex) {}
            }
            else
                this.permissionActivity = permissionActivity;
        }
    }
    public void permissionResult(int missingPermissions) {
        log.v ("Missing permissions = " + missingPermissions);
        if (missingPermissions > 0) {
            setText("" + missingPermissions + getResourceString(R.string.root_permission_missing));
            if (! permissionActivityStarted) {
                getApplicationContext().startActivity(permissionIntent);
                permissionActivityStarted = true;
            }
        }
        if (missingPermissions == 0) {
            log.getLogger().initialize();
            setText(getResourceString(R.string.root_got_permissions));
            if (initializationRunnable == null) {
                App.postOnUiThread(initializationFinished);
            } else {
                if (! permissionActivityStarted) {
                    permissionIntent.putExtra(Permission.startText, getResourceString(R.string.root_initialization));
                    getApplicationContext().startActivity(permissionIntent);
                    permissionActivityStarted = true;
                }
                setText(getResourceString(R.string.root_initialization));
                CompletableFuture future = CompletableFuture.runAsync(initializationRunnable, getThreadPoolExecutor());
                future.thenRunAsync(initializationFinished, App.getMainExecutorService());
            }
        }
    }
    public boolean isInitialized(RootActivity activity) {
        if (rootMainActivity == null)
            rootMainActivity = activity.getClass();
        return initialized;
    }
    public boolean isInitialized() {
        return initialized;
    }
    public Runnable getInitializationRunnable() {
        return null;
    }
    private void setText(String text) {
        if (permissionActivity != null)
            permissionActivity.setText(text);
    }
}

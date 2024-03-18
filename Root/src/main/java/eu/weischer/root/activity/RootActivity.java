package eu.weischer.root.activity;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewbinding.ViewBinding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import eu.weischer.root.application.App;
import eu.weischer.root.application.Logger;

public class RootActivity extends AppCompatActivity {
    protected static Logger.LogAdapter log = Logger.getLogAdapter("RootActivity");

    protected Layout layoutAnnotation = null;
    private boolean running = false;
    private boolean working = false;
    private ViewBinding rootBinder = null;
    private int menuId = 0;
    private boolean firstWork = true;
    private CompletableFuture<Boolean> preCompletable = null;

    public RootActivity() {
        super();
        log.v ("Constructor");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.v ("onCreate");
        layoutAnnotation = getLayoutAnnotation();
        if (layoutAnnotation != null) {
            Class bindingClass = layoutAnnotation.bindingClass();
            if (!bindingClass.equals(void.class)) {
                viewBinding(bindingClass);
                if (layoutAnnotation.setNavigatiobBarColor())
                    getWindow().setNavigationBarColor(((ColorDrawable)rootBinder.getRoot().getBackground()).getColor());
            }
            menuId = layoutAnnotation.menuId();
            if (menuId != 0) {
                for (Field field : rootBinder.getClass().getFields()) {
                    if (field.getName().equals("toolbar")) {
                        try {
                            setSupportActionBar((Toolbar) field.get(rootBinder));
                        } catch (IllegalAccessException e) {}
                    }
                }
            }
        }
        if (! App.getApp().isInitialized(this)) {
            log.i("Activity aborted");
            finish();
        } else {
            CompletableFuture<Boolean> completable = getPreCompletable();
            if (completable != null)
                preCompletable = completable.handleAsync((result, exception) -> {
                    preCompletable = null;
                    if ((exception == null) && (result == true))
                        checkWork();
                    return true;
                }, App.getMainExecutorService());
        }
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        log.v ("onRestart");
    }
    @Override
    protected void onStart() {
        super.onStart();
        log.v ("onStart");
    }
    @Override
    protected void onResume() {
        super.onResume();
        log.v ("onResume");
        running = true;
        checkWork();
    }
    @Override
    protected void onPause() {
        super.onPause();
        log.v ("onPause");
        running = false;
        checkWork();
    }
    @Override
    protected void onStop() {
        super.onStop();
        log.v ("onStop");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        log.v ("onDestroy");
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (menuId == 0)
            return false;
        getMenuInflater().inflate(menuId, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem menuItem) {
        int id = menuItem.getItemId();
/*
        if (id == android.R.id.home) {
            homeButtonPressed();
            return true;
        }
*/
        for (Method method : getClass().getMethods()) {
            if (method.isAnnotationPresent(MenuItemHandler.class)) {
                Annotation annotation = method.getAnnotation(MenuItemHandler.class);
                MenuItemHandler menuItemHandler = (MenuItemHandler) annotation;
                if (menuItemHandler.id() == menuItem.getItemId()) {
                    try {
                        method.invoke(this);
                        return true;
                    } catch (Exception ex) {
                    }
                }
            }
        }
        return false;
    }

    protected void startWork() {
        log.v ("startWork");
    };
    protected void stopWork() {
        log.v ("stopWork");
    };
    protected boolean mayWork() {
        if (preCompletable != null)
            return false;
        return running;
    }
    protected void checkWork() {
        if (mayWork()) {
            log.a("checkWork: may work(working is " + working + ")");
            if (! working) {
                working = true;
                if (firstWork) {
                    firstWork = false;
                    firstStartWork();
                }
                startWork();
            }
        } else {
            log.a("checkWork may not work (working is " + working + ")");
            if (working) {
                working = false;
                stopWork();
            }
        }
    }
    protected <T extends ViewBinding> T getBinder() {
        return (T) rootBinder;
    }
    protected boolean isWorking() {
        return working;
    }
    protected CompletableFuture<Boolean> getPreCompletable() {
        return null;
    }
    protected void firstStartWork() {}

    private Layout getLayoutAnnotation() {
        return (Layout) App.getClassAnnotation(this, Layout.class);
    }
    private void viewBinding(Class<?> viewBindingClass) {
        if (viewBindingClass != null) {
            try {
                Method method = viewBindingClass.getMethod("inflate", LayoutInflater.class);
                rootBinder = (ViewBinding) method.invoke(null, getLayoutInflater());
                setContentView(rootBinder.getRoot());
            } catch (Exception ex) {
                log.e (ex, "Error during bind view");
            }
        }
    }
}

package eu.weischer.root.application;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.work.WorkManager;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class App {
    private final static String mainActivityTag = "MainActivity";
    private static RootApplication app = null;
    private static Handler uiHandler = null;
    private static Thread uiThread = null;
    private static MainExecutorService mainExecutorService = null;
    protected static Logger.LogAdapter log = Logger.getLogAdapter("App");

    public static RootApplication getApp() {
        return app;
    }
    public static void setApp(RootApplication app) {
        App.app = app;
        uiThread = Thread.currentThread();
    }
    public static Context getContext() {
        return app.getApplicationContext();
    }
    public static String getResourceString(int resourceId){
        return app.getResourceString(resourceId);
    }
    public static Integer getResourceInt(int resourceId){
        return app.getResourceInt(resourceId);
    }
    public static int getResourceColor(int resourceId){
        return app.getResourceColor(resourceId);
    }
    public static String getApplicationName() {
        return app.getApplicationName();
    }
    public static String getRootFolder() {
        return app.getRootFolder();
    }
    public static Annotation getClassAnnotation (Object object, Class annotationClass) {
        if (object != null) {
            Class annoated = object.getClass();
            while (annoated != null) {
                for (Annotation annotation : annoated.getAnnotations()) {
                    if (annotation.annotationType().equals(annotationClass)) {
                        return annotation;
                    }
                }
                annoated = annoated.getSuperclass();
            }
        }
        return null;
    }
    public static WorkManager getWorkManager() {
        return app.getWorkManager();
    }
    public static ThreadPoolExecutor getThreadPoolExecutor() {
        return app.getThreadPoolExecutor();
    }
    //    public static WorkManager getWorkManager() {
//        return app.getWorkManager();
//    }
    public static Handler getUiHandler() {
        if (uiHandler==null)
            uiHandler = new Handler(Looper.getMainLooper());
        return uiHandler;
    }
    //    public static CallableSet getInitializationTasks() {
//        return app.getInitializationTasks();
//    }
    public static String getMetadata(String key, String defaultValue) {
        try {
            Bundle metaData = app.getPackageManager().getApplicationInfo(app.getPackageName(),
                    PackageManager.GET_META_DATA).metaData;
            return metaData.getString(key);
        } catch (Exception e) {}
        return defaultValue;
    }
    public static void toast (String text) {
        if (Thread.currentThread() == uiThread)
            Toast.makeText(app, text, Toast.LENGTH_LONG).show();
        else
            getUiHandler().post(()->{Toast.makeText(app, text, Toast.LENGTH_LONG).show();});

    }
    public static <T> Future<T> submit(Callable<T> task) {
        return getThreadPoolExecutor().submit(task);
    }
    public static Future<?> submit(Runnable task) {
        return getThreadPoolExecutor().submit(task);
    }
    public static void postOnUiThread(Runnable runnable) {
        getUiHandler().post(runnable);
    }
    public static PackageManager getPackageManager() {
        return app.getPackageManager();
    }
    public static String getPackageName() {
        return app.getPackageName();
    }
    public static Object getSystemService(String name) {
        return app.getSystemService(name);
    }
    public static ExecutorService getMainExecutorService() {
        if (mainExecutorService == null)
            mainExecutorService = MainExecutorService.getMainExecutorService();
        return mainExecutorService;
    }
    public static void kill () {
        app.kill();
    }
    public static <A, B> Runnable getBiConsumer(final BiConsumer<A, B> biConsumer, final A firstArgument, final B secondArgument) {
        return () -> {
            biConsumer.accept(firstArgument, secondArgument);
        };
    }
    public static <A> Runnable getConsumer(final Consumer<A> consumer, final A argument) {
        return () -> {
            consumer.accept(argument);
        };
    }

//    public static RootService getApplicationService() {
//        return app.getApplicationService();
//    }

    private App() {}
}

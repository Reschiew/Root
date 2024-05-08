package eu.weischer.root.work;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import eu.weischer.root.application.App;
import eu.weischer.root.application.Logger;

public class RootWork {
    public enum WorkState {
        defined,
        started,
        running,
        completed,
        cancelled,
        failed;
    }
    public interface WorkResponse {
        default void signalState(WorkState state) {};
    }

    private static final String workPreference = "WorkPreference";
    private static final class Future<V> implements Callable<V> {
        private V result = null;
        private boolean finished = false;
        private boolean interrupted = false;
        @Override
        public V call() throws Exception {
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException ex) {
                if (result != null)
                    return result;
            }
            return null;
        }
    }

    public static RootWork getByUUID(UUID uuid) {
        return uuidMap.get(uuid);
    };
    public static RootWork getByKey(String key) {
        return workMap.get(key);
    }
    public static boolean getPreference(String key) {
        SharedPreferences sharedPreferences = getSharedPreferences();
        return sharedPreferences.getBoolean(key, false);
    }
    private static SharedPreferences getSharedPreferences() {
        return App.getApp().getSharedPreferences(workPreference, Context.MODE_PRIVATE);
    }

    private static Logger.LogAdapter log = Logger.getLogAdapter("RootWork");
    private static Map<String, RootWork> workMap = new ConcurrentHashMap<>();
    private static Map<UUID, RootWork> uuidMap = new ConcurrentHashMap<>();
    private static int uniqueRequestCode = 1;
    private static int uniqueNotificationId = 1;

    private WorkResponse response = null;
    private WorkState state = WorkState.defined;
    private WorkRequest workRequest = null;
    private RootNotification rootNotification = null;
    private Thread workerThread = null;
    private String key;

    public RootWork(@NonNull String key, WorkResponse response) {
        this.key = key;
        this.response = response;

        createRootNotification();
        workMap.put(key, this);
    }
    public synchronized void updateResponse(WorkResponse response) {
        this.response = response;
    }
    public synchronized void removeResponse() {
        response = null;
    }

    public synchronized void startWork() {
        if (state == WorkState.defined) {
            log.i("startWork");
            try {
                workRequest = new OneTimeWorkRequest.Builder(RootWorker.class).build();
                uuidMap.put(workRequest.getId(), this);
                log.v("UUID is " + workRequest.getId());
                setState(WorkState.started);
                App.getWorkManager().enqueueUniqueWork(key, ExistingWorkPolicy.REPLACE, (OneTimeWorkRequest) workRequest);
            } catch (Exception ex) {
                log.e(ex,"Error during startWork");
                setState(WorkState.failed);
            }
        }
    }

    public synchronized void cancelWork() {
        if (isFinished())
            log.v("Work already fnished");
        else {
            if (workRequest != null) {
                try {
                    App.getWorkManager().cancelWorkById(workRequest.getId());
                    setState(WorkState.cancelled);
                    onCancel();
                    log.i("work cancelled " + workRequest.getId().toString());
                } catch (Exception ex) {
                    log.e(ex, "Error during cancel work");
                }
            }
        }
    }
    public void setWorkerThread(Thread workerThread) {
        this.workerThread = workerThread;
    }
    public synchronized void clean() {
        if (workRequest != null) {
            UUID uuid = workRequest.getId();
            if (uuid != null)
                uuidMap.remove(uuid);
        }
        workMap.remove(key);
    }
    public synchronized boolean isFinished() {
        return (state==WorkState.completed) || (state==WorkState.cancelled) || (state==WorkState.failed);
    }
    public WorkResult work() {
        return WorkResult.failed;
    }
    public void onClose() {}
    public RootNotification getRootNotification() {
        return rootNotification;
    }
    public void updateNotifification(String text) {
        if (rootNotification != null)
            rootNotification.updateText(text);
    }
    public void setPreference() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, true);
        editor.commit();
    }
    public void clearPreference() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, false);
        editor.commit();
    }
    public synchronized WorkResponse getResponse() {
        return response;
    }
    public synchronized WorkState getState() {
        return state;
    }
    protected synchronized WorkRequest getWorkRequest() {
        return workRequest;
    }
    protected synchronized String getKey() {
        return key;
    }
    protected synchronized boolean isAbort() {
        return Thread.currentThread().isInterrupted();
    }
    protected synchronized void setState(WorkState state) {
        if (! isFinished()) {
            this.state = state;
            if (response != null) {
                try {
                    response.signalState(state);
                } catch (Exception ex) {}
            }
            if (isFinished())
                uuidMap.remove(workRequest.getId());
        }
    }
    protected void onCancel() {
        log.v("onCancel ");
        if (workerThread != null)
            workerThread.interrupt();
    }

    private void createRootNotification() {
        try {
            Foreground foregroundAnnotation = (Foreground) App.getClassAnnotation(this, Foreground.class);
            if (foregroundAnnotation != null) {
                Context context = App.getContext();

                Class activity = foregroundAnnotation.activity();
                int requestCode = 1;
                int notificationId = 1;
                String channelId = "WorkChannel";
                int smallIconId = android.R.drawable.ic_dialog_info;
                String title = "Work";
                String text = "Text";
                int colorId = android.graphics.Color.WHITE;

                requestCode = foregroundAnnotation.requestCode() < 0 ?
                        uniqueRequestCode++ : foregroundAnnotation.requestCode();
                activity = foregroundAnnotation.activity();
                notificationId = foregroundAnnotation.notificationId() < 0 ?
                        uniqueNotificationId++ : foregroundAnnotation.notificationId();
                channelId = foregroundAnnotation.channelId();
                smallIconId = foregroundAnnotation.smallIconId();
                title = foregroundAnnotation.title().isEmpty() ?
                        activity.getSimpleName() : foregroundAnnotation.title();
                text = foregroundAnnotation.text();
                colorId = foregroundAnnotation.colorId();

                rootNotification = new RootNotification(context, notificationId,channelId,
                        smallIconId, title, text, colorId);
                Intent notificationIntent = new Intent(context, activity);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode,
                        notificationIntent, PendingIntent.FLAG_IMMUTABLE);
                rootNotification.setPendingIntent(pendingIntent);
                rootNotification.setOngoing(true);
                rootNotification.setSilent(true);
            }
        } catch (Exception ex) {
            log.e(ex, "Exception in createRootNotification");
            rootNotification = null;
        }
    }
}

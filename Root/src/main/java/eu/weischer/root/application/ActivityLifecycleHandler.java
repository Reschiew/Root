package eu.weischer.root.application;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

import eu.weischer.root.activity.RootActivity;

/**
 * Created by Michael on 23.12.2016.
 */

public class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private enum LifecycleState {
        undefined,
        created,
        restarted,
        started,
        running,
        paused,
        stopped,
        destroyed,
        saveInstanceState
    }
    private Logger.LogAdapter log = Logger.getLogAdapter("LifecycleHandler");
    private Map<Activity, LifecycleState> activityMap = new HashMap<Activity, LifecycleState>();
    private Map<Class, RootActivity>  classMap = new HashMap<>();

    public ActivityLifecycleHandler(Application application) {
        super();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        updateState (activity, LifecycleState.created);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        updateState (activity, LifecycleState.started);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        updateState (activity, LifecycleState.running);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        updateState (activity, LifecycleState.paused);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        updateState (activity, LifecycleState.stopped);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        updateState (activity, LifecycleState.saveInstanceState);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        updateState (activity, LifecycleState.destroyed);
    }

    private void updateState(Activity activity, LifecycleState lifecycleState) {
        LifecycleState previousState = activityMap.get(activity);
        if (previousState == null)
            previousState = LifecycleState.undefined;
        activityMap.put(activity,lifecycleState);
        log.v("Activity " + activity.getClass().getSimpleName() + " changed state from " + previousState.name() + " to " + lifecycleState.name());


        if (lifecycleState == LifecycleState.destroyed) {
            activityMap.remove(activity);
        }
    }
}


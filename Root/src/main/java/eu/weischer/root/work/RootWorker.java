package eu.weischer.root.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import eu.weischer.root.application.Logger;

public class RootWorker extends Worker {
    private final static Logger.LogAdapter log = Logger.getLogAdapter("RootWorker");

    private RootWork rootWork = null;

    private RootWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        log.i("Constructor");
    }

    @NonNull
    @Override
    public Result doWork() {
        Result result = Result.failure();
        try {
            log.i("doWork");
            rootWork = RootWork.getByUUID(getId());
            if (rootWork == null) {
                log.v("rootWork is null for UUID "+ getId());
                return result;
            }
            switch (rootWork.getState()) {
                default:
                case defined:
                case running:
                    rootWork.setState(RootWork.WorkState.failed);
                case completed:
                case cancelled:
                case failed:
                    rootWork.clean();
                    return result;
                case started:
                    break;
            }
            rootWork.setWorkerThread(Thread.currentThread());
            rootWork.setState(RootWork.WorkState.running);
            RootNotification rootNotification = rootWork.getRootNotification();
            if (rootNotification != null) {
                setForegroundAsync(rootNotification.createForegroundInfo());
                rootWork.setPreference();
            }

            WorkResult workResult = rootWork.work();
            rootWork.clearPreference();
            rootWork.onClose();
            switch (workResult) {
                case success:
                    result = Result.success();
                    rootWork.setState(RootWork.WorkState.completed);
                    break;
                case undefined:
                case failed:
                    result = Result.failure();
                    rootWork.setState(RootWork.WorkState.failed);
                    break;
                case cancelled:
                    result = Result.failure();
                    rootWork.setState(RootWork.WorkState.cancelled);
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            log.e(ex, "Error in doWork");
            rootWork.setState(RootWork.WorkState.failed);
        }
        rootWork.clean();
        return result;
    }
}

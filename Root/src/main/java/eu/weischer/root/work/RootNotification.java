package eu.weischer.root.work;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;

import eu.weischer.root.application.App;
import eu.weischer.root.application.Logger;

public class RootNotification {
    private static int uniqueNotificationId = 1;
    private static Logger.LogAdapter log = Logger.getLogAdapter("RootNotification");

    private int notificationId = 0;
    private String channelId;
    private String title;
    private String text;
    private int smallIconId = 0;
    private boolean ongoing = false;
    private boolean silent = false;
    private int colorId = android.graphics.Color.WHITE;
    private PendingIntent pendingIntent = null;
    private NotificationCompat.Builder notificationBuilder = null;
    private NotificationManager notificationManager = null;
    NotificationChannel notificationChannel = null;
    public RootNotification(Context context, int notificationId, String channelId, int smallIconId, String title, String text, int colorId) {
        this.notificationId = notificationId==0 ? uniqueNotificationId++ : notificationId;
        this.channelId = channelId;
        this.smallIconId = smallIconId;
        this.title = title;
        this.text = text;
        this.colorId = colorId;
        notificationBuilder = new NotificationCompat.Builder(context, channelId);
        notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        notificationChannel = new NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(notificationChannel);
    }
    public Notification getNotification() {
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(text);
        notificationBuilder.setSmallIcon(smallIconId);
        notificationBuilder.setOngoing(ongoing);
        notificationBuilder.setSilent(silent);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setChannelId(channelId);
        notificationBuilder.setColor(App.getResourceColor(colorId));
        return notificationBuilder.build();
    }
    public void setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }
    public void setOngoing(boolean ongoing) {
        this.ongoing = ongoing;
    }
    public void setSilent(boolean silent) {
        this.silent = silent;
    }
    public void issueNotification() {
        notificationManager.notify(notificationId, getNotification());
    }
    public void updateText(String text){
        log.a("Update text " + text);
        this.text = text;
        issueNotification()      ;
    }
    @NonNull
    public ForegroundInfo createForegroundInfo() {
        return new ForegroundInfo(notificationId,getNotification());
    }
}

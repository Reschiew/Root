package eu.weischer.root.work;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Foreground {
    Class<?> activity();
    int requestCode() default -1;
    int notificationId() default -1;
    String channelId();
    int smallIconId();
    String title() default "";
    String text() default "";
    int colorId() default android.graphics.Color.WHITE;
}

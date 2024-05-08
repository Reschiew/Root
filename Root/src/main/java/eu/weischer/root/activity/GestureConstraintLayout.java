package eu.weischer.root.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import eu.weischer.root.application.App;

public class GestureConstraintLayout extends ConstraintLayout {
    public interface OnGesture {
        default void click(View view) {return;};
        default void doubleClick(View view) {return;};
        default void longClick(View view) {return;};
        default void swipeLeft(View view) {return;};
        default void swipeRight(View view) {return;};
        default void swipeUp(View view) {return;};
        default void swipeDown(View view) {return;};
    }
    private final static class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        private GestureConstraintLayout constraintLayout = null;
        private GestureListener(GestureConstraintLayout constraintLayout) {
            super();
            this.constraintLayout = constraintLayout;
        }
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            constraintLayout.onClick();
            return true;
        }
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            constraintLayout.onDoubleClick();
            return true;
        }
        @Override
        public void onLongPress(MotionEvent e) {
            constraintLayout.onLongClick();
        }
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            constraintLayout.onSwipeRight();
                            return true;
                        } else {
                            constraintLayout.onSwipeLeft();
                            return true;
                        }
                    }
                }
                else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            constraintLayout.onSwipeDown();
                            return true;
                        } else {
                            constraintLayout.onSwipeUp();
                            return true;
                        }
                    }
                }
            }
            catch (Exception exception) {
            }
            return false;
        }
    }
    private GestureDetector gestureDetector = null;
    private OnGesture onGesture = null;
    private long lastEventTime = 0;

    public GestureConstraintLayout(@NonNull Context context) {
        super(context);
    }
    public GestureConstraintLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    public GestureConstraintLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public GestureConstraintLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        lastEventTime = ev.getEventTime();
        getGestureDetector().onTouchEvent(ev);
        return false;
    }
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = true;
        if (lastEventTime != ev.getEventTime())
            result = getGestureDetector().onTouchEvent(ev);
        return result;
    }

    public void setOnGesture(OnGesture onGesture) {
        this.onGesture = onGesture;
    }
    public void onSwipeRight() {
        if (onGesture != null)
            onGesture.swipeRight(this);
    }
    public void onSwipeLeft() {
        if (onGesture != null)
            onGesture.swipeLeft(this);
    }
    private void onSwipeUp() {
        if (onGesture != null)
            onGesture.swipeUp(this);
    }
    private void onSwipeDown() {
        if (onGesture != null)
            onGesture.swipeDown(this);
    }
    private void onClick() {
        if (onGesture != null)
            onGesture.click(this);
    }
    private void onDoubleClick() {
        if (onGesture != null)
            onGesture.doubleClick(this);
    }
    private void onLongClick() {
        if (onGesture != null)
            onGesture.longClick(this);
    }
    private GestureDetector getGestureDetector() {
        if (gestureDetector==null)
            gestureDetector = new GestureDetector(App.getContext(),
                    new GestureListener(this));
        return gestureDetector;
    }
}

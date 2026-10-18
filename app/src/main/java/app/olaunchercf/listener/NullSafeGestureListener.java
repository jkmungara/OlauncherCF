package app.olaunchercf.listener;

import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Android can pass a null first event to onFling (the API 34 SDK finally marks it nullable).
 * A Kotlin override of the API 33 signature must declare it non-null, and Kotlin then throws
 * on entry when it is null. Java adds no such check, so flings are routed through here first.
 */
public abstract class NullSafeGestureListener extends GestureDetector.SimpleOnGestureListener {

    @Override
    public final boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
        return onFlingSafe(e1, e2, velocityX, velocityY);
    }

    public abstract boolean onFlingSafe(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY);
}

package app.olaunchercf.listener

import android.content.Context
import android.view.MotionEvent
import kotlin.math.abs

// Thresholds in dp so swipes feel the same on every screen density.
// (The old 100px values meant ~50dp on hdpi but ~25dp on xxxhdpi.)
private const val SWIPE_DISTANCE_DP = 32f
private const val SWIPE_VELOCITY_DP = 32f

internal enum class Swipe { Left, Right, Up, Down }

internal class SwipeDetector(context: Context) {
    private val density = context.resources.displayMetrics.density
    private val minDistance = SWIPE_DISTANCE_DP * density
    private val minVelocity = SWIPE_VELOCITY_DP * density

    /** Classifies a fling, or returns null if it was too short or too slow to count. */
    fun classify(start: MotionEvent?, end: MotionEvent, velocityX: Float, velocityY: Float): Swipe? {
        // GestureDetector may pass a null start event (e.g. after a cancelled gesture).
        if (start == null) return null

        val diffX = end.x - start.x
        val diffY = end.y - start.y

        return if (abs(diffX) > abs(diffY)) {
            if (abs(diffX) > minDistance && abs(velocityX) > minVelocity) {
                if (diffX > 0) Swipe.Right else Swipe.Left
            } else {
                null
            }
        } else {
            if (abs(diffY) > minDistance && abs(velocityY) > minVelocity) {
                if (diffY > 0) Swipe.Down else Swipe.Up
            } else {
                null
            }
        }
    }
}

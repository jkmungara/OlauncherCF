package app.olaunchercf.listener

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import app.olaunchercf.data.Constants

/**
 * Swipe, double tap and long press listener for the empty home screen area.
 *
 * All callbacks run on the main thread. (They used to run on short lived Timer threads,
 * which leaked a thread per gesture and crashed when a callback touched the UI.)
 */
internal open class OnSwipeTouchListener(context: Context) : View.OnTouchListener {

    private val handler = Handler(Looper.getMainLooper())
    private val swipeDetector = SwipeDetector(context)
    private val gestureDetector = GestureDetector(context, GestureListener())

    // A long press only counts if the finger is still down this much longer.
    private val longPressRunnable = Runnable { onLongClick() }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(longPressRunnable)
        }
        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener : NullSafeGestureListener() {

        override fun onDown(e: MotionEvent): Boolean = true

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleClick()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            handler.removeCallbacks(longPressRunnable)
            handler.postDelayed(longPressRunnable, Constants.LONG_PRESS_DELAY_MS.toLong())
        }

        override fun onFlingSafe(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            when (swipeDetector.classify(e1, e2, velocityX, velocityY)) {
                Swipe.Left -> onSwipeLeft()
                Swipe.Right -> onSwipeRight()
                Swipe.Up -> onSwipeUp()
                Swipe.Down -> onSwipeDown()
                null -> return false
            }
            return true
        }
    }

    open fun onSwipeRight() {}
    open fun onSwipeLeft() {}
    open fun onSwipeUp() {}
    open fun onSwipeDown() {}
    open fun onLongClick() {}
    open fun onDoubleClick() {}
}

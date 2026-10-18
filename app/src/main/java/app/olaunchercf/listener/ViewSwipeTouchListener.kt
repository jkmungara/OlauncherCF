package app.olaunchercf.listener

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

/** Tap, long press and swipe listener for a single home screen app. */
internal open class ViewSwipeTouchListener(context: Context, private val view: View) : View.OnTouchListener {

    private val swipeDetector = SwipeDetector(context)
    private val gestureDetector = GestureDetector(context, GestureListener())

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> view.isPressed = true
            // CANCEL too, or the label stays dimmed when the gesture is interrupted
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.isPressed = false
        }
        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener : NullSafeGestureListener() {

        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick(view)
            return true
        }

        // Consumes the second tap so a quick double tap does not launch the app twice.
        override fun onDoubleTap(e: MotionEvent): Boolean = true

        override fun onLongPress(e: MotionEvent) {
            onLongClick(view)
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
    open fun onLongClick(view: View) {}
    open fun onClick(view: View) {}
}

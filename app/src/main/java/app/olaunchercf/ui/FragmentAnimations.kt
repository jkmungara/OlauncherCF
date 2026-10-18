package app.olaunchercf.ui

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.view.View
import androidx.fragment.app.Fragment

/**
 * Loads a navigation animator and renders the fragment into a hardware layer while it runs.
 *
 * Fading a view tree without a layer redraws every child with alpha on every frame.
 * With a layer the content is drawn once and the GPU only composites it.
 *
 * Returns null for anything that is not an animator resource, so the fragment framework
 * falls back to its default handling.
 */
internal fun Fragment.createLayeredAnimator(nextAnim: Int): Animator? {
    if (nextAnim == 0) return null

    val context = context ?: return null
    val target = view ?: return null

    val animator = try {
        if (context.resources.getResourceTypeName(nextAnim) != "animator") return null
        AnimatorInflater.loadAnimator(context, nextAnim)
    } catch (e: Exception) {
        return null
    }

    animator.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator) {
            target.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        override fun onAnimationEnd(animation: Animator) {
            target.setLayerType(View.LAYER_TYPE_NONE, null)
        }
    })

    return animator
}

package com.studycheck.student.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.OvershootInterpolator

/**
 * 按钮触摸反馈工具
 * 提供按压缩小、释放弹回的弹性动画
 */
object TouchFeedback {

    private val interpolator = OvershootInterpolator(3f)

    /**
     * 为View添加触摸弹性反馈
     */
    fun apply(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    animatePress(v)
                    false
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    animateRelease(v)
                    false
                }
                else -> false
            }
        }
    }

    /**
     * 批量绑定
     */
    fun applyAll(vararg views: View) {
        views.forEach { apply(it) }
    }

    private fun animatePress(view: View) {
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.93f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.93f)
        val animSet = AnimatorSet()
        animSet.playTogether(scaleDownX, scaleDownY)
        animSet.duration = 80
        animSet.start()
    }

    private fun animateRelease(view: View) {
        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f)
        val animSet = AnimatorSet()
        animSet.playTogether(scaleUpX, scaleUpY)
        animSet.duration = 200
        animSet.interpolator = interpolator
        animSet.start()
    }
}

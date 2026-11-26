package com.malakaonline.tv7reals

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

class CursorLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var cursorView: ImageView? = null
    private var indicatorView: TextView? = null
    private var targetView: View? = null

    var isCursorMode: Boolean = false
        private set

    private var cursorX: Float = 0f
    private var cursorY: Float = 0f

    private val baseStepPx: Float = dpToPx(16f)
    private var dpadCenterDownTime: Long = 0L
    private var isLongPressTriggered: Boolean = false
    private val longPressThresholdMs: Long = 650L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        isLongPressTriggered = true
        toggleCursorMode()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        cursorView = findViewById(R.id.cursorView)
        indicatorView = findViewById(R.id.cursorIndicator)
        targetView = findViewById(R.id.webView)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (cursorX == 0f && cursorY == 0f && w > 0 && h > 0) {
            // Center the cursor initially on the screen
            cursorX = w / 2f
            cursorY = h / 2f
            updateCursorPosition()
        }
    }

    /**
     * Toggles the virtual D-Pad cursor on or off.
     */
    fun toggleCursorMode(enable: Boolean = !isCursorMode) {
        isCursorMode = enable

        cursorView?.apply {
            visibility = if (isCursorMode) View.VISIBLE else View.GONE
            if (isCursorMode) {
                bringToFront()
                updateCursorPosition()
            }
        }

        indicatorView?.apply {
            if (isCursorMode) {
                visibility = View.VISIBLE
                alpha = 1f
                text = context.getString(R.string.cursor_on)
                // Animate brief highlight pulse
                ObjectAnimator.ofFloat(this, "alpha", 1f, 0.4f, 1f).apply {
                    duration = 500
                    start()
                }
            } else {
                visibility = View.GONE
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode

        // 1. Menu or Info key triggers toggle on key up
        if (keyCode == KeyEvent.KEYCODE_MENU ||
            keyCode == KeyEvent.KEYCODE_INFO ||
            keyCode == KeyEvent.KEYCODE_GUIDE
        ) {
            if (event.action == KeyEvent.ACTION_UP) {
                toggleCursorMode()
            }
            return true
        }

        // 2. Long-press on DPAD_CENTER / ENTER detection to toggle cursor mode
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        dpadCenterDownTime = SystemClock.uptimeMillis()
                        isLongPressTriggered = false
                        mainHandler.postDelayed(longPressRunnable, longPressThresholdMs)
                    }
                }
                KeyEvent.ACTION_UP -> {
                    mainHandler.removeCallbacks(longPressRunnable)
                    if (isLongPressTriggered) {
                        // Long press already handled mode toggle
                        return true
                    }
                    if (isCursorMode) {
                        // Regular short tap in cursor mode => dispatch touch click
                        dispatchSimulatedClick()
                        return true
                    }
                }
            }
        }

        // 3. If cursor mode is OFF, let key pass through normally (for native D-pad navigation)
        if (!isCursorMode) {
            return super.dispatchKeyEvent(event)
        }

        // 4. In Cursor Mode: Handle arrow movements
        if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_MULTIPLE) {
            // Accelerate slightly on sustained hold (repeatCount > 0)
            val multiplier = when {
                event.repeatCount > 20 -> 3.0f
                event.repeatCount > 8 -> 2.0f
                event.repeatCount > 2 -> 1.5f
                else -> 1.0f
            }
            val step = baseStepPx * multiplier

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    cursorY = max(0f, cursorY - step)
                    updateCursorPosition()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    cursorY = min(height.toFloat() - 10f, cursorY + step)
                    updateCursorPosition()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    cursorX = max(0f, cursorX - step)
                    updateCursorPosition()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    cursorX = min(width.toFloat() - 10f, cursorX + step)
                    updateCursorPosition()
                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    fun setCursorPosition(x: Float, y: Float) {
        cursorX = x
        cursorY = y
        updateCursorPosition()
    }

    private fun updateCursorPosition() {
        cursorView?.let { view ->
            view.x = cursorX
            view.y = cursorY
        }
    }

    /**
     * Dispatches down and up MotionEvents at (x, y) to targetView (the WebView).
     */
    fun dispatchSimulatedClick(x: Float = cursorX, y: Float = cursorY) {
        val target = targetView ?: this
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        val downEvent = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_DOWN,
            x,
            y,
            0
        )
        target.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        // Schedule ACTION_UP shortly after
        mainHandler.postDelayed({
            val upTime = SystemClock.uptimeMillis()
            val upEvent = MotionEvent.obtain(
                downTime,
                upTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                0
            )
            target.dispatchTouchEvent(upEvent)
            upEvent.recycle()
        }, 50)
    }

    /**
     * Dispatches a simulated tap at the exact screen center.
     * Triggers HTML5 video play/pause buttons directly without needing manual cursor positioning.
     */
    fun dispatchCenterClick() {
        val cx = if (width > 0) width / 2f else resources.displayMetrics.widthPixels / 2f
        val cy = if (height > 0) height / 2f else resources.displayMetrics.heightPixels / 2f
        dispatchSimulatedClick(cx, cy)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}

package com.primaloptima.scribe

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.primaloptima.scribe.data.Note

/**
 * Manages in-app floating reference windows. Each window is a draggable overlay
 * added directly to the activity's content frame, so no special permissions are needed.
 */
class FloatingWindowManager(private val activity: AppCompatActivity) {

    private val root: ViewGroup =
        activity.findViewById<ViewGroup>(android.R.id.content)

    private val activeWindows = mutableMapOf<String, View>()

    // ── Public API ────────────────────────────────────────────────────────────

    /** Open (or bring to front) a floating window for [note]. */
    fun openWindow(note: Note) {
        if (activeWindows.containsKey(note.id)) {
            activeWindows[note.id]?.bringToFront()
            return
        }
        val windowView = createWindowView(note)
        root.addView(windowView)
        activeWindows[note.id] = windowView
    }

    /** Close the floating window for the given note ID. */
    fun closeWindow(noteId: String) {
        activeWindows.remove(noteId)?.let { root.removeView(it) }
    }

    /** Close all open floating windows. */
    fun closeAll() {
        activeWindows.values.toList().forEach { root.removeView(it) }
        activeWindows.clear()
    }

    val windowCount: Int get() = activeWindows.size

    // ── Window creation ───────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private fun createWindowView(note: Note): View {
        val ctx = activity

        // Default position: 10% from each edge
        val dm = ctx.resources.displayMetrics
        val initW = (dm.widthPixels * 0.75f).toInt()
        val initH = (dm.heightPixels * 0.45f).toInt()
        val initX = (dm.widthPixels  * 0.12f)
        val initY = (dm.heightPixels * 0.20f)

        // Root container
        val container = FrameLayout(ctx).apply {
            elevation = 24f
            alpha     = 0f
            translationX = initX
            translationY = initY
        }

        val lp = FrameLayout.LayoutParams(initW, initH)
        container.layoutParams = lp

        // Build the window from the layout
        val inflater = activity.layoutInflater
        val inner = inflater.inflate(R.layout.layout_floating_window, container, false)

        inner.findViewById<TextView>(R.id.tv_fw_title)?.text = note.name
        inner.findViewById<TextView>(R.id.tv_fw_content)?.text =
            note.content.ifBlank { ctx.getString(R.string.empty_note) }

        inner.findViewById<ImageButton>(R.id.btn_fw_close)?.setOnClickListener {
            closeWindow(note.id)
        }

        container.addView(inner)

        // Animate in
        container.animate().alpha(1f).setDuration(180).start()

        // Drag via the title bar
        val titleBar = inner.findViewById<View>(R.id.fw_title_bar) ?: inner
        var downRawX = 0f
        var downRawY = 0f
        var downTx   = 0f
        var downTy   = 0f

        titleBar.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX; downRawY = event.rawY
                    downTx   = container.translationX; downTy = container.translationY
                    container.bringToFront()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    container.translationX = (downTx + dx)
                        .coerceIn(0f, (dm.widthPixels  - initW).toFloat())
                    container.translationY = (downTy + dy)
                        .coerceIn(0f, (dm.heightPixels - initH).toFloat())
                    true
                }
                else -> false
            }
        }

        return container
    }
}

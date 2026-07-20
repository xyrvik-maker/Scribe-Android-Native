package com.primaloptima.scribe

import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import android.app.Activity

/**
 * Bare-bones crash reporter. Uses only system views and a built-in Android
 * theme so it can never itself crash due to missing resources or a broken
 * custom theme.
 */
class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val trace = intent.getStringExtra(EXTRA_STACK_TRACE)
            ?: "No stack trace was captured."

        // Build the entire UI in code — zero dependency on XML layouts or
        // custom styles that might themselves be broken.
        val tv = TextView(this).apply {
            text = trace
            textSize = 11f
            setTextColor(0xFFCC0000.toInt())
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(24, 24, 24, 24)
            setTextIsSelectable(true)   // let the user long-press and copy
            fontFeatureSettings = "\"tnum\""
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFFFFFFFF.toInt())
            addView(tv)
        }

        setContentView(scroll)
    }

    companion object {
        const val EXTRA_STACK_TRACE = "extra_stack_trace"
    }
}

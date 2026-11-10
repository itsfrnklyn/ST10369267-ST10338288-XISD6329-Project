package com.example.nyamatsiapp

import android.app.Activity
import android.view.View

import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object ScreenInsets {

    fun apply(
        activity: Activity,
        root: View
    ) {

        WindowCompat.setDecorFitsSystemWindows(
            activity.window,
            false
        )

        // Dark status bar icons against the light background.
        WindowInsetsControllerCompat(
            activity.window,
            activity.window.decorView
        ).isAppearanceLightStatusBars = true

        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) {
                view, insets ->

            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            view.setPadding(
                initialLeft + bars.left,
                initialTop + bars.top,
                initialRight + bars.right,
                initialBottom + bars.bottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(root)
    }
}
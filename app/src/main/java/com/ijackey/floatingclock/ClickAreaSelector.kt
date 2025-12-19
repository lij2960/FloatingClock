package com.ijackey.floatingclock

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView

class ClickAreaSelector : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var selectorView: View
    private lateinit var positionText: TextView
    private lateinit var confirmButton: Button
    private var selectedX = 0f
    private var selectedY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createSelector()
    }

    private fun createSelector() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createSelectionOverlay()
    }


    
    private fun createSelectionOverlay() {
        // 创建小提示框，位于屏幕顶部
        val hintPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#E0000000"))
            setPadding(30, 20, 30, 20)
        }
        
        val hintText = TextView(this).apply {
            text = "3秒后点击选择位置"
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        
        val cancelBtn = Button(this).apply {
            text = "取消"
            textSize = 12f
            setPadding(20, 10, 20, 10)
            setOnClickListener { stopSelf() }
        }
        
        hintPanel.addView(hintText)
        hintPanel.addView(cancelBtn)
        
        selectorView = hintPanel
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }
        
        windowManager.addView(hintPanel, params)
        
        // 倒计时后开始选择
        startCountdown(hintText)
    }
    
    private fun startCountdown(hintText: TextView) {
        var countdown = 3
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        val countdownRunnable = object : Runnable {
            override fun run() {
                if (countdown > 0) {
                    hintText.text = "${countdown}秒后点击选择位置"
                    countdown--
                    handler.postDelayed(this, 1000)
                } else {
                    hintText.text = "点击屏幕选择位置"
                    enableSelection()
                }
            }
        }
        
        handler.post(countdownRunnable)
    }
    
    private fun enableSelection() {
        // 移除提示框
        windowManager.removeView(selectorView)
        
        // 创建透明选择层
        val selectionLayer = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    selectedX = event.rawX
                    selectedY = event.rawY
                    AutoClickService.instance?.addClickStep(selectedX, selectedY)
                    stopSelf()
                    true
                } else false
            }
        }
        
        val selectionParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        selectorView = selectionLayer
        windowManager.addView(selectionLayer, selectionParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::selectorView.isInitialized) {
            try {
                windowManager.removeView(selectorView)
            } catch (e: Exception) {
                // 忽略移除异常
            }
        }
    }
}
package com.ijackey.floatingclock

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class ComboSelector : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var selectorView: View
    private val handler = Handler(Looper.getMainLooper())
    
    private var step = 0 // 0: 第一次点击, 1: 滑动, 2: 第二次点击
    private var firstClickX = 0f
    private var firstClickY = 0f
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var swipeEndX = 0f
    private var swipeEndY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startSelection()
    }

    private fun startSelection() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showStepHint("选择第一个点击位置")
    }

    private fun showStepHint(message: String) {
        val hintPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#E0000000"))
            setPadding(30, 20, 30, 20)
        }
        
        val hintText = TextView(this).apply {
            text = message
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
        selectorView = hintPanel
        
        // 3秒后开始选择
        handler.postDelayed({
            hintText.text = "点击屏幕选择位置"
            handler.postDelayed({
                windowManager.removeView(hintPanel)
                createSelectionLayer()
            }, 1000)
        }, 3000)
    }
    
    private fun createSelectionLayer() {
        val selectionLayer = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    handleSelection(event.rawX, event.rawY)
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
    
    private fun handleSelection(x: Float, y: Float) {
        windowManager.removeView(selectorView)
        
        when (step) {
            0 -> {
                // 第一次点击
                firstClickX = x
                firstClickY = y
                step = 1
                showStepHint("选择滑动起点")
            }
            1 -> {
                // 滑动起点
                swipeStartX = x
                swipeStartY = y
                step = 2
                showStepHint("选择滑动终点")
            }
            2 -> {
                // 滑动终点
                swipeEndX = x
                swipeEndY = y
                step = 3
                showStepHint("选择第二个点击位置")
            }
            3 -> {
                // 第二次点击
                val secondClickX = x
                val secondClickY = y
                
                // 添加组合操作到AutoClickService
                AutoClickService.instance?.apply {
                    addClickStep(firstClickX, firstClickY, 1000L)
                    addSwipeStep(swipeStartX, swipeStartY, swipeEndX, swipeEndY, 500L, 1000L)
                    addClickStep(secondClickX, secondClickY, 1000L)
                }
                
                // 通知FloatingClockService更新步骤显示
                val updateIntent = Intent("UPDATE_STEPS")
                sendBroadcast(updateIntent)
                
                stopSelf()
            }
        }
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
package com.ijackey.floatingclock

import android.app.Service
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
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
import android.widget.NumberPicker
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class FloatingClockService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var clockText: TextView
    private lateinit var selectButton: Button
    private lateinit var swipeButton: Button
    private lateinit var startButton: Button
    private lateinit var timerButton: Button
    private lateinit var stepsButton: Button
    private var isSelectingSwipe = false
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateTimeRunnable: Runnable
    private var isSelectingPosition = false
    private var targetHour = 0
    private var targetMinute = 0
    private var targetSecond = 0
    private var timerRunnable: Runnable? = null
    private var isTimerSet = false
    


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createFloatingClock()
        startTimeUpdates()
        

    }

    private fun createFloatingClock() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 创建容器
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            
            // 创建背景
            val background = GradientDrawable().apply {
                setColor(Color.parseColor("#80000000"))
                cornerRadius = 16f
                setStroke(2, Color.WHITE)
            }
            setBackground(background)
        }
        
        // 创建时钟文本
        clockText = TextView(this).apply {
            text = "00:00:00"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        
        // 创建选择位置按钮
        selectButton = Button(this).apply {
            text = "添加点击"
            textSize = 10f
            setPadding(16, 8, 16, 8)
            setOnClickListener { toggleSelectMode() }
        }
        
        // 创建滑动按钮
        swipeButton = Button(this).apply {
            text = "添加滑动"
            textSize = 10f
            setPadding(16, 8, 16, 8)
            setOnClickListener { toggleSwipeMode() }
        }
        
        // 创建开始/停止按钮
        startButton = Button(this).apply {
            text = "开始点击"
            textSize = 10f
            setPadding(16, 8, 16, 8)
            setOnClickListener { toggleAutoClick() }
        }
        
        // 创建定时按钮
        timerButton = Button(this).apply {
            text = "设置定时"
            textSize = 10f
            setPadding(16, 8, 16, 8)
            setOnClickListener { showTimePickerDialog() }
        }
        
        // 创建步骤管理按钮
        stepsButton = Button(this).apply {
            text = "步骤(0)"
            textSize = 10f
            setPadding(16, 8, 16, 8)
            setOnClickListener { showStepsDialog() }
        }
        
        container.addView(clockText)
        container.addView(selectButton)
        container.addView(swipeButton)
        container.addView(startButton)
        container.addView(stepsButton)
        container.addView(timerButton)
        floatingView = container

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            if (isSelectingPosition) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager.addView(floatingView, params)
        makeDraggable(params)
    }
    
    private fun toggleSelectMode() {
        selectButton.text = "点击屏幕"
        enablePositionSelection(false)
    }
    
    private fun toggleSwipeMode() {
        if (swipeStartX == 0f && swipeStartY == 0f) {
            swipeButton.text = "选择起点"
            enablePositionSelection(true)
        } else {
            swipeStartX = 0f
            swipeStartY = 0f
            swipeButton.text = "添加滑动"
        }
    }
    
    private fun enablePositionSelection(isSwipe: Boolean) {
        // 创建倒计时提示
        val hintPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#E0000000"))
            setPadding(30, 20, 30, 20)
        }
        
        val hintText = TextView(this).apply {
            text = if (isSwipe) "3秒后拖动选择滑动" else "3秒后点击选择位置"
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        
        val cancelBtn = Button(this).apply {
            text = "取消"
            textSize = 12f
            setPadding(20, 10, 20, 10)
            setOnClickListener {
                try {
                    windowManager.removeView(hintPanel)
                } catch (e: Exception) {
                    android.util.Log.w("FloatingClockService", "Failed to remove hint panel", e)
                }
                isSelectingPosition = false
                if (isSwipe) {
                    swipeButton.text = "添加滑动"
                } else {
                    selectButton.text = "添加点击"
                }
            }
        }
        
        hintPanel.addView(hintText)
        hintPanel.addView(cancelBtn)
        
        val hintParams = WindowManager.LayoutParams(
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
        
        windowManager.addView(hintPanel, hintParams)
        
        // 倒计时
        var countdown = 3
        val countdownRunnable = object : Runnable {
            override fun run() {
                if (countdown > 0) {
                    hintText.text = if (isSwipe) "${countdown}秒后拖动选择滑动" else "${countdown}秒后点击选择位置"
                    countdown--
                    handler.postDelayed(this, 1000)
                } else {
                    hintText.text = if (isSwipe) "拖动屏幕选择滑动" else "点击屏幕选择位置"
                    // 移除提示框，创建选择层
                    try {
                        windowManager.removeView(hintPanel)
                        createSelectionLayer(isSwipe)
                    } catch (e: Exception) {
                        android.util.Log.w("FloatingClockService", "Failed to remove hint panel or create selection layer", e)
                        isSelectingPosition = false
                        if (isSwipe) {
                            swipeButton.text = "添加滑动"
                        } else {
                            selectButton.text = "添加点击"
                        }
                    }
                }
            }
        }
        handler.post(countdownRunnable)
    }
    
    private fun createSelectionLayer(isSwipe: Boolean) {
        val overlay = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val x = event.rawX
                    val y = event.rawY
                    
                    if (isSwipe) {
                        if (swipeStartX == 0f && swipeStartY == 0f) {
                            // 选择起点
                            swipeStartX = x
                            swipeStartY = y
                            swipeButton.text = "选择终点"
                            try {
                                windowManager.removeView(this)
                                // 继续选择终点
                                handler.postDelayed({
                                    createSelectionLayer(true)
                                }, 500)
                            } catch (e: Exception) {
                                android.util.Log.w("FloatingClockService", "Failed to remove selection layer", e)
                                isSelectingPosition = false
                                swipeButton.text = "添加滑动"
                            }
                        } else {
                            // 选择终点
                            AutoClickService.instance?.addSwipeStep(swipeStartX, swipeStartY, x, y)
                            updateStepsButton()
                            swipeButton.text = "添加滑动"
                            try {
                                windowManager.removeView(this)
                            } catch (e: Exception) {
                                android.util.Log.w("FloatingClockService", "Failed to remove selection layer", e)
                            }
                            isSelectingPosition = false
                            
                            // 延迟执行滑动，避免与界面操作冲突
                            handler.postDelayed({
                                AutoClickService.instance?.performSwipe(swipeStartX, swipeStartY, x, y, 800L)
                                swipeStartX = 0f
                                swipeStartY = 0f
                            }, 1000)
                        }
                    } else {
                        AutoClickService.instance?.addClickStep(x, y)
                        updateStepsButton()
                        selectButton.text = "添加点击"
                        try {
                            windowManager.removeView(this)
                        } catch (e: Exception) {
                            android.util.Log.w("FloatingClockService", "Failed to remove selection layer", e)
                        }
                        isSelectingPosition = false
                    }
                    true
                } else false
            }
        }
        
        val overlayParams = WindowManager.LayoutParams(
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
        
        windowManager.addView(overlay, overlayParams)
    }
    
    private fun disablePositionSelection() {
        // 不需要特别处理
    }
    
    private fun toggleAutoClick() {
        try {
            if (AutoClickService.isAutoClickEnabled) {
                AutoClickService.instance?.stopAutoClick()
                startButton.text = "开始点击"
            } else {
                if (AutoClickService.instance != null && AutoClickService.clickSteps.isNotEmpty()) {
                    AutoClickService.instance?.startAutoClick()
                    startButton.text = "停止点击"
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingClockService", "Error in toggleAutoClick", e)
            startButton.text = "开始点击"
        }
    }
    
    private fun updateStepsButton() {
        val count = AutoClickService.instance?.getClickStepsCount() ?: 0
        stepsButton.text = "步骤($count)"
    }
    
    private fun showStepsDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E0000000"))
            setPadding(40, 40, 40, 40)
        }
        
        val titleText = TextView(this).apply {
            text = "步骤管理"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 16f
        }
        
        val stepsScrollView = android.widget.ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            )
        }
        
        val stepsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        val steps = AutoClickService.clickSteps
        val headerText = TextView(this).apply {
            text = "当前步骤数: ${steps.size}"
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        stepsContainer.addView(headerText)
        
        steps.forEachIndexed { index, step ->
            val stepLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 10, 0, 10)
            }
            
            val stepText = TextView(this).apply {
                val stepInfo = when (step.type) {
                    AutoClickService.StepType.CLICK -> "步骤${index + 1}: 点击 (${step.x.toInt()}, ${step.y.toInt()})"
                    AutoClickService.StepType.SWIPE -> "步骤${index + 1}: 滑动 (${step.x.toInt()}, ${step.y.toInt()}) 到 (${step.endX.toInt()}, ${step.endY.toInt()})"
                }
                text = "$stepInfo\n间隔: ${step.delay}ms"
                setTextColor(Color.WHITE)
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val editButton = Button(this).apply {
                text = "编辑"
                textSize = 10f
                setPadding(10, 5, 10, 5)
                setOnClickListener {
                    windowManager.removeView(dialogLayout)
                    showEditStepDialog(index)
                }
            }
            
            stepLayout.addView(stepText)
            stepLayout.addView(editButton)
            stepsContainer.addView(stepLayout)
        }
        
        stepsScrollView.addView(stepsContainer)
        

        
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        val clearButton = Button(this).apply {
            text = "清空步骤"
            setOnClickListener {
                AutoClickService.instance?.clearClickSteps()
                updateStepsButton()
                windowManager.removeView(dialogLayout)
            }
        }
        
        val closeButton = Button(this).apply {
            text = "关闭"
            setOnClickListener {
                windowManager.removeView(dialogLayout)
            }
        }
        
        buttonLayout.addView(clearButton)
        buttonLayout.addView(closeButton)
        
        dialogLayout.addView(titleText)
        dialogLayout.addView(stepsScrollView)
        dialogLayout.addView(buttonLayout)
        
        val dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            0,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        windowManager.addView(dialogLayout, dialogParams)
    }
    
    private fun showTimePickerDialog() {
        val now = Calendar.getInstance()
        targetHour = now.get(Calendar.HOUR_OF_DAY)
        targetMinute = now.get(Calendar.MINUTE)
        targetSecond = now.get(Calendar.SECOND)
        
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E0000000"))
            setPadding(40, 40, 40, 40)
        }
        
        val titleText = TextView(this).apply {
            text = "设置定时点击时间"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 16f
        }
        
        val timeDisplay = TextView(this).apply {
            text = String.format("%02d:%02d:%02d", targetHour, targetMinute, targetSecond)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 24f
            setPadding(0, 20, 0, 20)
        }
        
        val hourButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        val hourMinus = Button(this).apply {
            text = "时-"
            setOnClickListener {
                targetHour = if (targetHour > 0) targetHour - 1 else 23
                timeDisplay.text = String.format("%02d:%02d:%02d", targetHour, targetMinute, targetSecond)
            }
        }
        
        val hourPlus = Button(this).apply {
            text = "时+"
            setOnClickListener {
                targetHour = if (targetHour < 23) targetHour + 1 else 0
                timeDisplay.text = String.format("%02d:%02d:%02d", targetHour, targetMinute, targetSecond)
            }
        }
        
        val minuteButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        val minuteMinus = Button(this).apply {
            text = "分-"
            setOnClickListener {
                targetMinute = if (targetMinute > 0) targetMinute - 1 else 59
                timeDisplay.text = String.format("%02d:%02d:%02d", targetHour, targetMinute, targetSecond)
            }
        }
        
        val minutePlus = Button(this).apply {
            text = "分+"
            setOnClickListener {
                targetMinute = if (targetMinute < 59) targetMinute + 1 else 0
                timeDisplay.text = String.format("%02d:%02d:%02d", targetHour, targetMinute, targetSecond)
            }
        }
        
        val secondButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        val secondMinus = Button(this).apply {
            text = "秒-"
            setOnClickListener {
                targetSecond = if (targetSecond > 0) targetSecond - 1 else 59
                timeDisplay.text = String.format("%02d:%02d:%02d", targetHour, targetMinute, targetSecond)
            }
        }
        
        val secondPlus = Button(this).apply {
            text = "秒+"
            setOnClickListener {
                targetSecond = if (targetSecond < 59) targetSecond + 1 else 0
                timeDisplay.text = String.format("%02d:%02d:%02d", targetHour, targetMinute, targetSecond)
            }
        }
        
        val actionButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        val confirmButton = Button(this).apply {
            text = "确认"
            setOnClickListener {
                setTimer()
                windowManager.removeView(dialogLayout)
            }
        }
        
        val cancelButton = Button(this).apply {
            text = "取消"
            setOnClickListener {
                windowManager.removeView(dialogLayout)
            }
        }
        
        val clearButton = Button(this).apply {
            text = "清除"
            setOnClickListener {
                clearTimer()
                windowManager.removeView(dialogLayout)
            }
        }
        
        hourButtons.addView(hourMinus)
        hourButtons.addView(hourPlus)
        minuteButtons.addView(minuteMinus)
        minuteButtons.addView(minutePlus)
        secondButtons.addView(secondMinus)
        secondButtons.addView(secondPlus)
        actionButtons.addView(confirmButton)
        actionButtons.addView(cancelButton)
        actionButtons.addView(clearButton)
        
        dialogLayout.addView(titleText)
        dialogLayout.addView(timeDisplay)
        dialogLayout.addView(hourButtons)
        dialogLayout.addView(minuteButtons)
        dialogLayout.addView(secondButtons)
        dialogLayout.addView(actionButtons)
        
        val dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            0,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        windowManager.addView(dialogLayout, dialogParams)
    }
    
    private fun setTimer() {
        // 取消之前的定时器
        timerRunnable?.let { handler.removeCallbacks(it) }
        
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, targetSecond)
            set(Calendar.MILLISECOND, 0)
            
            // 如果目标时间已过，设置为明天
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        val delay = target.timeInMillis - now.timeInMillis
        
        timerButton.text = String.format("%02d:%02d:%02d", targetHour, targetMinute, targetSecond)
        isTimerSet = true
        
        timerRunnable = Runnable {
            if (!AutoClickService.isAutoClickEnabled) {
                toggleAutoClick()
            }
            isTimerSet = false
            timerButton.text = "设置定时"
        }
        
        handler.postDelayed(timerRunnable!!, delay)
    }
    
    private fun clearTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        isTimerSet = false
        timerButton.text = "设置定时"
    }
    
    private fun showEditStepDialog(stepIndex: Int) {
        val step = AutoClickService.clickSteps[stepIndex]
        
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E0000000"))
            setPadding(40, 40, 40, 40)
        }
        
        val titleText = TextView(this).apply {
            text = "编辑步骤${stepIndex + 1}"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 16f
        }
        
        val stepInfoText = TextView(this).apply {
            val stepInfo = when (step.type) {
                AutoClickService.StepType.CLICK -> "点击 (${step.x.toInt()}, ${step.y.toInt()})"
                AutoClickService.StepType.SWIPE -> "滑动 (${step.x.toInt()}, ${step.y.toInt()}) 到 (${step.endX.toInt()}, ${step.endY.toInt()})"
            }
            text = stepInfo
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 14f
            setPadding(0, 20, 0, 20)
        }
        
        val delayText = TextView(this).apply {
            text = "间隔时间 (毫秒)"
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        
        var currentDelay = step.delay
        val delayDisplay = TextView(this).apply {
            text = "${currentDelay}ms"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 20f
            setPadding(0, 10, 0, 10)
        }
        
        val delayButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        val minus1000 = Button(this).apply {
            text = "-1s"
            setOnClickListener {
                currentDelay = maxOf(100, currentDelay - 1000)
                delayDisplay.text = "${currentDelay}ms"
            }
        }
        
        val minus100 = Button(this).apply {
            text = "-100"
            setOnClickListener {
                currentDelay = maxOf(100, currentDelay - 100)
                delayDisplay.text = "${currentDelay}ms"
            }
        }
        
        val plus100 = Button(this).apply {
            text = "+100"
            setOnClickListener {
                currentDelay += 100
                delayDisplay.text = "${currentDelay}ms"
            }
        }
        
        val plus1000 = Button(this).apply {
            text = "+1s"
            setOnClickListener {
                currentDelay += 1000
                delayDisplay.text = "${currentDelay}ms"
            }
        }
        
        delayButtons.addView(minus1000)
        delayButtons.addView(minus100)
        delayButtons.addView(plus100)
        delayButtons.addView(plus1000)
        
        val actionButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        val saveButton = Button(this).apply {
            text = "保存"
            setOnClickListener {
                AutoClickService.instance?.updateStepDelay(stepIndex, currentDelay)
                windowManager.removeView(dialogLayout)
                showStepsDialog()
            }
        }
        
        val cancelButton = Button(this).apply {
            text = "取消"
            setOnClickListener {
                windowManager.removeView(dialogLayout)
                showStepsDialog()
            }
        }
        
        actionButtons.addView(saveButton)
        actionButtons.addView(cancelButton)
        
        dialogLayout.addView(titleText)
        dialogLayout.addView(stepInfoText)
        dialogLayout.addView(delayText)
        dialogLayout.addView(delayDisplay)
        dialogLayout.addView(delayButtons)
        dialogLayout.addView(actionButtons)
        
        val dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            0,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        windowManager.addView(dialogLayout, dialogParams)
    }
    


    private fun makeDraggable(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        floatingView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.y < clockText.height + 20) { // 只有点击时钟区域才能拖拽
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = true
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    false
                }
                else -> false
            }
        }
    }

    private fun startTimeUpdates() {
        updateTimeRunnable = object : Runnable {
            override fun run() {
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                clockText.text = currentTime
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimeRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // 清理所有Handler回调
        handler.removeCallbacksAndMessages(null)
        
        // 停止自动点击
        AutoClickService.instance?.stopAutoClick()
        
        // 重置状态
        isSelectingPosition = false
        swipeStartX = 0f
        swipeStartY = 0f
        
        // 安全移除所有窗口
        try {
            if (::floatingView.isInitialized) {
                windowManager.removeView(floatingView)
            }
        } catch (e: Exception) {
            android.util.Log.w("FloatingClockService", "Failed to remove floating view", e)
        }
    }
}
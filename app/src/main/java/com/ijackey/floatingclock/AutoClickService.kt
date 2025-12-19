package com.ijackey.floatingclock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.os.Handler
import android.os.Looper

class AutoClickService : AccessibilityService() {
    companion object {
        var instance: AutoClickService? = null
        var isAutoClickEnabled = false
        var clickSteps = mutableListOf<ClickStep>()
        var currentStepIndex = 0
        var clickInterval = 1000L
    }
    
    data class ClickStep(
        val x: Float, 
        val y: Float, 
        val delay: Long = 1000L,
        val type: StepType = StepType.CLICK,
        val endX: Float = 0f,
        val endY: Float = 0f,
        val duration: Long = 500L
    )
    
    enum class StepType {
        CLICK, SWIPE
    }

    private val handler = Handler(Looper.getMainLooper())
    private var clickRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun startAutoClick() {
        if (isAutoClickEnabled || clickSteps.isEmpty()) return
        
        // 确保停止之前的任务
        stopAutoClick()
        
        isAutoClickEnabled = true
        currentStepIndex = 0
        executeNextStep()
    }
    
    private fun executeNextStep() {
        if (!isAutoClickEnabled || clickSteps.isEmpty() || instance == null) return
        
        try {
            val step = clickSteps[currentStepIndex]
            when (step.type) {
                StepType.CLICK -> performClick(step.x, step.y)
                StepType.SWIPE -> performSwipe(step.x, step.y, step.endX, step.endY, step.duration)
            }
            
            currentStepIndex = (currentStepIndex + 1) % clickSteps.size
            
            if (isAutoClickEnabled && instance != null) {
                clickRunnable = Runnable {
                    executeNextStep()
                }
                handler.postDelayed(clickRunnable!!, step.delay)
            }
        } catch (e: Exception) {
            android.util.Log.e("AutoClickService", "Error in executeNextStep", e)
            stopAutoClick()
        }
    }

    fun stopAutoClick() {
        isAutoClickEnabled = false
        clickRunnable?.let { 
            handler.removeCallbacks(it)
            clickRunnable = null
        }
    }

    fun addClickStep(x: Float, y: Float, delay: Long = 1000L) {
        clickSteps.add(ClickStep(x, y, delay, StepType.CLICK))
    }
    
    fun addSwipeStep(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 500L, delay: Long = 1000L) {
        clickSteps.add(ClickStep(startX, startY, delay, StepType.SWIPE, endX, endY, duration))
    }
    

    
    fun clearClickSteps() {
        clickSteps.clear()
        currentStepIndex = 0
    }
    
    fun getClickStepsCount(): Int = clickSteps.size
    
    fun updateStepDelay(index: Int, newDelay: Long) {
        if (index >= 0 && index < clickSteps.size) {
            val step = clickSteps[index]
            clickSteps[index] = step.copy(delay = newDelay)
        }
    }

    private fun performClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder()
            .addStroke(strokeDescription)
            .build()
        
        dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
            }
            
            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
            }
        }, null)
    }
    
    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long) {
        android.util.Log.d("AutoClickService", "performSwipe called: ($startX, $startY) to ($endX, $endY)")
        
        // 确保持续时间至少100ms
        val swipeDuration = if (duration < 100) 300L else duration
        
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, swipeDuration)
        val gesture = GestureDescription.Builder()
            .addStroke(strokeDescription)
            .build()
        
        val result = dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                android.util.Log.d("AutoClickService", "Swipe completed")
            }
            
            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                android.util.Log.d("AutoClickService", "Swipe cancelled")
            }
        }, null)
        
        android.util.Log.d("AutoClickService", "dispatchGesture result: $result, duration: $swipeDuration")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopAutoClick()
    }
}
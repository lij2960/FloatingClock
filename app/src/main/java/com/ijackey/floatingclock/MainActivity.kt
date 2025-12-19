package com.ijackey.floatingclock

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ijackey.floatingclock.ui.theme.FloatingClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isServiceRunning by remember { mutableStateOf(false) }
            var isAutoClickRunning by remember { mutableStateOf(false) }
            
            FloatingClockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ClockControlPanel(
                        modifier = Modifier.padding(innerPadding),
                        onStartClock = { 
                            startFloatingClock()
                            isServiceRunning = true
                        },
                        onStopClock = { 
                            stopFloatingClock()
                            isServiceRunning = false
                        },
                        isRunning = isServiceRunning,
                        onCheckAccessibility = { checkAccessibilityService() }
                    )
                }
            }
        }
    }

    private fun startFloatingClock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(this, FloatingClockService::class.java)
        startService(intent)
        Toast.makeText(this, "悬浮时钟已启动", Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingClock() {
        val intent = Intent(this, FloatingClockService::class.java)
        stopService(intent)
        Toast.makeText(this, "悬浮时钟已停止", Toast.LENGTH_SHORT).show()
    }

    private fun checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "请启用无障碍服务后使用自动点击功能", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "无障碍服务已启用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )
        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return services?.contains("${packageName}/.AutoClickService") == true
        }
        return false
    }
}

@Composable
fun ClockControlPanel(
    modifier: Modifier = Modifier,
    onStartClock: () -> Unit,
    onStopClock: () -> Unit,
    isRunning: Boolean,
    onCheckAccessibility: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "悬浮时钟控制",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onStartClock,
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("启动悬浮时钟")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onStopClock,
            enabled = isRunning,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("停止悬浮时钟")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = if (isRunning) "悬浮时钟运行中" else "悬浮时钟已停止",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "自动点击功能在悬浮窗中操作",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onCheckAccessibility,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("检查无障碍服务")
        }
    }
}
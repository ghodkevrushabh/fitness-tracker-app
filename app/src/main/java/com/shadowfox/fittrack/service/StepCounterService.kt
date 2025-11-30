package com.shadowfox.fittrack.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.shadowfox.fittrack.R
import com.shadowfox.fittrack.data.StepDataStore
import kotlinx.coroutines.*

class StepCounterService : Service(), SensorEventListener {

    companion object {
        var serviceInstance: StepCounterService? = null
        private var currentStepCount: Int = 0

        fun getSteps(): Int {
            return currentStepCount
        }
    }

    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var notificationManager: NotificationManager? = null
    private var initialSensorOffset: Int = 0
    private var stepsToday: Int = 0
    private var dailyGoal: Int = 10000
    private var goalAchievedFlag: Boolean = false

    private val CHANNEL_ID = "StepTrackerChannel"
    private val NOTIFICATION_ID = 1

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Safety Check: Do we have permission?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // 2. Load Data
        serviceScope.launch {
            dailyGoal = StepDataStore.getGoal(applicationContext)
            initialSensorOffset = StepDataStore.checkAndResetDaily(applicationContext, 0)
        }

        // 3. Handle Reset
        if (intent?.action == "STOP_AND_RESET") {
            initialSensorOffset = 0
            stepsToday = 0
            currentStepCount = 0
            serviceScope.launch {
                StepDataStore.saveSteps(applicationContext, 0)
                updateNotification()
            }
        }

        // 4. CRITICAL CRASH FIX: Safe Start Logic
        try {
            val notification = buildNotification()
            // Only use the strict HEALTH type if we are running on Android 14+ AND we are targeting it
            if (Build.VERSION.SDK_INT >= 34 && applicationInfo.targetSdkVersion >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                // For older targets or older phones, use the standard start
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf() // Stop safely instead of crashing
            return START_NOT_STICKY
        }

        registerSensorListener()
        return START_STICKY
    }

    private fun registerSensorListener() {
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val rawSensorValue = event.values[0].toInt()

            if (initialSensorOffset == 0) {
                initialSensorOffset = rawSensorValue
            }

            stepsToday = (rawSensorValue - initialSensorOffset).coerceAtLeast(0)
            currentStepCount = stepsToday

            serviceScope.launch {
                StepDataStore.saveSteps(applicationContext, stepsToday)
                withContext(Dispatchers.Main) {
                    updateNotification()
                    checkGoalCompletion()
                }
            }
        }
    }

    private fun checkGoalCompletion() {
        if (stepsToday >= dailyGoal && !goalAchievedFlag) {
            goalAchievedFlag = true
            val goalNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GOAL ACHIEVED!")
                .setContentText("You hit your goal of $dailyGoal steps!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            notificationManager?.notify(NOTIFICATION_ID + 1, goalNotification)
        } else if (stepsToday < dailyGoal) {
            goalAchievedFlag = false
        }
    }

    private fun updateNotification(): Notification {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FitTrack Steps")
            .setContentText("Today: $stepsToday / $dailyGoal steps")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        notificationManager?.notify(NOTIFICATION_ID, notification)
        return notification
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Step Tracker Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FitTrack is Running")
            .setContentText("Initializing step counter...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        serviceInstance = null
    }
}
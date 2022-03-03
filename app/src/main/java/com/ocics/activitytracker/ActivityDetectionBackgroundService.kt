package com.ocics.activitytracker

import android.app.IntentService
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.tasks.Task

class ActivityDetectionBackgroundService:Service() {
    private val TAG = "ActivityDetectionBackgroundService"
    lateinit var mActivityRecognitionClient: ActivityRecognitionClient
    lateinit var mPendingIntent: PendingIntent

    inner class MyBinder : Binder() {
        fun getService() : ActivityDetectionBackgroundService? {
            return this@ActivityDetectionBackgroundService
        }
    }

    override fun onBind(p0: Intent?): IBinder = MyBinder()

    override fun onCreate() {
        super.onCreate()

        mActivityRecognitionClient = ActivityRecognitionClient(this)
        val intent = Intent(this, ActivityTransitionService::class.java)
        mPendingIntent = PendingIntent.getService(this, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        requestActivityUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeActivityUpdates()
    }

    private fun requestActivityUpdates() {
        val task: Task<Void> = mActivityRecognitionClient.requestActivityUpdates(1000L, mPendingIntent)
        task.addOnSuccessListener {
            Log.d(TAG, "Task added successfully")
        }
        task.addOnFailureListener {
            Log.d(TAG, "Task added failed")
        }
    }

    private fun removeActivityUpdates() {
        val task: Task<Void> = mActivityRecognitionClient.removeActivityUpdates(
            mPendingIntent)

        task.addOnSuccessListener {
            Log.d(TAG, "Task removed successfully")
        }
        task.addOnFailureListener {
            Log.d(TAG, "Task removed failed")
        }
    }

    class ActivityTransitionService: IntentService(ActivityTransitionService::class.simpleName) {
        private val TAG = "ActivityTransitionService"
        val ACTIVITY_TRANSITION_INTENT = "ACTIVITY_TRANSITION_INTENT"

        override fun onHandleIntent(intent: Intent?) {
            Log.d(TAG, "onHandleIntent, ${intent.toString()}")
            val result = intent?.let {
                ActivityRecognitionResult.extractResult(it)
            }
            result?.probableActivities?.filter {
                it.type == DetectedActivity.STILL ||
                        it.type == DetectedActivity.WALKING ||
                        it.type == DetectedActivity.RUNNING ||
                        it.type == DetectedActivity.IN_VEHICLE
            }?.sortedByDescending { it.confidence }?.run {
                if (isNotEmpty()) {
                    notifyActivity(this[0])
                }
            }
        }

        private fun notifyActivity(activity: DetectedActivity) {
            val intent = Intent(ACTIVITY_TRANSITION_INTENT)
            intent.putExtra("type", activity.type)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

}
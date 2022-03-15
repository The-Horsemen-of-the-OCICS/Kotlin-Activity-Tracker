package com.ocics.activitytracker

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.tasks.Task

class ActivityRecognitionService:Service() {
    private val TAG = "ActivityRecognitionService"
    lateinit var mPendingIntent: PendingIntent

    inner class MyBinder : Binder() {
        fun getService() : ActivityRecognitionService? {
            return this@ActivityRecognitionService
        }
        val serverInstance: ActivityRecognitionService
        get() = this@ActivityRecognitionService
    }

    override fun onBind(intent: Intent?): IBinder = MyBinder()

    override fun onCreate() {
        super.onCreate()

        val intent = Intent(this, ActivityRecognitionReceiver::class.java)
        mPendingIntent = PendingIntent.getBroadcast(this, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        requestActivityUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeActivityUpdates()
    }

    private fun requestActivityUpdates() {
        val task: Task<Void> = ActivityRecognitionClient(this).requestActivityUpdates(1000L, mPendingIntent)
        task.addOnSuccessListener {
            Log.d(TAG, "Task added successfully")
        }
        task.addOnFailureListener {
            Log.d(TAG, "Task added failed")
        }
    }

    private fun removeActivityUpdates() {
        val task: Task<Void> = ActivityRecognitionClient(this).removeActivityUpdates(
            mPendingIntent)

        task.addOnSuccessListener {
            Log.d(TAG, "Task removed successfully")
        }
        task.addOnFailureListener {
            Log.d(TAG, "Task removed failed")
        }
    }


}
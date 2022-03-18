package com.ocics.activitytracker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity


class ActivityRecognitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras
        if (bundle != null) {
            for (key in bundle.keySet()) {
                Log.d("ActivityRecognitionReceiver", key + " : " + if (bundle[key] != null) bundle[key] else "NULL")
            }
        }
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = intent.let {
                ActivityRecognitionResult.extractResult(it)
            }

            result?.probableActivities?.filter {
                it.type == DetectedActivity.STILL ||
                        it.type == DetectedActivity.WALKING ||
                        it.type == DetectedActivity.RUNNING ||
                        it.type == DetectedActivity.IN_VEHICLE
            }?.sortedByDescending { it.confidence }?.run {
                if (isNotEmpty()) {
                    notifyActivity(context, this[0])
                }
            }
        }
    }

    private fun notifyActivity(context: Context, activity: DetectedActivity) {
        Log.d("ActivityRecognitionReceiver", "notifyActivity, ${activity.type}")

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("type", activity.type)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 1002, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        pendingIntent.send()
    }
}
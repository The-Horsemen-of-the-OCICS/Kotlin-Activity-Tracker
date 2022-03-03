package com.ocics.activitytracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import java.text.SimpleDateFormat
import java.util.*

class ActivityTransitionReceiver : BroadcastReceiver() {
    val ACTIVITY_TRANSITION_INTENT = "ACTIVITY_TRANSITION_INTENT"

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
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
        val intent = Intent(ACTIVITY_TRANSITION_INTENT)
        intent.putExtra("type", activity.type)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}
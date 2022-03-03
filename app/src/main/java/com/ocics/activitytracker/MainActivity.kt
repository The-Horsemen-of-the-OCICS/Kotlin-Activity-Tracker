package com.ocics.activitytracker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.ocics.activitytracker.databinding.ActivityMainBinding
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    val TAG = "MainActivity"
    val ACTIVITY_TRANSITION_INTENT = "ACTIVITY_TRANSITION_INTENT"

    var lastActivity = ""
    var lastTransitionTime = System.currentTimeMillis()

    lateinit var mBroadcastReceiver: BroadcastReceiver
    lateinit var mBinding: ActivityMainBinding
    lateinit var mClient: ActivityRecognitionClient

    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create view binding
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mClient = ActivityRecognition.getClient(this)

        // Init map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Init receiver
        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val extras = intent.extras?.keySet()?.map { "$it: ${intent.extras?.get(it)}" }
                    ?.joinToString { it }
                Log.d(TAG, intent.toString() + extras.toString())
                val type = intent.getIntExtra("type", -1)
                onActivityChanged(type)
            }
        }
        getTimeText()
        checkPermissionAndStartService()
    }

    private fun getTimeText() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        mBinding.timeText.text = sdf.format(Date())
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter(ACTIVITY_TRANSITION_INTENT))
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
    }

    private fun checkPermissionAndStartService() {
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) + ContextCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    android.Manifest.permission.ACTIVITY_RECOGNITION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), 1000
            )
        } else {
            startService(Intent(this, ActivityDetectionBackgroundService::class.java))
            requestTransitionUpdates()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    stopService(Intent(this, ActivityDetectionBackgroundService::class.java))
                    startService(Intent(this, ActivityDetectionBackgroundService::class.java))
                } else {
                    mBinding.activityText.text = "No Permission Granted"
                }
            }
        }
    }

    private fun requestTransitionUpdates() {
        mClient.requestActivityTransitionUpdates(
            getActivityTransitionRequest(),
            getPendingIntent()
        )
            .addOnSuccessListener {
                Log.d(TAG, "Task added successfully")
            }
            .addOnFailureListener {
                Log.d(TAG, "Task added failed")
            }
    }

    private fun getActivityTransitionRequest(): ActivityTransitionRequest {
        val transitions = mutableListOf<ActivityTransition>()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        return ActivityTransitionRequest(transitions)
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        return PendingIntent.getBroadcast(this, 112, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun onActivityChanged(activityType: Int) {
        Log.d(TAG, "onActivityChanged: $activityType")
        var curActivity = ""
        var curDrawable = AppCompatResources.getDrawable(this, R.drawable.unknown_icon)
        when (activityType) {
            DetectedActivity.STILL -> {
                curActivity = "Standing Still"
                curDrawable = AppCompatResources.getDrawable(this, R.drawable.still_icon)
            }
            DetectedActivity.WALKING -> {
                curActivity = "Walking"
                curDrawable = AppCompatResources.getDrawable(this, R.drawable.walk_icon)
            }
            DetectedActivity.RUNNING -> {
                curActivity = "Running"
                curDrawable = AppCompatResources.getDrawable(this, R.drawable.run_icon)
            }
            DetectedActivity.IN_VEHICLE -> {
                curActivity = "In Vehicle"
                curDrawable = AppCompatResources.getDrawable(this, R.drawable.vehicle_icon)
            }
            else -> {
                curActivity = "Unknown"
            }
        }
        // Activity changed
        if (lastActivity != curActivity) {
            if (lastActivity != "") {
                val tempTime = System.currentTimeMillis()
                val millDiff = tempTime - lastTransitionTime
                val differenceInMinutes = millDiff / (60 * 1000) % 60
                val differenceInSeconds = millDiff / 1000 % 60
                Toast.makeText(
                    this,
                    "You were $lastActivity for $differenceInMinutes minutes $differenceInSeconds seconds",
                    Toast.LENGTH_LONG
                ).show()
                lastTransitionTime = tempTime
            }
            lastActivity = curActivity
            mBinding.activityText.text = "Current Activity: " + curActivity
            mBinding.activityImage.setImageDrawable(curDrawable)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        updateLocationUI()
        getDeviceLocation()
    }

    private fun updateLocationUI() {
        try {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getDeviceLocation() {
        try {
            val locationResult = mFusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Current location: ${task.result}")
                    val cameraPosition = CameraPosition.Builder().target(LatLng(task.result.latitude, task.result.longitude)).zoom(12f).build()
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

                } else {
                    Log.d(TAG, "Current location is null.")
                }
            }
        } catch (e: Exception) {
            Log.e("Exception: %s", e.message, e)
        }
    }
}
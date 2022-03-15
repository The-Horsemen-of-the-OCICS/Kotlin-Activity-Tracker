package com.ocics.activitytracker

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.ocics.activitytracker.databinding.ActivityMainBinding
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    val TAG = "MainActivity"

    var lastActivity = ""
    var lastTransitionTime = System.currentTimeMillis()

    lateinit var mBinding: ActivityMainBinding

    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create view binding
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Init map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

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
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        stopService(Intent(this, ActivityRecognitionService::class.java))
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra("type")) {
            val type = intent.getIntExtra("type", -1)
            onActivityChanged(type)
        }
    }

    private fun checkPermissionAndStartService() {
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) + ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ), 1000
            )
        } else {
            startServiceAndTasks()
        }
    }

    private fun startServiceAndTasks() {
        Log.d(TAG, "startServiceAndTasks")
        stopServiceAndTasks()

        startService(Intent(this, ActivityRecognitionService::class.java))
        val pendingIntent = PendingIntent.getBroadcast(this, 2001,  Intent(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val task = ActivityRecognitionClient(this).requestActivityTransitionUpdates(getActivityTransitionRequest(),
            pendingIntent)
        task.run {
            addOnSuccessListener {
                Log.d(TAG, "requestActivityTransitionUpdates added")
            }
            addOnFailureListener {
                Log.d(TAG, "requestActivityTransitionUpdates failed")
            }
        }

    }

    private fun stopServiceAndTasks() {
        stopService(Intent(this, ActivityRecognitionService::class.java))
        val pendingIntent = PendingIntent.getBroadcast(this, 2001,  Intent(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val task = ActivityRecognitionClient(this).removeActivityTransitionUpdates(
            pendingIntent)

        task.run {
            addOnSuccessListener {
                Log.d(TAG, "removeActivityTransitionUpdates added")
            }
            addOnFailureListener {
                Log.d(TAG, "removeActivityTransitionUpdates failed")
            }
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
                    Log.d(TAG, ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION).toString())
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED) {
                        updateLocationUI()
                        getDeviceLocation()
                    }
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACTIVITY_RECOGNITION) ==
                        PackageManager.PERMISSION_GRANTED){
                        startServiceAndTasks()
                    }
                } else {
                    mBinding.activityText.text = "No Permission Granted"
                }
            }
        }
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
package com.example.dustmeasurement

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.dustmeasurement.data.Repository
import com.example.dustmeasurement.data.models.airquality.Grade
import com.example.dustmeasurement.data.models.airquality.MeasuredValue
import com.example.dustmeasurement.data.models.monitoringStation.MonitoringStation
import com.example.dustmeasurement.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var cancellationTokenSourcd: CancellationTokenSource? = null

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val scope = MainScope()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        bindViews()
        initVariables()
        requestLocationPermissions()

    }

    override fun onDestroy() {
        super.onDestroy()
        cancellationTokenSourcd?.cancel()
        scope.cancel()
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val locationPermissionGranted =
            requestCode == REQUEST_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

        val backgroundLocationPermissionGranted =
            requestCode == REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!backgroundLocationPermissionGranted){
                requestBackgroundLocationPermissions()
            } else{
                fetchAirQualityData()
            }
        } else {
            if (!locationPermissionGranted) {
                finish()
            } else {
                fetchAirQualityData()
            }
        }
    }

    private fun bindViews() {
        binding.refresh.setOnRefreshListener {
            fetchAirQualityData()
        }
    }

    private fun initVariables(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun requestLocationPermissions(){
        ActivityCompat.requestPermissions(
                this,
                arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_ACCESS_LOCATION_PERMISSIONS
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocationPermissions(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS
        )
    }


    @SuppressLint("MissingPermission")
    private fun fetchAirQualityData() {
        cancellationTokenSourcd = CancellationTokenSource()

        fusedLocationProviderClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSourcd!!.token
        ).addOnSuccessListener { location ->
            scope.launch {
                binding.tvErrorDescription.visibility = View.GONE
                try {
                    val monitoringStation =
                        Repository.getNearbyMonitoringStation(location.latitude, location.longitude)

                    val measuredValues =
                        Repository.getLatestAirQualityData(monitoringStation!!.stationName!!)

                    displayAirQualityData(monitoringStation, measuredValues!!)
                } catch (e: Exception){
                    binding.tvErrorDescription.visibility = View.VISIBLE
                    binding.contentsLayout.alpha = 0F
                } finally {
                    binding.progressBar.visibility = View.GONE
                    binding.refresh.isRefreshing = false
                }

            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun displayAirQualityData(monitoringStation: MonitoringStation, measuredValue: MeasuredValue){
        binding.contentsLayout.animate()
            .alpha(1F)
            .start()


        binding.tvMeasuringStationName.text = monitoringStation.stationName
        binding.tvMeasuringStationAddress.text = monitoringStation.addr

        (measuredValue.khaiGrade?: Grade.UNKNOWN).let {  grade ->
            binding.root.setBackgroundResource(grade.colorResId)
            binding.tvTotalGradeLabel.text = grade.label
            binding.tvTotalGradleEmoji.text = grade.emoji
        }

        with(measuredValue) {
            binding.tvFineDustInformation.text =
                "미세먼지: $pm10Value ㎍/㎥ ${(pm10Grade?: Grade.UNKNOWN).emoji}"
            binding.tvUltraFineDustInformation.text =
                "초미세먼지: $pm25Value ㎍/㎥ ${(pm25Grade?: Grade.UNKNOWN).emoji}"

            with(binding.so2Item){
                tvLabel.text = "아황산가스"
                tvGradle.text = (so2Grade?: Grade.UNKNOWN).toString()
                tvValue.text = "$so2Value ppm"
            }

            with(binding.coItem){
                tvLabel.text = "일산화탄소소"
                tvGradle.text = (coGrade?: Grade.UNKNOWN).toString()
                tvValue.text = "$coValue ppm"
            }

            with(binding.o3Item){
                tvLabel.text = "오존"
                tvGradle.text = (o3Grade?: Grade.UNKNOWN).toString()
                tvValue.text = "$o3Value ppm"
            }

            with(binding.no2Item){
                tvLabel.text = "이산화질소"
                tvGradle.text = (no2Grade?: Grade.UNKNOWN).toString()
                tvValue.text = "$no2Value ppm"
            }


        }
    }


    companion object {
        private const val REQUEST_ACCESS_LOCATION_PERMISSIONS = 100
        private const val REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS = 100
    }

}
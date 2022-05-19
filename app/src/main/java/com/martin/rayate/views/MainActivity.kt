package com.martin.rayate.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryBounds
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.ui.IconGenerator
import com.martin.rayate.R
import com.martin.rayate.extensions.Extensions.toast
import com.martin.rayate.utils.FireBaseUtils
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_home.btnSignOut
import kotlinx.android.synthetic.main.activity_main.*


import java.util.ArrayList




class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    private lateinit var coord: LatLng


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupLocClient()

        btnActivar.setOnClickListener {
            startActivity(Intent(this, ActivarActivity::class.java))
        }


        btnSignOut.setOnClickListener {
            FireBaseUtils.firebaseAuth.signOut()
            startActivity(Intent(this, SignInActivity::class.java))
            toast("sesion cerrada")
            finish()
        }



    }

    private lateinit var fusedLocClient: FusedLocationProviderClient
    // use it to request location updates and get the latest location

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap //initialise map
        getCurrentLocation()

    }

    private fun setupLocClient() {
        fusedLocClient =
            LocationServices.getFusedLocationProviderClient(this)
    }

    // prompt the user to grant/deny access
    private fun requestLocPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), //permission in the manifest
            ActivarActivity.REQUEST_LOCATION)
    }
    companion object {
        private const val REQUEST_LOCATION = 1 //request code to identify specific permission request
        private const val TAG = "MainActivity" // for debugging
    }

    @SuppressLint("PotentialBehaviorOverride")
    private fun getCurrentLocation() {
        // Check if the ACCESS_FINE_LOCATION permission was granted before requesting a location
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {

            // call requestLocPermissions() if permission isn't granted
            requestLocPermissions()
        } else {

            fusedLocClient.lastLocation.addOnCompleteListener {
                // lastLocation is a task running in the background
                val location = it.result //obtain location
                //reference to the database
                // val database: FirebaseDatabase = FirebaseDatabase.getInstance()
                // val ref: DatabaseReference = database.getReference("test")
                if (location != null) {

                    val latLng = LatLng(location.latitude, location.longitude)
                    // create a marker at the exact location
                    var Marker = map.addMarker(
                        MarkerOptions().position(latLng)
                        .title("AQUI ESTAS").draggable(false))
                    if (Marker != null) {
                        Marker.showInfoWindow()
                    }
                    // create an object that will specify how the camera will be updated
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)

                    map.moveCamera(update)
                    //Save the location data to the database
                    // ref.setValue(location)
                    coord = latLng

                    val db = Firebase.firestore

                    // [START fs_geo_query_hashes]
                    // Find cities within 50km of London
                    val center = GeoLocation(coord.latitude, coord.longitude)
                    val radiusInM = (100 * 1000).toDouble()
                    var iconGen = IconGenerator(this)
                    iconGen.setStyle(IconGenerator.STYLE_RED)
                    iconGen.setTextAppearance(R.style.iconGenText)


                    // Each item in 'bounds' represents a startAt/endAt pair. We have to issue
                    // a separate query for each pair. There can be up to 9 pairs of bounds
                    // depending on overlap, but in most cases there are 4.
                    val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM)
                    val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
                    for (b in bounds) {
                        val q = db.collection("locales")
                            .orderBy("geohash")
                            .startAt(b.startHash)
                            .endAt(b.endHash)
                        tasks.add(q.get())
                    }

                    // Collect all the query results together into a single list
                    Tasks.whenAllComplete(tasks)
                        .addOnCompleteListener {

                            for (task in tasks) {
                                val snap = task.result
                                for (doc in snap.documents) {
                                    val lat = doc.getDouble("lat")!!
                                    val lng = doc.getDouble("lng")!!
                                    val coordLocal = LatLng(lat, lng)
                                    val nombreLocal = doc.get("localNombre").toString()
                                    println("Local es:" + nombreLocal)

                                    // We have to filter out a few false positives due to GeoHash
                                    // accuracy, but most will match
                                    val docLocation = GeoLocation(lat, lng)
                                    val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center)
                                    if (distanceInM <= radiusInM) {
                                       var markador = map.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(iconGen.makeIcon(nombreLocal))).position(LatLng(lat, lng)).title(nombreLocal))
                                        map.setOnMarkerClickListener { markador ->

                                            val intent = Intent(this, PerfilActivity::class.java)

                                            intent.putExtra("local", markador.title)
                                            startActivity(intent)


                                         true
                                        }
                                    }
                                }
                            }

                            // matchingDocs contains the results
                            // ...
                        }



                } else {
                    // if location is null , log an error message
                    Log.e(MainActivity.TAG, "No location found")
                }



            }
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //check if the request code matches the REQUEST_LOCATION
        if (requestCode == ActivarActivity.REQUEST_LOCATION)
        {
            //check if grantResults contains PERMISSION_GRANTED.If it does, call getCurrentLocation()
            if (grantResults.size == 1 && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                //if it doesn`t log an error message
                Log.e(MainActivity.TAG, "Location permission has been denied")
            }
        }
    }



}
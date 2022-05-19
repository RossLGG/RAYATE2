package com.martin.rayate.views

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.martin.rayate.R
import kotlinx.android.synthetic.main.activity_activar.*
import kotlinx.android.synthetic.main.activity_create_account.*
import com.martin.rayate.extensions.Extensions.toast
import kotlinx.android.synthetic.main.activity_sign_in.*


class ActivarActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    private lateinit var coord: LatLng

    lateinit var nombreLocal: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activar)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupLocClient()


        btnConfirmarActivacion.setOnClickListener {

            pasoPago()
            // asdasd

            // signIn()
        }


    }

    private lateinit var fusedLocClient: FusedLocationProviderClient
    // use it to request location updates and get the latest location

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap //initialise map
        getCurrentLocation()
        setMapLongClick(map)
    }

    private fun notEmpty(): Boolean = nombreLocal.isNotEmpty()

    private fun setupLocClient() {
        fusedLocClient =
            LocationServices.getFusedLocationProviderClient(this)
    }

    // prompt the user to grant/deny access
    private fun requestLocPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), //permission in the manifest
            REQUEST_LOCATION)
    }

    companion object {
        const val REQUEST_LOCATION = 1 //request code to identify specific permission request
        const val TAG = "ActivarActivity" // for debugging
    }


    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            map.clear()
            map.addMarker(
                    MarkerOptions()
                            .position(latLng)


            )

         coord = latLng
        }
    }


    private fun pasoPago(){

        nombreLocal = textLocal.text.toString()

        if(notEmpty()) {

            val db = Firebase.firestore
            val query = db.collection("locales").whereEqualTo("localNombre", nombreLocal).get().addOnSuccessListener { task ->

                if (task.isEmpty){

                    var lat: Double = coord.latitude
                    var lng: Double = coord.longitude
                    val intent = Intent(this, Activar2Activity::class.java)
                    intent.putExtra("nombreLocal", nombreLocal)
                    intent.putExtra("lat", lat.toString())
                    intent.putExtra("lng", lng.toString())


                    startActivity(intent)


                    /**
                    val local = hashMapOf(
                        "localNombre" to nombreLocal,
                        "tipo" to true,

                    )
                    val user = Firebase.auth.currentUser
                    user?.let {

                        val uid = user.uid
                    }


                    if (user != null) {
                        db.collection("locales").document(user.uid).set(local).addOnCompleteListener { documentReference ->
                            Log.d("consola", "Local Agregado con ID: ${user.uid}")
                            if (documentReference.isComplete){

                                val Geofirehash = GeoFireUtils.getGeoHashForLocation(GeoLocation(coord.latitude, coord.longitude))

                                val GeoAdendo = hashMapOf(
                                    "geohash" to Geofirehash,
                                    "lat" to coord.latitude,
                                    "lng" to coord.longitude
                                )
                                db.collection("locales").document(user.uid).set(GeoAdendo, SetOptions.merge()).addOnCompleteListener {task ->
                                    if (task.isComplete){

                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()

                                    }


                                }


                                    .addOnFailureListener { e ->
                                        Log.w("consola", "Error Agregando Documento GeoHash", e)
                                    }




                            }



                        }
                            .addOnFailureListener { e ->
                                Log.w("consola", "Error Agregando Documento", e)
                            }
                    } else {
                        toast("SESION INVALIDA, INTENTE REINGRESAR A SU CUENTA")
                    }


                        **/


                } else {

                    toast("ESTE NOMBRE DE ESTUDIO YA ESTA TOMADO")

                }


            }






        } else {
            toast("POR FAVOR INGRESE EL NOMBRE DE SU ESTUDIO")
        }






    }


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
                   var Marker = map.addMarker(MarkerOptions().position(latLng)
                        .title("PRESIONA EL MAPA PARA CAMBIAR UBICACION").draggable(true))
                    if (Marker != null) {
                        Marker.showInfoWindow()
                    }
                    // create an object that will specify how the camera will be updated
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)

                    map.moveCamera(update)
                    //Save the location data to the database
                   // ref.setValue(location)
                    coord = latLng
                } else {
                    // if location is null , log an error message
                    Log.e(TAG, "No location found")
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
        if (requestCode == REQUEST_LOCATION)
        {
            //check if grantResults contains PERMISSION_GRANTED.If it does, call getCurrentLocation()
            if (grantResults.size == 1 && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                //if it doesn`t log an error message
                Log.e(TAG, "Location permission has been denied")
            }
        }
    }

}
package com.martin.rayate.views

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.wallet.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.internal.bind.util.ISO8601Utils
import com.martin.rayate.R
import com.martin.rayate.extensions.Extensions.toast
import com.martin.rayate.utils.UtilPago
import com.martin.rayate.utils.microsToString
import kotlinx.android.synthetic.main.activity_activar.*
import kotlinx.android.synthetic.main.activity_activar2.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToLong

class Activar2Activity : AppCompatActivity() {

    private lateinit var paymentsClient: PaymentsClient

    private var price: Int = 5000

    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991

    private lateinit var coord: LatLng

    lateinit var presentLocal: String





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activar2)

        paymentsClient = UtilPago.createPaymentsClient(this)

        presentLocal = textPresent.text.toString()





                val readyToPayRequest =
                    IsReadyToPayRequest.fromJson(UtilPago.isReadyToPayRequest().toString())

                val readyToPayTask = paymentsClient.isReadyToPay(readyToPayRequest)
                readyToPayTask.addOnCompleteListener { task ->
                    try {
                        task.getResult(ApiException::class.java)?.let(::setGooglePayAvailable)
                    } catch (exception: ApiException) {

                        toast("POR FAVOR INSTALE GOOGLE PAY EN SU DISPOSITIVO PARA CONTINUAR")

                    }
                }

        googlePayButton.setOnClickListener { requestPayment() }

    }

    private fun notEmpty(): Boolean = presentLocal.isNotEmpty()






    private fun setGooglePayAvailable(available: Boolean) {
        if (notEmpty()) {
            aceptoPago.visibility = View.VISIBLE
            aceptoPago.setOnCheckedChangeListener { compoundButton, b ->
                if (available && aceptoPago.isChecked) {
                    googlePayButton.visibility = View.VISIBLE
                    googlePayButton.setOnClickListener { requestPayment() }
                } else {
                    googlePayButton.visibility = View.GONE
                }
            }
        } else {
            toast("POR FAVOR RELLENE LA PRESENTACION DE SU PERFIL")
        }
    }

    private fun requestPayment() {
        // Disables the button to prevent multiple clicks.
        googlePayButton.isClickable = false

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.

        val price = (5000).toLong().microsToString()

        val paymentDataRequestJson = UtilPago.getPaymentDataRequest(price)
        if (paymentDataRequestJson == null) {
            Log.e("RequestPayment", "Can't fetch payment data request")
            return
        }
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        if (request != null) {
            AutoResolveHelper.resolveTask(
                paymentsClient.loadPaymentData(request), this, LOAD_PAYMENT_DATA_REQUEST_CODE)
        }
    }


    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // value passed in AutoResolveHelper
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK ->
                        data?.let { intent ->
                            PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
                        }
                    Activity.RESULT_CANCELED -> {
                        // Nothing to do here normally - the user simply cancelled without selecting a
                        // payment method.
                    }

                    AutoResolveHelper.RESULT_ERROR -> {
                        AutoResolveHelper.getStatusFromIntent(data)?.let {
                            handleError(it.statusCode)
                        }
                    }
                }
                // Re-enables the Google Pay payment button.
                googlePayButton.isClickable = true
            }
        }
    }



    private fun handlePaymentSuccess(paymentData: PaymentData) {
        val paymentInformation = paymentData.toJson() ?: return

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData = JSONObject(paymentInformation).getJSONObject("paymentMethodData")

            // If the gateway is set to "example", no payment information is returned - instead, the
            // token will only consist of "examplePaymentMethodToken".
            if (paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("type") == "PAYMENT_GATEWAY" && paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token") == "examplePaymentMethodToken") {

               /** AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage("Gateway name set to \"example\" - please modify " +
                            "Constants.java and replace it with your own gateway.")
                    .setPositiveButton("OK", null)
                    .create()
                    .show() **/
            }

            val billingName = paymentMethodData.getJSONObject("info")
                .getJSONObject("billingAddress").getString("name")
            Log.d("BillingName", billingName)

            Toast.makeText(this, getString(R.string.payments_show_name, billingName), Toast.LENGTH_LONG).show()

            var z = ZoneId.of("America/Argentina/Buenos_Aires")
            var tiempo = LocalDate.now(z)
            var hoy = tiempo.toString()
            var final = tiempo.plusMonths(1).toString()




            val db = Firebase.firestore

            var nombreLocal: String? = intent.getStringExtra("nombreLocal")

            val local = hashMapOf(
                "localNombre" to nombreLocal,
                "localPresent" to presentLocal,
            )


            var lat: Double = intent.getStringExtra("lat")?.toDouble()!!
            var lng: Double = intent.getStringExtra("lng")!!.toDouble()

            val userUpdate = hashMapOf(
                "fechaSubscripcion" to hoy,
                "fechafin" to final,
                "tipo" to true


            )
            val user = Firebase.auth.currentUser
            user?.let {

                val uid = user.uid
            }


            if (user != null) {
                db.collection("locales").document(user.uid).set(local).addOnCompleteListener { documentReference ->
                    Log.d("consola", "Local Agregado con ID: ${user.uid}")
                    if (documentReference.isComplete) {


                        db.collection("usuarios").document(user.uid)
                            .set(userUpdate, SetOptions.merge()).addOnCompleteListener { task ->

                                if (task.isComplete) {


                                    val Geofirehash = GeoFireUtils.getGeoHashForLocation(
                                        GeoLocation(
                                            lat,
                                            lng
                                        )
                                    )

                                    val GeoAdendo = hashMapOf(
                                        "geohash" to Geofirehash,
                                        "lat" to lat,
                                        "lng" to lng
                                    )
                                    db.collection("locales").document(user.uid)
                                        .set(GeoAdendo, SetOptions.merge())
                                        .addOnCompleteListener { task ->
                                            if (task.isComplete) {

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
                                Log.w("consola", "Error Actualizando Usuario", e)


                            }

                    }

                }
                    .addOnFailureListener { e ->
                        Log.w("consola", "Error Agregando Documento", e)
                    }
            } else {
                toast("SESION INVALIDA, INTENTE REINGRESAR A SU CUENTA")
            }









            // Logging token string.
            Log.d("GooglePaymentToken", paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("token"))

        } catch (e: JSONException) {
            Log.e("handlePaymentSuccess", "Error: " + e.toString())
        }

    }

    private fun handleError(statusCode: Int) {
        Log.w("loadPaymentData failed", String.format("Error code: %d", statusCode))
    }


}
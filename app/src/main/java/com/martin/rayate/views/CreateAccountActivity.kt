package com.martin.rayate.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.martin.rayate.R
import kotlinx.android.synthetic.main.activity_create_account.*
import android.widget.EditText
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.martin.rayate.extensions.Extensions.toast
import com.martin.rayate.utils.FireBaseUtils.firebaseAuth
import com.martin.rayate.utils.FireBaseUtils.firebaseUser


class CreateAccountActivity : AppCompatActivity() {
    lateinit var userName: String
    lateinit var userEmail: String
    lateinit var userPassword: String
    lateinit var createAccountInputsArray: Array<EditText>
        /* VARIABLES QUE SE INICIAN COMO NULL */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
        createAccountInputsArray = arrayOf(etNombre, etEmail, etPassword, etConfirmPassword)
        btnCreateAccount.setOnClickListener {
            signIn()
        }

        btnSignIn2.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            toast("por favor ingrese a su cuenta")
            finish()
        }
    }

    /* verifica si hay sesion activa*/

    override fun onStart() {
        super.onStart()
        val user: FirebaseUser? = firebaseAuth.currentUser
        user?.let {
            startActivity(Intent(this, MainActivity::class.java))
            toast("bienvenide")
        }
    }

    private fun notEmpty(): Boolean = etNombre.text.toString().trim().isNotEmpty() && etEmail.text.toString().trim().isNotEmpty() &&
            etPassword.text.toString().trim().isNotEmpty() &&
            etConfirmPassword.text.toString().trim().isNotEmpty()

    private fun identicalPassword(): Boolean {
        var identical = false
        if (notEmpty() &&
            etPassword.text.toString().trim() == etConfirmPassword.text.toString().trim()
        ) {
            identical = true
        } else if (!notEmpty()) {
            createAccountInputsArray.forEach { input ->
                if (input.text.toString().trim().isEmpty()) {
                    input.error = "${input.hint} es requerido"
                }
            }
        } else {
            toast("las contraseñas no coinciden !")
        }
        return identical
    }

    private fun signIn() {
        if (identicalPassword()) {
            // identicalPassword() returns true only  when inputs are not empty and passwords are identical
            userEmail = etEmail.text.toString().trim()
            userPassword = etPassword.text.toString().trim()

            /*create a user*/
            firebaseAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        toast("cuenta creada exitosamente !")
                        sendEmailVerification()
                        val creacion = firebaseAuth.currentUser?.metadata?.creationTimestamp
                        val hoy = firebaseAuth.currentUser?.metadata?.lastSignInTimestamp
                        println(creacion)
                        println(hoy)

                        if (creacion  == hoy) {
                            val nombre = etNombre.text.toString()
                            val user = Firebase.auth.currentUser
                            user?.let {


                                val photoUrl = user.photoUrl
                                val uid = user.uid
                            }
                            val usuario = hashMapOf(
                                "id" to (user?.uid),
                                "nombre" to nombre,
                                "tipo" to false,
                                "ubicacion" to null,
                                "urlperfil" to null,

                            )
                            val db = Firebase.firestore
                            if (user != null) {
                                db.collection("usuarios").document(user.uid).set(usuario).addOnSuccessListener { documentReference ->
                                    Log.d("consola", "Usuario Agregado con ID: ${user.uid}")
                                }
                                        .addOnFailureListener { e ->
                                            Log.w("consola", "Error Agregando Documento", e)
                                        }
                            }
                        } else {
                            toast("no se registro datos")
                        }
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("nombre", etNombre.text.toString())
                        startActivity(intent)
                        finish()
                    } else {
                        toast("fallo de autentificacion !")
                    }
                }
        }
    }

    /*  envia email de verificacion para usuario nuevo, solo si usuario firebase no es null
    *
    */

    private fun sendEmailVerification() {
        firebaseUser?.let {
            it.sendEmailVerification().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    toast("email enviado a $userEmail")
                }
            }
        }
    }
}



/** class CreateAccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
    }
} */
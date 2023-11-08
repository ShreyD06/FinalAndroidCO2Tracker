package com.shreyd.co2tracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = Firebase.auth

        val button = findViewById<Button>(R.id.loginSubmitButton)
        button.setOnClickListener {
            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()
            if (email != "" && password != "") {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {task ->
                    if(task.isSuccessful) {
                        val mainIntent = Intent(this, TempMain::class.java)
                        startActivity(mainIntent)
                    }
                    else {
                        Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_LONG).show()
                    }
                }
            }
            else {
                Toast.makeText(this, "You must enter email and password", Toast.LENGTH_LONG).show()
            }
        }
    }
}
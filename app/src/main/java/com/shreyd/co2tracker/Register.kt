package com.shreyd.co2tracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.util.*

class Register : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var carType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth = Firebase.auth

        val cars = resources.getStringArray(R.array.programming_languages)
        val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_item, cars)
        val autocompleteTV = findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)
        autocompleteTV.setAdapter(arrayAdapter)
        autocompleteTV.setDropDownBackgroundDrawable(ResourcesCompat.getDrawable(resources, R.drawable.dropdown_bg, null))

        autocompleteTV.onItemClickListener = AdapterView.OnItemClickListener{ adapterView, view, i, l ->
            println("$i, $l")
            carType = cars[i]
            println(carType)
        }

        val button = findViewById<Button>(R.id.registerSubmitButton)
        button.setOnClickListener {
            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()
            val id = email.replace(".", "").replace("#", "")
                .replace("$", "").replace("[", "").replace("]", "")
            println("----------------$id-------------")
            val user = com.shreyd.co2tracker.User(
                id = id,
                email = email.lowercase(Locale.ROOT),
                name = findViewById<EditText>(R.id.nameEditText).text.toString(),
                password = password,
                cartype = carType
            )
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) {task ->
                if (task.isSuccessful) {
                    addUser(user)
                    val mainIntent = Intent(this, TempMain::class.java)
                    startActivity(mainIntent)
                    finish()
                }
                else {
                    Toast.makeText(this, "Check Email or Password", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun addUser(user: User) {
        val dbUsers = FirebaseDatabase.getInstance().getReference("Users")
        dbUsers.child(user.id!!).setValue(user)

    }

}
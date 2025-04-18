package com.example.virtuallab

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        val editId: EditText = findViewById(R.id.editId)
        val editEmail: EditText = findViewById(R.id.editEmail)
        val loginButton: Button = findViewById(R.id.loginButton)
        loginButton.setOnClickListener {
            val id = editId.text.toString()
            val email = editEmail.text.toString()
            if (id == "12345678" && email == "baongocngo2211@gmail.com") {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else if (id != "12345678") {
                Toast.makeText(this, "Invalid ID", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Invalid Email", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
package com.example.virtuallab

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage)
        val startButton: Button = findViewById(R.id.startButton)
        startButton.setOnClickListener {
            val intent = Intent(this, SelectActivity::class.java)
            startActivity(intent)
        }

    }




}


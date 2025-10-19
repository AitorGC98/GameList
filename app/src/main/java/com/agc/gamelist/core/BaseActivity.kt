package com.agc.gamelist.core

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.agc.gamelist.R
import com.google.firebase.FirebaseApp

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ocultar el ActionBar
        supportActionBar?.hide()
    }
}

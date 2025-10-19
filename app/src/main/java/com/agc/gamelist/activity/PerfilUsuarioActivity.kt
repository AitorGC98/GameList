package com.agc.gamelist.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.agc.gamelist.R
import com.agc.gamelist.model.Usuario
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore

class PerfilUsuarioActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var usuario: Usuario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil_usuario)

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatus)

        // Cambiar a ConstraintLayout en lugar de LinearLayout
        val container: ConstraintLayout = findViewById(R.id.main)

        // Manejo de márgenes para ventanas
        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance()

        // Configurar la flecha de retroceso en el Action Bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Recuperar el correo del usuario desde el Intent
        val usuarioEmail = intent.getStringExtra("usuario_email")

        // Cambiar el fondo según el modo oscuro/claro
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            container.setBackgroundResource(R.color.black2) // Fondo oscuro
        } else {
            container.setBackgroundResource(R.drawable.background_cuadrado) // Fondo claro
        }

        if (usuarioEmail != null) {
            obtenerDatosUsuario(usuarioEmail)
        } else {
            Log.e("PerfilUsuarioActivity", "Correo de usuario no recibido")
        }
    }

    // Configurar la flecha de retroceso en el Action Bar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // Retroceder al presionar la flecha
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Recuperar los datos del usuario de Firestore
    private fun obtenerDatosUsuario(correo: String) {
        db.collection("Usuarios")
            .document(correo)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val usuarioData = document.toObject(Usuario::class.java)
                    if (usuarioData != null) {
                        usuario = usuarioData
                        actualizarUI()
                    } else {
                        Log.e("PerfilUsuarioActivity", "No se pudo convertir los datos del usuario.")
                    }
                } else {
                    Log.e("PerfilUsuarioActivity", "El documento del usuario no existe.")
                }
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
            }
    }
// Actualizar la interfaz de usuario con los datos del usuario
    private fun actualizarUI() {
        if (::usuario.isInitialized) {
            findViewById<TextView>(R.id.texto_nombre_usuario).text = usuario.nombre
            findViewById<TextView>(R.id.texto_bio_usuario).text = usuario.sobreTi

            val progressBar: ProgressBar = findViewById(R.id.experience_progress_bar)
            val progress = if (usuario.experienciaNecesaria > 0) {
                (usuario.experienciaActual / usuario.experienciaNecesaria.toFloat() * 100).toInt()
            } else {
                0
            }
            progressBar.progress = progress

            findViewById<TextView>(R.id.level_text).text = "Nivel: ${usuario.nivel}"

            val fotoPerfil: ShapeableImageView = findViewById(R.id.BotonPerfil)
            if (usuario.fotoPerfil.isNotEmpty()) {
                val resID = resources.getIdentifier(usuario.fotoPerfil, "drawable", packageName)
                if (resID != 0) {
                    fotoPerfil.setImageResource(resID)
                } else {
                    fotoPerfil.setImageResource(R.drawable.baseline_person_24)
                }
            } else {
                fotoPerfil.setImageResource(R.drawable.baseline_person_24)
            }
        } else {
            Log.w("PerfilUsuarioActivity", "El usuario no ha sido inicializado.")
        }
    }
}



package com.agc.gamelist.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.agc.gamelist.R
import com.agc.gamelist.databinding.ActivityGameViewBinding
import com.agc.gamelist.databinding.ActivityPerfilBinding
import com.agc.gamelist.fragments.ImageSelectionDialogFragment
import com.agc.gamelist.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private lateinit var firestore: FirebaseFirestore
    private var selectedImage: String = "" // Almacena el nombre de la imagen seleccionada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatus) // Color oscuro
        // Habilitar la flecha de "volver atrás"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firestore = FirebaseFirestore.getInstance()

        // Detectar el modo actual: claro u oscuro
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK

        // Cambiar el fondo según el modo
        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // Modo oscuro: fondo negro
            binding.main.setBackgroundResource(R.color.black2)  // O cualquier color que desees para el fondo oscuro
        } else {
            // Modo claro: fondo blanco
            binding.main.setBackgroundResource(R.drawable.background_cuadrado)  // O cualquier color que desees para el fondo claro
        }
        // Cargar datos de las preferencias al iniciar
        loadUserProfileFromPreferences()


        if (Build.VERSION.SDK_INT > 26) { // Usa el número en lugar de Build.VERSION_CODES.O

            binding.BotonPerfil.setOnClickListener {


                    showImageSelectionDialog()


            }

        } else {
            Toast.makeText(this, "Tu móvil es demasiado antiguo. Te recomiendo comprarte uno.", Toast.LENGTH_SHORT).show()
        }


        // Cargar las animaciones
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)
        val pulsar = AnimationUtils.loadAnimation(this, R.anim.use_animation)

        // Iniciar la animación de pulso, pero asegúrate de detener cualquier animación previa
        binding.botonSave.clearAnimation()  // Detenemos cualquier animación anterior
        binding.botonSave.startAnimation(pulseAnimation)  // Iniciamos la animación de pulso
        binding.botonSave.setOnClickListener {

            val aboutMeText = binding.edSobreTi.text.toString().trim()
            val name = binding.edNombre.text.toString().trim()

            if (aboutMeText.isNotEmpty() && name.isNotEmpty()) {
                saveUserName()
                saveUserAboutMe() // Guarda "Sobre ti"
                finish()
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }

        }

        // Configura el contador de caracteres para el campo 'Sobre ti'
        val edSobreTi = findViewById<EditText>(R.id.edSobreTi)
        val sobreTiCounter = findViewById<TextView>(R.id.sobreTiCounter)

        // Inicialmente ocultamos el contador
        sobreTiCounter.visibility = View.GONE

        edSobreTi.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val currentLength = s?.length ?: 0
                sobreTiCounter.text = "$currentLength/300"

                // Mostrar el contador solo si hay texto en el EditText
                if (currentLength > 0) {
                    sobreTiCounter.visibility = View.VISIBLE
                } else {
                    sobreTiCounter.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Configura el contador de caracteres para el campo 'Nombre'
        val edNombre = findViewById<EditText>(R.id.edNombre)
        val nombreCounter = findViewById<TextView>(R.id.nombreCounter)

        // Inicialmente ocultamos el contador
        nombreCounter.visibility = View.GONE

        edNombre.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val currentLength = s?.length ?: 0
                nombreCounter.text = "$currentLength/25"

                // Mostrar el contador solo si hay texto en el EditText
                if (currentLength > 0) {
                    nombreCounter.visibility = View.VISIBLE
                } else {
                    nombreCounter.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }



    private fun showImageSelectionDialog() {
        val dialog = ImageSelectionDialogFragment()
        dialog.setOnImageSelectedListener { imageName ->
            selectedImage = imageName
            saveUserProfileImage(selectedImage)
        }
        dialog.show(supportFragmentManager, "ImageSelectionDialogFragment")
    }

    private fun saveUserName() {
        val userName = binding.edNombre.text.toString().trim() // Obtiene el nombre del EditText

        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)

        if (userName.isNotEmpty()) {
            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
            val userId = prefs.getString("email", "") ?: ""

            // Verificar si el documento ya existe en Firestore
            val userDocRef = firestore.collection("Usuarios").document(userId)

            userDocRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Si el documento ya existe, solo actualiza el nombre
                    userDocRef.update("nombre", userName)
                        .addOnSuccessListener {


                            // Guardar el nombre en las preferencias
                            saveUserNameToPreferences(userName)
                        }
                        .addOnFailureListener { e ->
                            FirebaseCrashlytics.getInstance().recordException(e)

                            Toast.makeText(this, "No se pudo guardar:", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Si el documento no existe, crea un nuevo documento con el nombre
                    val user = Usuario(nombre = userName, correo = prefs.getString("email", "") ?: "")
                    userDocRef.set(user)
                        .addOnSuccessListener {


                            // Guardar el nombre en las preferencias
                            saveUserNameToPreferences(userName)
                        }
                        .addOnFailureListener { e ->
                            FirebaseCrashlytics.getInstance().recordException(e)
                            Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show()
                        }
                }
            }.addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private fun saveUserProfileImage(imageName: String) {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", "") ?: ""

        // Referencia al documento del usuario
        val userDocRef = firestore.collection("Usuarios").document(email)

        // Verificar si el documento ya existe
        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Si el documento existe, actualiza el campo 'fotoPerfil'
                userDocRef.update("fotoPerfil", imageName)
                    .addOnSuccessListener {

                        // Asigna la imagen al ImageView
                        setProfileImage(imageName)

                        // Guardar la imagen en las preferencias
                        saveUserProfileImageToPreferences(imageName)
                    }
                    .addOnFailureListener { e ->
                        FirebaseCrashlytics.getInstance().recordException(e)
                        Toast.makeText(this, "No se pudo guardar la imagen", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Si el documento no existe, crea uno nuevo con el correo y la foto de perfil
                val user = mapOf(
                    "correo" to email,
                    "fotoPerfil" to imageName
                )
                userDocRef.set(user)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Imagen de perfil guardada y usuario creado", Toast.LENGTH_SHORT).show()

                        // Asigna la imagen al ImageView
                        setProfileImage(imageName)

                        // Guardar la imagen en las preferencias
                        saveUserProfileImageToPreferences(imageName)
                    }
                    .addOnFailureListener { e ->
                        FirebaseCrashlytics.getInstance().recordException(e)
                        Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Al hacer clic en la flecha, finaliza la actividad (equivalente a presionar el botón de retroceso)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    // Método para asignar la imagen al ImageView
    private fun setProfileImage(imageName: String) {
        // Obtiene el ID del recurso drawable a partir del nombre de la imagen
        val resourceId = resources.getIdentifier(imageName, "drawable", packageName)

        // Verifica si el recurso existe
        if (resourceId != 0) {
            // Asigna la imagen al ImageView
            binding.BotonPerfil.setImageResource(resourceId)
        } else {
            Toast.makeText(this, "No se encontró la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    // Guardar el nombre del usuario en las preferencias
    private fun saveUserNameToPreferences(userName: String) {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("userName", userName)
        prefs.apply()
    }

    // Guardar el nombre de la imagen en las preferencias
    private fun saveUserProfileImageToPreferences(imageName: String) {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("profileImage", imageName)
        prefs.apply()
    }

    // Cargar el perfil del usuario desde las preferencias
    private fun loadUserProfileFromPreferences() {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val savedName = prefs.getString("userName", "")
        val savedImage = prefs.getString("profileImage", "")
        val savedAboutMe = prefs.getString("aboutMe", "")

        // Asignar el nombre al TextView si existe
        if (!savedName.isNullOrEmpty()) {
            binding.edNombre.setText(savedName)
        }

        // Asignar la imagen al ImageView si existe
        if (!savedImage.isNullOrEmpty()) {
            setProfileImage(savedImage)
        }
        // Asignar el "Sobre ti" al EditText si existe
        if (!savedAboutMe.isNullOrEmpty()) {
            binding.edSobreTi.setText(savedAboutMe)
        }
    }

    // Guardar "Sobre ti" en Firestore
    private fun saveUserAboutMe() {
        val aboutMeText = binding.edSobreTi.text.toString().trim()
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val userId = prefs.getString("email", "") ?: ""

        if (aboutMeText.isNotEmpty()) {
            val userDocRef = firestore.collection("Usuarios").document(userId)

            userDocRef.update("sobreTi", aboutMeText)
                .addOnSuccessListener {
                    Toast.makeText(this, "Información sobre ti guardada", Toast.LENGTH_SHORT).show()
                    saveAboutMeToPreferences(aboutMeText) // Guardar en preferencias
                }
                .addOnFailureListener { e ->
                    FirebaseCrashlytics.getInstance().recordException(e)
                    Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show()
                }
        }
    }
// Guardar "Sobre ti" en las preferencias
    private fun saveAboutMeToPreferences(aboutMeText: String) {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("aboutMe", aboutMeText)
        prefs.apply()
    }


}


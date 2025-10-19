package com.agc.gamelist.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.agc.gamelist.R


import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.agc.gamelist.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.SetOptions

class CrearForoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_foro)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        val tituloEditText = findViewById<EditText>(R.id.titulo_forum)
        val descripcionEditText = findViewById<EditText>(R.id.descripcion_forum)
        val crearForoButton = findViewById<ImageButton>(R.id.crear_foro_button)
        val tituloCounterTextView = findViewById<TextView>(R.id.titulo_counter)
        val descripcionCounterTextView = findViewById<TextView>(R.id.descripcion_counter)

        // Referencia al LinearLayout
        val container: LinearLayout = findViewById(R.id.mainLayout)

        // Detectar el modo actual: claro u oscuro
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatus) // Color oscuro
        // Cambiar el fondo según el modo
        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // Modo oscuro: fondo negro
            container.setBackgroundResource(R.color.black2)  // Fondo oscuro (debes definir este color en colors.xml)
        } else {
            // Modo claro: fondo blanco
            container.setBackgroundResource(R.drawable.background_cuadrado)  // Fondo claro (debes definir este drawable en res/drawable)
        }

        val userEmail = FirebaseAuth.getInstance().currentUser?.email

        // Configura el contador de caracteres para el título
        tituloEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val currentLength = s?.length ?: 0
                tituloCounterTextView.text = "$currentLength/60"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Configura el contador de caracteres para la descripción
        descripcionEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val currentLength = s?.length ?: 0
                descripcionCounterTextView.text = "$currentLength/250"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        crearForoButton.setOnClickListener {
            val titulo = tituloEditText.text.toString().trim()
            val descripcion = descripcionEditText.text.toString().trim()

            if (titulo.isNotEmpty() && descripcion.isNotEmpty() && userEmail != null) {
                // Crear foro en Firebase
                val foroData = hashMapOf(
                    "titulo" to titulo,
                    "descripcion" to descripcion,
                    "creador_foro" to userEmail,
                    "fecha_creacion" to System.currentTimeMillis()
                )

                // Añadir el foro a la colección "Foros"
                val forosRef = FirebaseFirestore.getInstance().collection("Foros")
                forosRef.add(foroData)
                    .addOnSuccessListener { documentReference ->
                        val foroId = documentReference.id

                        // Asignar al creador como Administrador en la subcolección "Roles"
                        val rolesRef = forosRef.document(foroId).collection("Roles")
                        rolesRef.document(userEmail).set(mapOf("rol" to "Administrador"))
                            .addOnSuccessListener {
                                // Actualizar el documento del usuario para agregar el ID del foro
                                val usuariosRef = FirebaseFirestore.getInstance().collection("Usuarios")
                                usuariosRef.document(userEmail).get()
                                    .addOnSuccessListener { userDocument ->
                                        if (userDocument.exists()) {
                                            // Obtener la lista actual de foros del usuario
                                            val forosList = userDocument.get("foros") as? List<String> ?: emptyList()

                                            // Crear una nueva lista con el nuevo ID del foro
                                            val updatedForosList = forosList.toMutableList().apply {
                                                add(foroId)
                                            }

                                            // Actualizar el documento del usuario
                                            usuariosRef.document(userEmail).update("foros", updatedForosList)
                                                .addOnSuccessListener {
                                                    ganarExperiencia(60)

                                                    finish() // Cierra la actividad y vuelve al fragmento
                                                }
                                                .addOnFailureListener { e ->
                                                    FirebaseCrashlytics.getInstance().recordException(e)
                                                    Toast.makeText(this, "No se ha podido registrar el foro", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            // Si el usuario no existe, manejar el error
                                            Toast.makeText(this, "El usuario no existe", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        FirebaseCrashlytics.getInstance().recordException(e)
                                    }
                            }
                            .addOnFailureListener { e ->
                                FirebaseCrashlytics.getInstance().recordException(e)
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "No se pudo crear el foro crear el foro: ", Toast.LENGTH_SHORT).show()
                        FirebaseCrashlytics.getInstance().recordException(e)
                    }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función encargada de obtener el email del usuario actual
    private fun getCurrentUserId(): String {

        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)
        // Recupera el email y el proveedor de la última sesión guardada.
        val email = prefs.getString("email", null)
        return email.toString()
        Log.d("ComentariosForoActivity",  email.toString())
    }

    // Función encargada de calcular la ganacia de experiencia
    private fun ganarExperiencia(cantidad: Int) {
        val usuarioId =getCurrentUserId()// Reemplaza con el ID real del usuario
        val db = FirebaseFirestore.getInstance()
        // Obtener la experiencia actual del usuario de Firestore
        db.collection("Usuarios").document(usuarioId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Inicializar el usuario desde el documento
                    val usuario = document.toObject(Usuario::class.java) ?: Usuario()

                    Log.d("HomeActivity", "Usuario actual: $usuario")

                    // Inicializa los campos si no existen
                    var experienciaActual = document.getLong("experienciaActual")?.toInt() ?: 0
                    var nivel = document.getLong("nivel")?.toInt() ?: 1
                    var experienciaNecesaria = document.getLong("experienciaNecesaria")?.toInt() ?: calcularNuevaExperienciaNecesaria(nivel)

                    // Aumentar la experiencia actual
                    experienciaActual += cantidad

                    // Verificar si el usuario ha subido de nivel
                    while (experienciaActual >= experienciaNecesaria) {
                        nivel++
                        experienciaActual -= experienciaNecesaria
                        experienciaNecesaria = calcularNuevaExperienciaNecesaria(nivel)
                    }

                    // Actualizar la base de datos con los nuevos valores, solo si son diferentes
                    val updates = hashMapOf(
                        "experienciaActual" to experienciaActual,
                        "nivel" to nivel,
                        "experienciaNecesaria" to experienciaNecesaria
                    )

                    db.collection("Usuarios").document(usuarioId)
                        .set(updates, SetOptions.merge()) // Usa merge para no sobreescribir datos existentes
                        .addOnSuccessListener {

                        }
                        .addOnFailureListener { exception ->
                            FirebaseCrashlytics.getInstance().recordException(exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                FirebaseCrashlytics.getInstance().recordException(exception)
            }
    }


    private fun calcularNuevaExperienciaNecesaria(nivel: Int): Int {
        // Lógica para calcular la nueva experiencia necesaria para el próximo nivel
        return nivel * 100 // Por ejemplo, 100, 200, 300, etc.
    }
}


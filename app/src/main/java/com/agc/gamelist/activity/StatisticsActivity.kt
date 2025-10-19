package com.agc.gamelist.activity

import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.agc.gamelist.R
import com.agc.gamelist.databinding.ActivityHomeBinding
import com.agc.gamelist.databinding.ActivityStatisticsBinding
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class StatisticsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityStatisticsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Infla el binding y establece la vista raíz
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)  // Usa binding.root aquí en lugar de R.layout.activity_statistics
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatus) // Color oscuro
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Cambiar color de la ActionBar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)  // Habilitar el botón de retroceso
            setDisplayShowHomeEnabled(true)  // Mostrar el ícono de inicio en la acción
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@StatisticsActivity, R.color.colorMorado)))  // Cambiar el color de fondo
        }


        // Llamada para actualizar estadísticas
        updateStatistics()
    }

    // Manejar clic en la flecha de retroceso
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // Finalizar la actividad al presionar la flecha
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
// Actualizar estadísticas
    private fun updateStatistics() {
        val userId = getCurrentUserId()
        Log.d("StatisticsActivity", "Usuario actual: $userId")

        // Cuenta los documentos en la colección "Jugando"
        countDocuments("Usuarios/$userId/Jugando") { count ->
            Log.d("StatisticsActivity", "Número de juegos 'Jugando': $count")
            binding.tvJugandoGamesValue.text = count.toString()
        }

        // Cuenta los documentos en la colección "Terminado"
        countDocuments("Usuarios/$userId/Terminado") { count ->
            Log.d("StatisticsActivity", "Número de juegos 'Terminados': $count")
            binding.tvCompletedGamesValue.text = count.toString()
        }

        // Cuenta los documentos en la colección "Abandonado"
        countDocuments("Usuarios/$userId/Abandonado") { count ->
            Log.d("StatisticsActivity", "Número de juegos 'Abandonados': $count")
            binding.tvAbandonedGamesValue.text = count.toString()
        }

        // Cuenta los documentos en la colección "Pendiente"
        countDocuments("Usuarios/$userId/Pendiente") { count ->
            Log.d("StatisticsActivity", "Número de juegos 'Pendientes': $count")
            binding.tvPendienteGamesValue.text = count.toString()
        }

        val generosValidos = listOf("Action", "Shooter", "Adventure", "Platformer")

        // Contar los juegos de cada género
        for (genero in generosValidos) {
            countDocumentsForGenre(userId, genero) { count ->
                when (genero) {
                    "Action" -> binding.tvActionGamesValue.text = count.toString()
                    "Shooter" -> binding.tvShooterGamesValue.text = count.toString()
                    "Adventure" -> binding.tvAdventureGamesValue.text = count.toString()
                    "Platformer" -> binding.tvPlatformerGamesValue.text = count.toString()
                }
            }
        }


        countUserForums(userId)
    }
// Cuenta los juegos de un género específico
    private fun countDocumentsForGenre(userId: String, genre: String, callback: (Int) -> Unit) {
        db.collection("Usuarios")
            .document(userId)
            .collection(genre)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val count = task.result?.size() ?: 0
                    callback(count)
                } else {
                    Log.w("StatisticsActivity", "Error al contar juegos del género $genre", task.exception)
                    callback(0)
                }
            }
    }
// Cuenta los documentos en una colección
    private fun countDocuments(collectionPath: String, callback: (Int) -> Unit) {
        Log.d("StatisticsActivity", "Consultando colección: $collectionPath")
        db.collection(collectionPath)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val count = task.result?.size() ?: 0
                    Log.d("StatisticsActivity", "Documentos en $collectionPath: $count")
                    callback(count)
                } else {
                    Log.w("StatisticsActivity", "Error al obtener los documentos de $collectionPath", task.exception)
                    callback(0)
                }
            }
    }
// Obtener el ID del usuario actual
    private fun getCurrentUserId(): String {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)
        val email = prefs.getString("email", null)
        Log.d("StatisticsActivity", "Correo del usuario actual: $email")
        return email ?: ""
    }
// Cuenta los foros del usuario
    private fun countUserForums(userId: String) {
        // Accede al documento del usuario en la colección "Usuarios"
        db.collection("Usuarios").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Obtén el array "foros" y cuenta sus elementos
                    val forums = document.get("foros") as? List<*>
                    val forumsCount = forums?.size ?: 0  // Si es nulo, establece 0

                    Log.d("StatisticsActivity", "Número de foros: $forumsCount")

                    // Asigna el conteo al TextView en la interfaz
                    binding.tvForosGamesValue.text = forumsCount.toString()
                } else {
                    Log.d("StatisticsActivity", "No se encontró el documento del usuario")
                    binding.tvForosGamesValue.text = "0"
                }
            }
            .addOnFailureListener { exception ->
                FirebaseCrashlytics.getInstance().recordException(exception)
                binding.tvForosGamesValue.text = "0"
            }
    }


    private fun enableEdgeToEdge() {

    }
}
package com.agc.gamelist.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.agc.gamelist.R
import com.agc.gamelist.activity.GameViewActivity
import com.agc.gamelist.adapters.GameAdapter
import com.agc.gamelist.databinding.FragmentPendienteBinding
import com.agc.gamelist.home.ui.dashboard.OnGameClickListener
import com.agc.gamelist.model.Game
import com.agc.gamelist.network.RawgApi
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PendienteFragment : Fragment(), OnGameClickListener {
    private var _binding: FragmentPendienteBinding? = null
    private val binding get() = _binding!!
    private lateinit var gameAdapter: GameAdapter
    private lateinit var rawgApi: RawgApi
    private val gamesList = mutableListOf<Game>()  // Lista para almacenar los juegos
    private lateinit var listenerRegistration: ListenerRegistration  // Para registrar el listener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPendienteBinding.inflate(inflater, container, false)

        // Configurar el RecyclerView con 'this' como listener
        gameAdapter = GameAdapter(gamesList, this)
        binding.recyclerPendiente.adapter = gameAdapter
        binding.recyclerPendiente.layoutManager = LinearLayoutManager(requireContext())

        // Inicializa la animación Lottie y el RecyclerView
        binding.lottieLoading.visibility = View.VISIBLE // Muestra el Lottie
        binding.recyclerPendiente.visibility = View.GONE // Oculta el RecyclerView



        // Inicializar la API de RAWG
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.rawg.io/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        rawgApi = retrofit.create(RawgApi::class.java)

        // Obtener y mostrar los juegos "Pendiente"
        val apiKey = getString(R.string.rawg_api_key)
        listenForGameChanges(apiKey) // Cambia la llamada para escuchar cambios

        return binding.root
    }
// Escuchar cambios en la colección "Pendiente"
    private fun listenForGameChanges(apiKey: String) {
        val userId = getCurrentUserId() // Obtén el ID del usuario

        // Escuchar los cambios en la colección "Pendiente"
        listenerRegistration = FirebaseFirestore.getInstance()
            .collection("Usuarios")
            .document(userId)
            .collection("Pendiente")
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    Log.e("PendienteFragment", "Error al escuchar cambios: ${error.message}")
                    return@addSnapshotListener
                }

                val juegoIds = documents?.mapNotNull { it.id } ?: emptyList()
                Log.d("PendienteFragment", "IDs de juegos actuales: $juegoIds")

                if (juegoIds.isNotEmpty()) {
                    fetchGameDetailsFromRawg(apiKey, juegoIds)
                } else {
                    binding.errorTextView.visibility = View.VISIBLE
                    binding.errorTextView.text = "No tienes juegos en esta lista."
                    gameAdapter.updateGames(emptyList()) // Actualizar la lista en el adaptador
                    binding.lottieLoading.visibility = View.GONE // Ocultar Lottie
                    binding.recyclerPendiente.visibility = View.GONE // Ocultar RecyclerView
                }
            }
    }
// Obtener detalles de los juegos desde RAWG
    private fun fetchGameDetailsFromRawg(apiKey: String, juegoIds: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            val fetchedGames = mutableListOf<Game>()
            var connectionError = false // Variable para rastrear errores de conexión

            for (id in juegoIds) {
                try {
                    val gameId: Int = id.toInt() // Convertir el id de String a Int
                    val response = rawgApi.getGameDetails(gameId, apiKey)

                    if (response.isSuccessful) {
                        response.body()?.let { fetchedGames.add(it) }
                        Log.d("PendienteFragment", "Juego añadido: ${response.body()?.name}")
                    } else {
                        Log.e("PendienteFragment", "Error en la respuesta de la API: ${response.code()} ${response.message()}")
                        connectionError = true // Hay un problema con la conexión
                    }
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                    Log.e("PendienteFragment", "Error al obtener detalles del juego con ID: $id, ${e.message}")
                    connectionError = true // Hay un problema de conexión
                }
            }

            withContext(Dispatchers.Main) {
                binding.lottieLoading.visibility = View.GONE // Ocultar el Lottie al final de la carga

                if (fetchedGames.isNotEmpty()) {
                    gameAdapter.updateGames(fetchedGames)
                    binding.recyclerPendiente.visibility = View.VISIBLE // Mostrar RecyclerView
                    binding.errorTextView.visibility = View.GONE // Ocultar mensaje de error
                } else {
                    // Aquí se verifica si hubo un error de conexión
                    if (connectionError) {
                        binding.errorTextView.visibility = View.VISIBLE
                        binding.errorTextView.text = "No se ha podido establecer conexión."
                    } else {
                        binding.errorTextView.visibility = View.VISIBLE
                        binding.errorTextView.text = "No tienes juegos en esta lista."
                    }
                    binding.recyclerPendiente.visibility = View.GONE // Ocultar RecyclerView
                }
            }
        }
    }

    override fun onGameClick(game: Game) {
        val intent = Intent(requireContext(), GameViewActivity::class.java)
        val bundle = Bundle().apply {
            putInt("id", game.id)
            putString("name", game.name)
            putString("released", game.released)
            putString("background_image", game.background_image)
            putDouble("rating", game.rating)
        }

        intent.putExtras(bundle)
        startActivity(intent)
    }

    private fun getCurrentUserId(): String {
        val prefs = requireActivity().getSharedPreferences(getString(R.string.prefs_file), AppCompatActivity.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        Log.d("PendienteFragment", email.toString())
        return email ?: ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Detener el listener al destruir la vista
        listenerRegistration.remove()
        _binding = null
    }
}

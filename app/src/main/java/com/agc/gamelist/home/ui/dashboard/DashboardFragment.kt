package com.agc.gamelist.home.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R
import com.agc.gamelist.activity.GameViewActivity
import com.agc.gamelist.adapters.GameAdapter
import com.agc.gamelist.databinding.FragmentDashboardBinding
import com.agc.gamelist.model.Game
import com.agc.gamelist.network.RawgApi
import com.bumptech.glide.Glide
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Fragmento que muestra una lista de juegos.
 */
class DashboardFragment : Fragment(), OnGameClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var gameAdapter: GameAdapter
    private lateinit var rawgApi: RawgApi
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var job: Job? = null // Nueva propiedad Job para manejar las corutinas
    private var isFirstLoad: Boolean = true // Variable para controlar la primera carga
    private var hasFetchedGames: Boolean = false // Variable para controlar si los juegos ya fueron cargados

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        gameAdapter = GameAdapter(emptyList(), this) // Pasa 'this' como listener
        recyclerView.adapter = gameAdapter

        setupSwipeRefresh() // Configurar el SwipeRefreshLayout
        setupSearchView()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.rawg.io/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        rawgApi = retrofit.create(RawgApi::class.java)

        // Solo cargar los juegos si aún no se han cargado
        if (!hasFetchedGames) {
            val apiKey = getString(R.string.rawg_api_key)
            fetchGames(apiKey) // Cargar todos los juegos al inicio
            hasFetchedGames = true // Marcar que los juegos han sido cargados
        }

        return root
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            val apiKey = getString(R.string.rawg_api_key)
            fetchGames(apiKey) // Recargar los juegos
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
// Configurar el SearchView
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchGames(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }
// Realizar la búsqueda de juegos
    private fun searchGames(query: String) {
        binding.errorTextView.visibility = View.GONE // Ocultar el mensaje de error
        binding.swipeRefreshLayout.isRefreshing = true // Activar la animación de refresco

        val apiKey = getString(R.string.rawg_api_key)

        // Cancelar la tarea anterior antes de iniciar una nueva
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = rawgApi.searchGames(apiKey, page = 1, pageSize = 40, query = query)
                withContext(Dispatchers.Main) { // Cambiar de contexto a Main
                    if (response.isSuccessful) {
                        val games = response.body()?.results ?: emptyList()
                        gameAdapter.updateGames(games) // Actualiza el adaptador con los resultados de la búsqueda
                        updateNoResultsMessage()
                    } else {
                        // Mostrar el mensaje de error
                        binding.errorTextView.visibility = View.VISIBLE
                    }
                    // Siempre ocultar la animación en la búsqueda
                    binding.lottieLoading.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false // Detener la animación de refresco
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Mostrar el mensaje de error
                    binding.errorTextView.visibility = View.VISIBLE
                    // Enviar el error a Firebase Crashlytics
                    FirebaseCrashlytics.getInstance().recordException(e)
                    binding.lottieLoading.visibility = View.GONE // Ocultar la animación
                    binding.swipeRefreshLayout.isRefreshing = false // Detener la animación de refresco
                }
            }
        }
    }
// Cargar juegos desde la API
    private fun fetchGames(apiKey: String) {
        binding.errorTextView.visibility = View.GONE // Ocultar el mensaje de error

        // Mostrar la animación Lottie solo en la primera carga
        if (isFirstLoad) {
            binding.lottieLoading.playAnimation() // Activar la animación Lottie
            binding.lottieLoading.visibility = View.VISIBLE // Hacer visible la animación
            isFirstLoad = false // Cambiar a false después de la primera carga
        }

        binding.swipeRefreshLayout.isRefreshing = true // Activar el indicador de refresco

        job?.cancel() // Cancelar cualquier tarea anterior
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = rawgApi.getGames(apiKey, page = 1, pageSize = 40)
                withContext(Dispatchers.Main) { // Cambiar de contexto a Main
                    if (response.isSuccessful) {
                        val games = response.body()?.results ?: emptyList()
                        gameAdapter.updateGames(games) // Carga inicial de juegos
                        updateNoResultsMessage()
                    } else {
                        // Mostrar el mensaje de error
                        binding.errorTextView.visibility = View.VISIBLE
                    }
                    binding.lottieLoading.visibility = View.GONE // Ocultar animación Lottie
                    binding.swipeRefreshLayout.isRefreshing = false // Detener el refresco
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Mostrar el mensaje de error
                    binding.errorTextView.visibility = View.VISIBLE
                    binding.lottieLoading.visibility = View.GONE // Ocultar animación Lottie
                    binding.swipeRefreshLayout.isRefreshing = false // Detener el refresco
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job?.cancel() // Cancelar corutinas cuando se destruye la vista
        _binding = null
    }

    private fun updateNoResultsMessage() {
        if (gameAdapter.itemCount == 0) {
            _binding?.errorTextView?.apply {
                text = getString(R.string.no_resultados)
                visibility = View.VISIBLE
            }
            binding.lottieLoading.visibility = View.GONE // Ocultar la animación si no hay resultados
        } else {
            _binding?.errorTextView?.apply {
                text = ""
                visibility = View.GONE
            }
        }
    }
}

interface OnGameClickListener {
    fun onGameClick(game: Game)
}





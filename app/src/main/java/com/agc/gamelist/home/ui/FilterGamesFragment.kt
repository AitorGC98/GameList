package com.agc.gamelist.home.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R
import com.agc.gamelist.adapters.GameFilterAdapter
import com.agc.gamelist.api.NewsApiService
import com.agc.gamelist.api.RetrofitClient
import com.agc.gamelist.model.Game
import com.agc.gamelist.model.Publisher
import com.agc.gamelist.network.RawgApi
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Array.set
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class FilterGamesFragment : Fragment() {
    private lateinit var apiKey: String
    private val adapters = mutableMapOf<String, GameFilterAdapter>()
    private lateinit var errorTextView: TextView // Declaración del TextView

    // Inicializa los TextViews
    private lateinit var textViewGeneral: TextView
    private lateinit var textViewFree: TextView
    private lateinit var textViewHistory: TextView
    private lateinit var textViewShort: TextView
    private lateinit var textViewBugs: TextView
    private lateinit var textViewSteam: TextView
    private lateinit var textViewMulti: TextView
    private lateinit var textViewYear: TextView
    private lateinit var textViewNintendo: TextView
    private lateinit var textViewPlay: TextView
    private lateinit var textViewXbox: TextView
    private lateinit var lottieAnimationView: LottieAnimationView

    // Crear un Handler para el hilo principal
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filter_games, container, false)
        apiKey = getString(R.string.rawg_api_key)

        // Inicializa los TextViews
        textViewGeneral = view.findViewById(R.id.titleTextView)
        textViewFree = view.findViewById(R.id.titleTextViewFree)
        textViewHistory = view.findViewById(R.id.titleTextViewHistoria)
        textViewShort = view.findViewById(R.id.titleTextViewCorto)
        textViewBugs = view.findViewById(R.id.titleTextViewLargos)
        textViewSteam = view.findViewById(R.id.titleTextViewSteam)
        textViewMulti = view.findViewById(R.id.titleTextViewMulti)
        textViewYear = view.findViewById(R.id.titleTextViewAno)
        textViewNintendo = view.findViewById(R.id.titleTextViewNintendo)
        textViewPlay = view.findViewById(R.id.titleTextViewPlay)
        textViewXbox = view.findViewById(R.id.titleTextViewXbox)
        // Inicializar LottieAnimationView
        lottieAnimationView = view.findViewById(R.id.lottieLoading)

        // Inicializa los TextViews
        errorTextView = view.findViewById(R.id.errorTextView)
        errorTextView.visibility = View.GONE // Asegúrate de que el TextView de error esté inicialmente oculto

        setupRecyclerViews(view)
        fetchGamesData()

        return view
    }

    private fun setupRecyclerViews(view: View) {
        val recyclerViewIds = listOf(
            R.id.recyclerViewGames to "general",
            R.id.recyclerViewGamesFree to "free",
            R.id.recyclerViewGamesHistoria to "history",
            R.id.recyclerViewGamesCorto to "short",
            R.id.recyclerViewGamesLargos to "bugs",
            R.id.recyclerViewGamesSteam to "steam",
            R.id.recyclerViewGamesMulti to "multi",
            R.id.recyclerViewGamesAno to "year",
            R.id.recyclerViewGamesNintendo to "nintendo",
            R.id.recyclerViewGamesPlay to "play",
            R.id.recyclerViewGamesXbox to "xbox" // Cambiado para tener un ID único
        )

        for ((recyclerId, adapterKey) in recyclerViewIds) {
            val recyclerView: RecyclerView = view.findViewById(recyclerId)
            recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            val adapter = GameFilterAdapter(emptyList())
            recyclerView.adapter = adapter
            adapters[adapterKey] = adapter
        }
    }

    private fun fetchGamesData() {
        CoroutineScope(Dispatchers.IO).launch {
            val api = Retrofit.Builder()
                .baseUrl("https://api.rawg.io/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RawgApi::class.java)

            try {
                val responses = listOf(
                    async { api.getGamesReleasedThisMonth(apiKey, getDateRange()) },
                    async { api.getUpcomingGames(apiKey, getNextMonthDateRange(), pageSize = 10) },
                    async { api.getTopGames(apiKey, ordering = "-metacritic", pageSize = 10) },
                    async { api.getShortGames(apiKey) },
                    async { api.getTopUbisoftGames(apiKey) },
                    async { api.getSteamLeaderboardGames(apiKey) },
                    async { api.getMultiplayerGames(apiKey, tags = "multiplayer", dates = getCurrentYearDateRange()) },
                    async { api.getReleasedThisYear(apiKey, dates = getCurrentYearDateRange()) },
                    async { api.getGamesByPublisher(apiKey, "10681", getLastTwoMonthsDateRange()) }, // Nintendo
                    async { api.getGamesByPublisher(apiKey, "11687", getLastTwoMonthsDateRange()) }, // Play
                    async { api.getGamesByPublisher(apiKey, "34843", getLastTwoMonthsDateRange()) }  // Xbox
                )

                val results = responses.awaitAll()

                // Usa el Handler para actualizar la UI
                mainHandler.post {
                    // Actualiza los adaptadores
                    adapters["general"]?.updateData(results[0].body()?.results ?: emptyList())
                    adapters["free"]?.updateData(results[1].body()?.results ?: emptyList())
                    adapters["history"]?.updateData(results[2].body()?.results ?: emptyList())
                    adapters["short"]?.updateData(results[3].body()?.results ?: emptyList())
                    adapters["bugs"]?.updateData(results[4].body()?.results ?: emptyList())
                    adapters["steam"]?.updateData(results[5].body()?.results ?: emptyList())
                    adapters["multi"]?.updateData(results[6].body()?.results ?: emptyList())
                    adapters["year"]?.updateData(results[7].body()?.results ?: emptyList())
                    adapters["nintendo"]?.updateData(results[8].body()?.results ?: emptyList())
                    adapters["play"]?.updateData(results[9].body()?.results ?: emptyList())
                    adapters["xbox"]?.updateData(results[10].body()?.results ?: emptyList())

                    // Hacer visibles los TextViews
                    textViewGeneral.visibility = View.VISIBLE
                    textViewFree.visibility = View.VISIBLE
                    textViewHistory.visibility = View.VISIBLE
                    textViewShort.visibility = View.VISIBLE
                    textViewBugs.visibility = View.VISIBLE
                    textViewSteam.visibility = View.VISIBLE
                    textViewMulti.visibility = View.VISIBLE
                    textViewYear.visibility = View.VISIBLE
                    textViewNintendo.visibility = View.VISIBLE
                    textViewPlay.visibility = View.VISIBLE
                    textViewXbox.visibility = View.VISIBLE

                    // Oculta el TextView de error si todo va bien
                    errorTextView.visibility = View.GONE
                    lottieAnimationView.visibility = View.GONE // Mostrar animación
                }
            } catch (e: Exception) {
                Log.e("FilterGamesFragment", "Error fetching games data: ${e.message}")

                // Usa el Handler para actualizar la UI en caso de error
                mainHandler.post {
                    lottieAnimationView.visibility = View.GONE // Mostrar animación

                    errorTextView.visibility = View.VISIBLE // Muestra el TextView de error
                }
            }
        }
    }




    private fun getLastTwoMonthsDateRange(): String {
        val calendarEnd = Calendar.getInstance()
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendarEnd.time)

        val calendarStart = Calendar.getInstance().apply { add(Calendar.MONTH, -10) }
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendarStart.time)

        return "$startDate,$endDate"
    }

    private fun getDateRange(): String {
        val calendarStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendarStart.time)

        val calendarEnd = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendarEnd.time)

        return "$startDate,$endDate"
    }

    private fun getNextMonthDateRange(): String {
        val calendar = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        return "$startDate,$endDate"
    }

    private fun getCurrentYearDateRange(): String = "2024-01-01,2024-10-10"

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancelar cualquier tarea programada en el Handler
        mainHandler.removeCallbacksAndMessages(null) // Cancela todos los callbacks y mensajes
    }

}



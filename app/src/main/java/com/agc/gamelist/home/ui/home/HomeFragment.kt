package com.agc.gamelist.home.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.agc.gamelist.R
import com.agc.gamelist.adapters.NewsAdapter
import com.agc.gamelist.api.NewsApiService
import com.agc.gamelist.databinding.FragmentHomeBinding
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var newsAdapter: NewsAdapter
    private var job: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inicializar el RecyclerView
        val staggeredGridLayoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.recyclerView.layoutManager = staggeredGridLayoutManager

        // Configurar el adaptador vacío inicialmente
        newsAdapter = NewsAdapter(emptyList()) { article ->
            // Abrir la noticia en el navegador al hacer clic
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
            startActivity(intent)
        }
        binding.recyclerView.adapter = newsAdapter

        // Configurar el SwipeRefreshLayout
        setupSwipeRefresh()

        // Llamar a la función que obtiene las noticias
        fetchGamingNews()

        return root
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchGamingNews() // Recargar las noticias
        }
    }

    private fun fetchGamingNews() {
        job?.cancel() // Cancelar cualquier trabajo anterior
        job = Job() // Iniciar un nuevo Job

        job = lifecycleScope.launch {
            binding.lottieLoading.visibility = View.VISIBLE // Mostrar Lottie durante la carga
            binding.errorTextView.visibility = View.GONE // Ocultar el mensaje de error
            newsAdapter.updateArticles(emptyList()) // Vaciar el adaptador antes de cargar nuevos datos
            binding.swipeRefreshLayout.isRefreshing = true // Activar el indicador de refresco

            try {
                // Llamada a la API con Retrofit
                val apiKey = requireContext().getString(R.string.newsapi_api_key)
                val response = NewsApiService.api.getGamingNews(apiKey = apiKey)
                Log.e("API NOTICIAS", "Error: ${response.status}. Mensaje: ${response}")
                // Cambiar a hilo principal para actualizar la UI
                withContext(Dispatchers.Main) {
                    if (response.status == "ok") {
                        // Barajar la lista de artículos antes de actualizar el adaptador
                        val shuffledArticles = response.articles.shuffled()
                        newsAdapter.updateArticles(shuffledArticles)
                    } else {
                        binding.errorTextView.visibility = View.VISIBLE // Mostrar mensaje de error
                    }
                }
            } catch (e: Exception) {

                // Manejo de errores mejorado
                withContext(Dispatchers.Main) {
                    binding.errorTextView.visibility = View.VISIBLE // Mostrar mensaje de error
                    // Enviar el error a Firebase Crashlytics
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            } finally {
                binding.lottieLoading.visibility = View.GONE // Ocultar Lottie al finalizar la carga
                binding.swipeRefreshLayout.isRefreshing = false // Detener el refresco
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job?.cancel() // Cancelar cualquier corutina en ejecución
        _binding = null
    }
}





package com.agc.gamelist.home.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.agc.gamelist.R
import com.agc.gamelist.adapters.ViewPagerAdapter
import com.agc.gamelist.databinding.FragmentNotificationsBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        // Obtén referencias a TabLayout y ViewPager desde el binding
        val tabLayout = binding.tabLayout
        val viewPager = binding.viewPager

        // Configura el adaptador del ViewPager
        val adapter = ViewPagerAdapter(requireActivity())
        viewPager.adapter = adapter


        // Detectar el modo actual: claro u oscuro
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK

        // Cambiar el fondo según el modo
        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // Modo oscuro: fondo negro
            _binding?.mainLayout?.setBackgroundResource(R.color.black2)
        } else {
            // Modo claro: fondo blanco
            _binding?.mainLayout?.setBackgroundResource(R.drawable.background_cuadrado)  // Fondo claro (debes definir este drawable en res/drawable)
        }

        // Conecta el TabLayout con el ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Jugando"
                1 -> "Terminado"
                2 -> "Pendiente"
                3 -> "Abandonado"
                else -> null
            }
        }.attach()
        // Configuración de apariencia para el TabLayout
        tabLayout.apply {


            // Cambiar el fondo de las pestañas
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorMorado))
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.agc.gamelist.home.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.agc.gamelist.R
import com.agc.gamelist.adapters.ForumPagerAdapter
import com.agc.gamelist.fragments.ExplorarFragment
import com.agc.gamelist.fragments.MisForosFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ForumFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: ForumPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout del fragmento
        val view = inflater.inflate(R.layout.fragment_forum, container, false)

        // Inicializar el TabLayout y el ViewPager
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)

        // Configurar el adaptador para el ViewPager
        pagerAdapter = ForumPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // Conectar el ViewPager con el TabLayout
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Mis Foros"
                1 -> tab.text = "Explorar"
            }
        }.attach()

        return view
    }
}


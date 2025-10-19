package com.agc.gamelist.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.agc.gamelist.fragments.AbandonadoFragment
import com.agc.gamelist.fragments.JugandoFragment
import com.agc.gamelist.fragments.PendienteFragment
import com.agc.gamelist.fragments.TerminadoFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // Número de pestañas
    private val NUM_TABS = 4

    override fun getItemCount(): Int = NUM_TABS

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> JugandoFragment()
            1 -> TerminadoFragment()
            2 -> PendienteFragment()
            3 -> AbandonadoFragment()
            else -> JugandoFragment()
        }
    }
}

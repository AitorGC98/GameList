package com.agc.gamelist.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.agc.gamelist.fragments.ExplorarFragment
import com.agc.gamelist.fragments.MisForosFragment

class ForumPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        // Número de pestañas (2: Mis Foros y Explorar)
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        // Retorna el fragmento correspondiente a cada pestaña
        return when (position) {
            0 -> MisForosFragment()
            1 -> ExplorarFragment()
            else -> throw IllegalStateException("Unexpected position: $position")
        }
    }
}

package com.agc.gamelist.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R

class BaneadoAdapter(
    private val baneados: List<Map<String, String>>,
    private val onDesbanearClick: (String) -> Unit // Callback para el desbaneo
) : RecyclerView.Adapter<BaneadoAdapter.BaneadoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaneadoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_baneado, parent, false)
        return BaneadoViewHolder(view)
    }

    override fun onBindViewHolder(holder: BaneadoViewHolder, position: Int) {
        val baneado = baneados[position]
        holder.bind(baneado)

        // Agregar un listener para el clic en el ítem
        holder.itemView.setOnClickListener {
            val email = baneado["email"] // Obtener el email del baneado
            if (email != null) {
                // Mostrar el menú
                showPopupMenu(holder.itemView, email)
            }
        }
    }
// Mostrar el menú de opciones
    private fun showPopupMenu(view: View, email: String) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.menuInflater.inflate(R.menu.menu_baneado, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.opcionDesbanear -> {
                    onDesbanearClick(email) // Llamar al callback para desbanear
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    override fun getItemCount(): Int = baneados.size

    class BaneadoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nombreTextView: TextView = itemView.findViewById(R.id.nombreBaneado)

        fun bind(baneado: Map<String, String>) {
            nombreTextView.text = baneado["email"] ?: "Email no disponible"
        }
    }
}

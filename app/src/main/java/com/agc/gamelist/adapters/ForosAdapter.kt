package com.agc.gamelist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R
import com.google.firebase.firestore.FirebaseFirestore

class ForosAdapter(
    private val listaForos: List<Foro>,
    private val onItemClick: (Foro) -> Unit
) : RecyclerView.Adapter<ForosAdapter.ForoViewHolder>() {

    private val db = FirebaseFirestore.getInstance() // Instancia de Firestore

    inner class ForoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tituloTextView: TextView = view.findViewById(R.id.titulo_forum)
        val descripcionTextView: TextView = view.findViewById(R.id.descripcion_forum)
        val numeroUsuariosTextView: TextView = view.findViewById(R.id.numero_usuarios)

        fun bind(foro: Foro) {
            tituloTextView.text = foro.titulo
            descripcionTextView.text = foro.descripcion

            // Llamada a Firestore para contar los usuarios en el foro
            db.collection("Foros")
                .document(foro.id)
                .collection("Usuarios")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val count = querySnapshot.size()
                    numeroUsuariosTextView.text = "$count"
                }
                .addOnFailureListener {
                    numeroUsuariosTextView.text = "0" // Valor por defecto en caso de error
                }

            itemView.setOnClickListener {
                onItemClick(foro)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_foro, parent, false)
        return ForoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForoViewHolder, position: Int) {
        holder.bind(listaForos[position])
    }

    override fun getItemCount() = listaForos.size
}

data class Foro(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = ""
)

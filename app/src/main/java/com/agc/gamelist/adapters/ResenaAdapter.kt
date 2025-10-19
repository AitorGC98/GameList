package com.agc.gamelist.adapters

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R
import com.agc.gamelist.activity.PerfilUsuarioActivity
import com.agc.gamelist.model.Resenna
import java.text.SimpleDateFormat
import java.util.Locale

class ResennaAdapter(private val resenas: List<Resenna>) : RecyclerView.Adapter<ResennaAdapter.ResenaViewHolder>() {

    class ResenaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreUsuario: TextView = view.findViewById(R.id.nombrUsuario)
        val comentarioUsuario: TextView = view.findViewById(R.id.ComentarioUsuario)
        val recommendationText: TextView = view.findViewById(R.id.recommendationText)
        val recommendationIcon: ImageView = view.findViewById(R.id.recommendationIcon)
        val fechaPublicacion: TextView = view.findViewById(R.id.fechaPublicacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResenaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return ResenaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResenaViewHolder, position: Int) {
        val resena = resenas[position]

        // Nombre del usuario o "Anónimo" si no existe
        holder.nombreUsuario.text = resena.nombreUsuario ?: "Anónimo"

        holder.nombreUsuario.setOnClickListener {
            // Crea el Intent para abrir PerfilUsuario
            val intent = Intent(holder.itemView.context, PerfilUsuarioActivity::class.java)
            // Agrega el correo del usuario como extra en el Intent
            intent.putExtra("usuario_email", resena.userId)
            // Inicia la actividad PerfilUsuario
            holder.itemView.context.startActivity(intent)
        }
        // Comentario del usuario
        holder.comentarioUsuario.text = resena.comentario

        // Fecha de publicación, formateada
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.fechaPublicacion.text = formatoFecha.format(resena.fecha)

        // Recomendación: cambiar texto e ícono según el valor de 'recomendado'
        if (resena.recomendado == true) {
            holder.recommendationText.text = "Recomendado"
            holder.recommendationText.setTextColor(Color.parseColor("#4CAF50")) // Verde
            holder.recommendationIcon.setImageResource(R.drawable.ic_like_resena) // Cambia al ícono de "Me gusta"
        } else {
            holder.recommendationText.text = "No recomendado"
            holder.recommendationText.setTextColor(Color.parseColor("#F44336")) // Rojo
            holder.recommendationIcon.setImageResource(R.drawable.ic_dislike_resena) // Cambia al ícono de "No me gusta"
        }
    }

    override fun getItemCount(): Int {
        return resenas.size
    }
}

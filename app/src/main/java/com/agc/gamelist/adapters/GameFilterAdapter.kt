package com.agc.gamelist.adapters

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R
import com.agc.gamelist.activity.GameViewActivity
import com.agc.gamelist.model.Game
import com.bumptech.glide.Glide

class GameFilterAdapter(private var games: List<Game>) : RecyclerView.Adapter<GameFilterAdapter.GameViewHolder>() {

    // Método para actualizar los datos en el adaptador
    fun updateData(newGames: List<Game>) {
        games = newGames
        notifyDataSetChanged() // Notificar cambios en los datos
    }

    // Crear la vista del ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_filter, parent, false)
        return GameViewHolder(view)
    }

    // Vincular los datos a la vista del ViewHolder
    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]
        Log.d("GameFilterAdapter", "Binding game: ${game.name} at position $position") // Para verificar qué juegos se están vinculando
        holder.bind(game)

        // Configurar el clic en el item
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, GameViewActivity::class.java)
            val bundle = Bundle().apply {
                putInt("id", game.id)
                putString("name", game.name)
                putString("released", game.released)
                putString("background_image", game.background_image)
                putDouble("rating", game.rating)
            }
            intent.putExtras(bundle)
            context.startActivity(intent) // Iniciar la nueva actividad
        }
    }

    override fun getItemCount(): Int = games.size

    // Clase interna para el ViewHolder
    class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val gameImageView: ImageView = itemView.findViewById(R.id.gameImageView)
        private val gameTitleTextView: TextView = itemView.findViewById(R.id.gameTitleTextView)

        // Método para vincular datos
        fun bind(game: Game) {
            // Cargar la imagen y el título del juego
            gameTitleTextView.text = game.name
            // Usa Glide para cargar la imagen en el ImageView
            // Usa Glide para cargar la imagen en el ImageView
            // Usa Glide para cargar la imagen en el ImageView
            Glide.with(itemView.context)
                .load(game.background_image) // Usa el campo correcto para la URL de la imagen
                .error(R.drawable.ic_no_image) // Imagen predeterminada si no se encuentra la URL
                .into(gameImageView) // Cambia holder por gameImageView

        }
    }
}


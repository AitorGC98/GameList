package com.agc.gamelist.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R
import com.agc.gamelist.R.layout.item_game
import com.agc.gamelist.home.ui.dashboard.OnGameClickListener
import com.agc.gamelist.model.Game
import com.bumptech.glide.Glide
class GameAdapter(private var games: List<Game>, private val listener: OnGameClickListener) :
    RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    // Clase interna que representa cada elemento de la lista
    inner class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gameImage: ImageView = itemView.findViewById(R.id.image_view)
        val gameName: TextView = itemView.findViewById(R.id.title_text)
        val gameReleaseDate: TextView = itemView.findViewById(R.id.address_text)
        val gameRating: TextView = itemView.findViewById(R.id.note_text)

        init {
            itemView.setOnClickListener {
                // Notifica al listener con el juego seleccionado
                listener.onGameClick(games[adapterPosition])
            }
        }
    }

    // Crea el ViewHolder para un nuevo elemento de la lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game, parent, false)
        return GameViewHolder(view)
    }

    // Vincula los datos del juego al ViewHolder correspondiente
    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]
        holder.gameName.text = game.name
        // Cambiar de fecha de lanzamiento a plataformas
        val platformsText = game.platforms?.joinToString(", "){ it.platform.name }?: "No platforms available"

        holder.gameReleaseDate.text =platformsText
        holder.gameRating.text = "${game.rating} ★"

        // Cargar la imagen del juego usando Glide
        Glide.with(holder.itemView.context)
            .load(game.background_image)
            .into(holder.gameImage)
    }

    // Devuelve la cantidad de elementos en la lista
    override fun getItemCount(): Int = games.size

    // Método para actualizar los juegos en el adaptador
    fun updateGames(newGames: List<Game>) {
        games = newGames
        notifyDataSetChanged()  // Notifica que los datos han cambiado para que el RecyclerView se actualice
    }
}

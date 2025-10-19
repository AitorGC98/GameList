package com.agc.gamelist.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.api.Article
import com.agc.gamelist.databinding.ItemNewsBinding
import com.bumptech.glide.Glide

class NewsAdapter(
    private var articles: List<Article>,
    private val onItemClick: (Article) -> Unit
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(val binding: ItemNewsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article = articles[position]
        holder.binding.titleTextView.text = article.title

        // Usar Glide para cargar la imagen de la noticia
        Glide.with(holder.itemView.context)
            .load(article.urlToImage)
            .into(holder.binding.imageView)

        // Configurar el tamaño aleatorio de la imagen (por ejemplo)
        val randomHeight = (100..200).random() // Altura aleatoria entre 100dp y 200dp
        holder.binding.imageView.layoutParams.height = randomHeight.toDp(holder.itemView.context)

        // Configurar el click listener para abrir el artículo
        holder.itemView.setOnClickListener {
            onItemClick(article)
        }
    }
    fun Int.toDp(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }


    override fun getItemCount() = articles.size

    // Método para actualizar la lista de artículos
    fun updateArticles(newArticles: List<Article>) {
        articles = newArticles
        notifyDataSetChanged()  // Notificar al adaptador que los datos han cambiado
    }
}


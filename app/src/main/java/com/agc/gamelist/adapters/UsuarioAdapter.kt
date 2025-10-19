package com.agc.gamelist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R

class UsuarioAdapter(
    private val usuarios: List<Map<String, String>>,
    private val onUserClicked: (Map<String, String>, View) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    class UsuarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emailTextView: TextView = view.findViewById(R.id.textViewEmail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]
        holder.emailTextView.text = usuario["email"]

        holder.itemView.setOnClickListener {
            onUserClicked(usuario, holder.itemView)
        }
    }

    override fun getItemCount(): Int {
        return usuarios.size
    }
}

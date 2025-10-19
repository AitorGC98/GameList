package com.agc.gamelist.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R
import com.agc.gamelist.activity.Comentario
import com.agc.gamelist.activity.PerfilUsuarioActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore

class ComentariosAdapter(
    private val foroId: String,
    private val listaComentarios: List<Comentario>,
    private val onResponderClick: (Comentario) -> Unit,
    private val onMostrarRespuestasClick: (Comentario) -> Unit,
    private val onBorrarComentarioClick: (Comentario) -> Unit,
    private val currentUserId: String
) : RecyclerView.Adapter<ComentariosAdapter.ComentarioViewHolder>() {

    class ComentarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textoComentario: TextView = itemView.findViewById(R.id.texto_comentario)
        val botonBorrar: ImageButton = itemView.findViewById(R.id.imageButton_borrar)
        val botonResponder: ImageButton = itemView.findViewById(R.id.imageButton_mostrar_respuestas)
        val textoMostrarMas: TextView = itemView.findViewById(R.id.texto_mostrar_mas)  // Cambiado a TextView
        val listaRespuestas: RecyclerView = itemView.findViewById(R.id.recycler_view_respuestas)
        val fotoPerfil: ImageView = itemView.findViewById(R.id.profile_image)
        val textoNombre: TextView = itemView.findViewById(R.id.texto_nombre)
        val botonLike: ImageButton = itemView.findViewById(R.id.imageButton_like)
        val botonDislike: ImageButton = itemView.findViewById(R.id.imageButton_dislike)
        val textoNumLikes: TextView = itemView.findViewById(R.id.texto_num_likes)
        val textoNumDislikes: TextView = itemView.findViewById(R.id.texto_num_dislikes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comentario, parent, false)
        return ComentarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {
        val comentario = listaComentarios[position]

        holder.textoComentario.text = comentario.texto
        holder.textoNumLikes.text = comentario.numLikes.toString()
        holder.textoNumDislikes.text = comentario.numDislikes.toString()
        holder.botonResponder.setOnClickListener {
            onResponderClick(comentario)
        }
        // Obtener el contexto desde el itemView del ViewHolder
        val context = holder.itemView.context
        // Obtener el voto del usuario actual
        val currentVote = comentario.userVotes[currentUserId] ?: 0

        // Cambiar la apariencia de los botones según el estado del voto
        holder.botonLike.isSelected = currentVote == 1
        holder.botonDislike.isSelected = currentVote == -1

        // Cambiar iconos de like/dislike según el voto
        holder.botonLike.setImageResource(if (currentVote == 1) R.drawable.ic_like_active else R.drawable.ic_like)
        holder.botonDislike.setImageResource(if (currentVote == -1) R.drawable.ic_dislike_active else R.drawable.ic_dislike)

        // Establecer el nombre del usuario
        holder.textoNombre.text = comentario.nombreUsuario

        // Agregar el escuchador para el clic en el nombre del usuario
        holder.textoNombre.setOnClickListener {
            // Crear un Intent para pasar a la siguiente actividad
            val intent = Intent(holder.itemView.context, PerfilUsuarioActivity::class.java)

            // Pasa el correo del usuario a la actividad
            intent.putExtra("usuario_email", comentario.userId) // Asegúrate de que el objeto comentario tenga el correo del usuario.

            // Inicia la actividad
            holder.itemView.context.startActivity(intent)
        }

        // Lógica de like/dislike
        holder.botonLike.setOnClickListener {
            handleVote(holder, comentario, 1) // 1 para like
        }

        holder.botonDislike.setOnClickListener {
            handleVote(holder, comentario, -1) // -1 para dislike
        }
        // Implementar el evento de larga pulsación
        holder.itemView.setOnLongClickListener {
            showPopupMenu(holder.itemView, comentario)
            true // Devuelve true para indicar que el evento se ha manejado
        }

        // Cargar la imagen desde drawable usando el nombre almacenado en comentario.fotoPerfil
        val resourceId = context.resources.getIdentifier(comentario.fotoPerfil, "drawable", context.packageName)


        // Mostrar el botón de borrar solo si el comentario pertenece al usuario actual
        holder.botonBorrar.visibility = if (comentario.userId == currentUserId) View.VISIBLE else View.GONE

        // Lógica para el botón de borrar
        holder.botonBorrar.setOnClickListener {
            onBorrarComentarioClick(comentario)
        }
        val colorPersonalizado = ContextCompat.getColor(holder.itemView.context, R.color.white)
        // Marcar el comentario como borrado si corresponde
        if (comentario.isBorrado) {
            holder.textoComentario.setTextColor(Color.GRAY)
            holder.textoComentario.setTypeface(null, Typeface.ITALIC)
            holder.botonBorrar.visibility = View.GONE // Asegurarse de que se oculta el botón de borrar
        }else {
            holder.textoComentario.setTextColor(colorPersonalizado)
            holder.textoComentario.setTypeface(null, Typeface.NORMAL)
            holder.botonBorrar.visibility = if (comentario.userId == currentUserId) View.VISIBLE else View.GONE
        }


        holder.textoNombre.text = comentario.nombreUsuario
        holder.fotoPerfil.setImageResource(resourceId)

        // Log para verificar el estado de cada comentario
        Log.d("ComentariosAdapter", "Vinculando comentario ID: ${comentario.id}, Texto: ${comentario.texto}, Respuestas: ${comentario.respuestas.size}")

        // Configurar el RecyclerView para las respuestas
        holder.listaRespuestas.layoutManager = LinearLayoutManager(holder.itemView.context)

        // Establecer el adaptador para las respuestas
        holder.listaRespuestas.adapter = ComentariosAdapter(
            foroId,
            comentario.respuestas,
            onResponderClick,
            onMostrarRespuestasClick,
            onBorrarComentarioClick,
            currentUserId

        )

        Log.d("ComentariosAdapter", "BORRAAAAAAAR: ${comentario.userId}, Usuario actual: ${currentUserId}")
        // Mostrar el botón de borrar solo si el comentario pertenece al usuario actual
        holder.botonBorrar.visibility = if (comentario.userId == currentUserId) View.VISIBLE else View.GONE
        holder.botonBorrar.setOnClickListener {
            // Al hacer clic, llama al callback y espera a que se borre el comentario
            onBorrarComentarioClick(comentario) // Llama al callback de borrado
        }

        if (comentario.isBorrado) {
            holder.textoComentario.setTextColor(Color.GRAY)  // Cambiar el color del texto a gris
            holder.textoComentario.setTypeface(null, Typeface.ITALIC)  // Cambiar a cursiva
            holder.botonBorrar.visibility = View.GONE  // Ocultar el botón de borrar
        }
        val lineaRespuesta = holder.itemView.findViewById<View>(R.id.linea_respuesta)
        // Verificar si el comentario tiene respuestas
        tieneRespuestas(comentario.id) { hayRespuestas ->
            if (hayRespuestas) {
                holder.textoMostrarMas.visibility = View.VISIBLE
                Log.d("ComentariosAdapter", "El comentario ID: ${comentario.id} tiene respuestas.")
                // Aquí puedes manejar lo que deseas hacer si hay respuestas
            } else {
                holder.textoMostrarMas.visibility = View.GONE
                Log.d("ComentariosAdapter", "El comentario ID: ${comentario.id} no tiene respuestas.")
                // Aquí puedes manejar lo que deseas hacer si no hay respuestas
            }
        }

        // Verificamos si el comentario es un comentario "padre"
        val esComentarioPadre = comentario.idComentarioPadre == null

        if (esComentarioPadre) {
            // Lógica para el comentario padre
            Log.d("ComentariosAdapter", "Comentario ID: ${comentario.id} es un comentario padre.")
            lineaRespuesta.visibility = View.GONE //

            // Otras configuraciones para el comentario padre
        } else {
            lineaRespuesta.visibility = View.VISIBLE
            // Lógica para el comentario hijo (es una respuesta a otro comentario)
            Log.d("ComentariosAdapter", "Comentario ID: ${comentario.id} es una respuesta.")
        }

        holder.textoComentario.text = comentario.texto

        // Establecer la visibilidad inicial de las respuestas
        if (comentario.respuestasVisible) {
            holder.listaRespuestas.visibility = View.VISIBLE
            holder.textoMostrarMas.text = "Ocultar respuestas"
        } else {
            holder.listaRespuestas.visibility = View.GONE
            holder.textoMostrarMas.text = "Mostrar más"
        }

        // Configurar el click listener
        holder.textoMostrarMas.setOnClickListener {
            // Alternar visibilidad
            comentario.respuestasVisible = !comentario.respuestasVisible

            // Si está visible, mostrar respuestas o cargar respuestas si están vacías
            if (comentario.respuestasVisible) {
                if (comentario.respuestas.isEmpty()) {
                    // Cargar respuestas si están vacías
                    onMostrarRespuestasClick(comentario) // Cargar respuestas
                }
                holder.listaRespuestas.visibility = View.VISIBLE
                holder.textoMostrarMas.text = "Ocultar respuestas"
            } else {
                // Ocultar respuestas
                holder.listaRespuestas.visibility = View.GONE
                holder.textoMostrarMas.text = "Mostrar más"
            }

            // Notificar cambios
            notifyItemChanged(position) // Notificar que este item ha cambiado
        }
    }

    private fun tieneRespuestas(comentarioId: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        // Realiza una consulta para contar las respuestas del comentario
        db.collection("Foros").document(foroId).collection("Comentarios")
            .whereEqualTo("idComentarioPadre", comentarioId)  // Filtrar por el ID del comentario padre
            .get()
            .addOnSuccessListener { result ->
                // Si hay documentos en el resultado, significa que hay respuestas
                val hayRespuestas = result.size() > 0
                callback(hayRespuestas)  // Llama al callback con el resultado
            }
            .addOnFailureListener { e ->
                Log.e("ComentariosAdapter", "Error al verificar respuestas: ${e.message}")
                callback(false)  // En caso de error, asumimos que no hay respuestas
            }
    }

    private fun handleVote(holder: ComentarioViewHolder, comentario: Comentario, voteType: Int) {
        val previousVote = comentario.userVotes[currentUserId] ?: 0

        // Si el usuario ya votó, actualiza el conteo
        if (previousVote != 0) {
            if (previousVote == voteType) {
                // Si hace clic en el mismo voto, no hace nada
                return
            } else {
                // Resta el voto anterior
                if (previousVote == 1) {
                    comentario.numLikes--
                } else {
                    comentario.numDislikes--
                }
            }
        }

        // Actualiza el voto del usuario en el mapa
        comentario.userVotes[currentUserId] = voteType

        // Suma el nuevo voto
        if (voteType == 1) {
            comentario.numLikes++
        } else {
            comentario.numDislikes++
        }

        // Actualiza el TextView con los nuevos conteos
        holder.textoNumLikes.text = comentario.numLikes.toString()
        holder.textoNumDislikes.text = comentario.numDislikes.toString()

        // Guardar cambios en Firebase
        saveComentarioToFirebase(comentario)
    }

    private fun saveComentarioToFirebase(comentario: Comentario) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Foros").document(foroId).collection("Comentarios").document(comentario.id)
            .set(comentario)
            .addOnSuccessListener {
                Log.d("ComentariosAdapter", "Comentario actualizado con éxito.")
            }
            .addOnFailureListener { e ->
                Log.e("ComentariosAdapter", "Error al actualizar el comentario: ${e.message}")
            }
    }


    // Método para alternar la visibilidad de las respuestas
    private fun toggleRespuestasVisibility(holder: ComentarioViewHolder, comentario: Comentario) {
        if (holder.listaRespuestas.visibility == View.VISIBLE) {
            holder.listaRespuestas.visibility = View.GONE
            holder.textoMostrarMas.text = "Mostrar más"
        } else {
            holder.listaRespuestas.visibility = View.VISIBLE
            holder.textoMostrarMas.text = "Ocultar respuestas"
        }
    }

    private fun showPopupMenu(view: View, comentario: Comentario) {
        val popupMenu = PopupMenu(view.context, view)

        getUserRole() { userRole ->
            // Inflar el menú por defecto
            popupMenu.menuInflater.inflate(R.menu.comentario_menu, popupMenu.menu)

            // Si el usuario es administrador, añade las opciones adicionales
            if (userRole == "Administrador") {
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {

                        R.id.action_borrar -> {
                            borrarComentarioYRespuestas(comentario)
                            true
                        }

                        R.id.action_PermaBan -> {
                            // Verificar si el usuario a banear es un administrador
                            if (comentario.userId == currentUserId ) {
                                // Mostrar un Toast indicando que no se puede banear a un administrador
                                Toast.makeText(view.context, "No te puedes banear a ti mismo.", Toast.LENGTH_SHORT).show()
                            } else {
                                banearUsuario(comentario.userId, "permanente", Long.MAX_VALUE)
                            }
                            true
                        }

                        R.id.action_BanWeek -> {
                            // Verificar si el usuario a banear es un administrador
                            if (comentario.userId == currentUserId) {
                                // Mostrar un Toast indicando que no se puede banear a un administrador
                                Toast.makeText(view.context, "No te puedes banear a ti mismo.", Toast.LENGTH_SHORT).show()
                            } else {
                                val unaSemanaEnMilisegundos = 7 * 24 * 60 * 60 * 1000L
                                banearUsuario(comentario.userId, "1 semana", System.currentTimeMillis() + unaSemanaEnMilisegundos)
                            }
                            true
                        }

                        R.id.action_Banday -> {
                            // Verificar si el usuario a banear es un administrador
                            if (comentario.userId == currentUserId) {
                                // Mostrar un Toast indicando que no se puede banear a un administrador
                                Toast.makeText(view.context, "No te puedes banear a ti mismo.", Toast.LENGTH_SHORT).show()
                            } else {
                                val unDiaEnMilisegundos = 24 * 60 * 60 * 1000L
                                banearUsuario(comentario.userId, "1 día", System.currentTimeMillis() + unDiaEnMilisegundos)
                            }
                            true
                        }

                        R.id.action_BanMin -> {
                            // Verificar si el usuario a banear es un administrador
                            if (comentario.userId == currentUserId) {
                                // Mostrar un Toast indicando que no se puede banear a un administrador
                                Toast.makeText(view.context, "No te puedes banear a ti mismo.", Toast.LENGTH_SHORT).show()
                            } else {
                                val quinceMinutosEnMilisegundos = 15 * 60 * 1000L
                                banearUsuario(comentario.userId, "15 minutos", System.currentTimeMillis() + quinceMinutosEnMilisegundos)
                            }
                            true
                        }

                        else -> false
                    }
                }

                popupMenu.show()
            }
        }
    }



    private fun borrarComentarioYRespuestas(comentario: Comentario) {
        val db = FirebaseFirestore.getInstance()

        // Primero, eliminar todas las respuestas asociadas a este comentario
        db.collection("Foros")
            .document(foroId)
            .collection("Comentarios")
            .whereEqualTo("idComentarioPadre", comentario.id)
            .get()
            .addOnSuccessListener { respuestas ->
                // Crear una lista de tareas para eliminar todas las respuestas
                val tareas = mutableListOf<Task<Void>>()

                for (respuesta in respuestas) {
                    val respuestaId = respuesta.id
                    val tarea = db.collection("Foros")
                        .document(foroId)
                        .collection("Comentarios")
                        .document(respuestaId)
                        .delete()  // Eliminar cada respuesta
                    tareas.add(tarea)
                }

                // Esperar a que todas las tareas de eliminación se completen
                Tasks.whenAllComplete(tareas)
                    .addOnSuccessListener {
                        // Ahora eliminar el comentario principal
                        db.collection("Foros")
                            .document(foroId)
                            .collection("Comentarios")
                            .document(comentario.id)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("ComentariosAdapter", "Comentario y respuestas eliminados exitosamente.")
                                // Aquí puedes realizar alguna acción adicional, como actualizar la UI
                            }
                            .addOnFailureListener { e ->
                                FirebaseCrashlytics.getInstance().recordException(e)
                            }
                    }
                    .addOnFailureListener { e ->
                        FirebaseCrashlytics.getInstance().recordException(e)
                    }
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
            }
    }

    private fun banearUsuario(userId: String, tipoBaneo: String, fechaExpiracion: Long) {
        val db = FirebaseFirestore.getInstance()


        // Datos del baneo
        val banData = mapOf(
            "tipoBaneo" to tipoBaneo,
            "fechaExpiracion" to fechaExpiracion,
            "estaBaneado" to true
        )

        // Almacena la información del baneo en la subcolección 'baneados' del foro específico
        if (foroId != null) {
            db.collection("Foros").document(foroId).collection("baneados").document(userId)
                .set(banData)  // Usa set() para crear o sobrescribir el documento
                .addOnSuccessListener {
                    Log.d("Ban", "Usuario $userId baneado en el foro $foroId con éxito.")
                }
                .addOnFailureListener { e ->
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
        }
    }



    private fun getUserRole(callback: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Foros")
            .document(foroId)  // Asegúrate de que este ID de foro sea el correcto
            .collection("Roles")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val rol = document.getString("rol") ?: "Usuario" // Valor por defecto si no se encuentra
                    callback(rol)  // Llama al callback con el rol obtenido
                } else {
                    callback("Usuario") // Valor por defecto si el documento no existe
                }
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                callback("Usuario") // Valor por defecto en caso de error
            }
    }


    // Método para banear al usuario (implementa la lógica según necesites)
    private fun banUser(userId: String, duration: String) {
        // Implementa aquí la lógica de baneo
        Log.d("ComentariosAdapter", "Baneando al usuario $userId por $duration.")
        // Aquí puedes añadir la lógica para almacenar el baneo en Firestore
    }


    override fun getItemCount(): Int = listaComentarios.size
}



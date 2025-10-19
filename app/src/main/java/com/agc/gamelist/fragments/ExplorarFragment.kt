package com.agc.gamelist.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.agc.gamelist.R

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.widget.SearchView
import com.agc.gamelist.activity.ComentariosForoActivity
import com.agc.gamelist.adapters.Foro
import com.agc.gamelist.adapters.ForosAdapter
import com.agc.gamelist.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

class ExplorarFragment : Fragment() {

    private lateinit var forosAdapter: ForosAdapter
    private var listaForos: MutableList<Foro> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_explorar, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_foros)
        val searchView = view.findViewById<SearchView>(R.id.search_view)

        // Configurar el RecyclerView
        forosAdapter = ForosAdapter(listaForos) { foro ->
            // Acción al hacer clic en un foro
            abrirComentariosForo(foro)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = forosAdapter

        // Cargar los foros desde Firebase Firestore
        cargarForos()

        // Configurar la búsqueda
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarForos(newText)
                return true
            }
        })

        return view
    }
// Cargar los foros desde Firebase Firestore
    private fun cargarForos() {
        // Conectar con Firebase Firestore y obtener la lista de foros
        val db = FirebaseFirestore.getInstance()

        db.collection("Foros")
            .get()
            .addOnSuccessListener { result ->
                listaForos.clear()
                for (document in result) {
                    val foro = document.toObject(Foro::class.java).copy(id = document.id)
                    listaForos.add(foro)
                }
                forosAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e) // Registrar la excepción en Crashlytics
            }
    }

    override fun onResume() {
        super.onResume()
        // Recargar los foros al volver al fragmento
        cargarForos()

        // Reconfigurar el adaptador en caso de que se haya cambiado
        forosAdapter = ForosAdapter(listaForos) { foro ->
            // Acción al hacer clic en un foro
            abrirComentariosForo(foro)
        }

        // Configurar el RecyclerView con el adaptador
        view?.findViewById<RecyclerView>(R.id.recycler_view_foros)?.adapter = forosAdapter
    }
// Filtrar los foros según la consulta
    private fun filtrarForos(query: String?) {
        val forosFiltrados = if (query.isNullOrEmpty()) {
            listaForos
        } else {
            listaForos.filter { foro ->
                foro.titulo.contains(query, ignoreCase = true) ||
                        foro.descripcion.contains(query, ignoreCase = true)
            }
        }
        // Actualizamos el adaptador con los foros filtrados
        forosAdapter = ForosAdapter(forosFiltrados) { foro ->
            abrirComentariosForo(foro)
        }
        view?.findViewById<RecyclerView>(R.id.recycler_view_foros)?.adapter = forosAdapter
    }
// Abrir los comentarios del foro
    private fun abrirComentariosForo(foro: Foro) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email

        // Verificar si el usuario ya pertenece al foro
        val forosRef = FirebaseFirestore.getInstance().collection("Usuarios").document(userEmail!!)
        forosRef.get().addOnSuccessListener { userDocument ->
            if (userDocument.exists()) {
                val forosList = userDocument.get("foros") as? List<String> ?: emptyList()
                if (forosList.contains(foro.id)) {
                    // Si el usuario ya pertenece al foro, abre los comentarios
                    abrirComentariosActivity(foro.id)
                } else {
                    // Si no pertenece, pregunta si desea unirse
                    mostrarDialogoUnirse(foro)
                }
            }
        }.addOnFailureListener { e ->
            FirebaseCrashlytics.getInstance().recordException(e) // Registrar la excepción en Crashlytics
        }
    }
// Abrir la actividad de comentarios
    private fun abrirComentariosActivity(foroId: String) {
        getCurrentUserId()?.let {
            verificarSiBaneado(foroId, getCurrentUserId()!!) { baneado, tiempoRestante ->
                if (baneado) {
                    // El usuario está baneado
                    val tiempoRestanteSegundos = tiempoRestante ?: 0L // Asegurarse de que no sea nulo
                    val dias = tiempoRestanteSegundos / (1000 * 60 * 60 * 24)
                    val horas = (tiempoRestanteSegundos / (1000 * 60 * 60)) % 24
                    val minutos = (tiempoRestanteSegundos / (1000 * 60)) % 60
                    val segundos = (tiempoRestanteSegundos / 1000) % 60

                    // Formato del mensaje
                    val mensaje = String.format("Baneado. Tiempo restante: %d días, %02d horas, %02d minutos, %02d segundos.", dias, horas, minutos, segundos)

                    Toast.makeText(activity, mensaje, Toast.LENGTH_LONG).show()
                } else {
                    val intent = Intent(activity, ComentariosForoActivity::class.java)
                    intent.putExtra("foroId", foroId)
                    startActivity(intent)

                }
            }

        }
    }

    private fun mostrarDialogoUnirse(foro: Foro) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Unirse al foro")
        builder.setMessage("¿Deseas unirte al foro '${foro.titulo}'?")
        builder.setPositiveButton("Sí") { _, _ -> unirseAlForo(foro.id) }
        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun unirseAlForo(foroId: String) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        val userId = FirebaseAuth.getInstance().currentUser?.uid // Obtiene el ID del usuario
        val forosRef = FirebaseFirestore.getInstance().collection("Usuarios").document(userEmail!!)

        // Añadir el ID del foro a la lista de foros del usuario
        forosRef.update("foros", FieldValue.arrayUnion(foroId))
            .addOnSuccessListener {
                // Añadir el usuario a la subcolección de usuarios del foro
                val usuariosForoRef = FirebaseFirestore.getInstance().collection("Foros").document(foroId).collection("Usuarios")
                val usuarioData = hashMapOf(
                    "email" to userEmail,
                    "userId" to userId
                )
                // Agregar el documento del usuario en la subcolección de usuarios del foro
                usuariosForoRef.document(userId!!).set(usuarioData)
                    .addOnSuccessListener {
                        abrirComentariosActivity(foroId)
                    }
                    .addOnFailureListener { e ->
                        FirebaseCrashlytics.getInstance().recordException(e) // Registrar la excepción en Crashlytics
                    }
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e) // Registrar la excepción en Crashlytics
            }
    }

    private fun verificarSiBaneado(foroId: String, userId: String, callback: (Boolean, Long?) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        // Obtener la referencia a la colección de baneados en el foro específico
        db.collection("Foros").document(foroId).collection("baneados").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Obtener la fecha de expiración y el estado de baneo
                    val fechaExpiracion = document.getLong("fechaExpiracion") ?: 0L
                    val estaBaneado = document.getBoolean("estaBaneado") ?: false

                    // Log adicional para verificar los valores
                    Log.e("VerificacionBaneo", "estaBaneado: $estaBaneado, fechaExpiracion: $fechaExpiracion, currentTime: ${System.currentTimeMillis()}")

                    // Verifica si está baneado y si la fecha de expiración no ha pasado
                    if (estaBaneado && fechaExpiracion > System.currentTimeMillis()) {
                        Log.e("VerificacionBaneo", "El usuario está baneado y aún no ha expirado.")
                        val tiempoRestante = fechaExpiracion - System.currentTimeMillis() // Calcular tiempo restante
                        callback(true, tiempoRestante) // Está baneado
                    } else {
                        Log.e("VerificacionBaneo", "El usuario NO está baneado o el baneo ha expirado.")
                        callback(false, null) // No está baneado
                    }
                } else {
                    Log.e("VerificacionBaneo", "No existe el documento, el usuario NO está baneado.")
                    callback(false, null) // No está baneado
                }
            }
            .addOnFailureListener { e ->
                Log.e("VerificacionBaneo", "Error al verificar baneo", e)
                FirebaseCrashlytics.getInstance().recordException(e) // Reporta la excepción a Crashlytics
                callback(false, null) // En caso de error, asumimos que el usuario no está baneado
            }
    }




    private fun getCurrentUserId(): String? {
        // Obtén el contexto de la actividad que contiene el fragmento
        val prefs = requireActivity().getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)

        // Recupera el email de la última sesión guardada
        val email = prefs.getString("email", null)

        // Para evitar errores si el email es nulo, devuelve null si es el caso
        return email
    }

}

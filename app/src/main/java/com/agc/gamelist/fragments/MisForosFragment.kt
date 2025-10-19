package com.agc.gamelist.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R
import com.agc.gamelist.activity.ComentariosForoActivity
import com.agc.gamelist.activity.CrearForoActivity
import com.agc.gamelist.adapters.Foro
import com.agc.gamelist.adapters.ForosAdapter
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore

class MisForosFragment : Fragment() {

    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var borrarForosReceiver: BroadcastReceiver
    private lateinit var recyclerView: RecyclerView
    private lateinit var foroAdapter: ForosAdapter
    private val forosList = mutableListOf<Foro>() // Lista para almacenar los foros
    private val forosSet = mutableSetOf<String>() // Para evitar duplicados

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mis_foros, container, false)

        // Inicializar el RecyclerView
        recyclerView = view.findViewById(R.id.recycler_mis_foros)
        recyclerView.layoutManager = LinearLayoutManager(context)

// En el adaptador
        foroAdapter = ForosAdapter(forosList) { foro ->
            val userId = getCurrentUserId() // Asumiendo que tienes una función que obtiene el ID del usuario

            userId?.let { id ->
                verificarSiBaneado(foro.id, id) { baneado, tiempoRestante ->
                    if (baneado) {
                        // El usuario está baneado
                        val tiempoRestanteSegundos = tiempoRestante ?: 0L // Asegurarse de que no sea nulo
                        val dias = tiempoRestanteSegundos / (1000 * 60 * 60 * 24)
                        val horas = (tiempoRestanteSegundos / (1000 * 60 * 60)) % 24
                        val minutos = (tiempoRestanteSegundos / (1000 * 60)) % 60
                        val segundos = (tiempoRestanteSegundos / 1000) % 60

                        // Formato del mensaje
                        val mensaje = String.format(
                            "Baneado. Tiempo restante: %d días, %02d horas, %02d minutos, %02d segundos.",
                            dias, horas, minutos, segundos
                        )

                        Toast.makeText(activity, mensaje, Toast.LENGTH_LONG).show()
                    } else {
                        val intent = Intent(activity, ComentariosForoActivity::class.java)
                        intent.putExtra("foroId", foro.id)
                        startActivity(intent)

                    }
                }
            }
        }

        recyclerView.adapter = foroAdapter

        // Obtener foros del usuario
        cargarForosDelUsuario()

        // Obtener referencia al botón
        val crearForoButton = view.findViewById<FloatingActionButton>(R.id.fab_add)

        // Establecer el listener al botón
        crearForoButton.setOnClickListener {
            val intent = Intent(activity, CrearForoActivity::class.java)
            startActivity(intent)

            // Broadcast para recargar foros
            val intentVuelta = Intent("FORO_CREADO")
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intentVuelta)
        }

        // Registro del BroadcastReceiver para foros borrados
        borrarForosReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                recargarForos() // Llama al método de recarga
                val foroIdBorrado = intent?.getStringExtra("foroId") ?: return
                eliminarForoPorId(foroIdBorrado)
            }
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(borrarForosReceiver, IntentFilter("FORO_BORRADO"))


        // Registrar el BroadcastReceiver
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                recargarForos() // Llama al método de recarga
            }
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(broadcastReceiver, IntentFilter("FORO_BORRADO"))

        return view
    }

    private fun eliminarForoPorId(foroId: String) {
        // Encontrar el foro que coincida con el ID
        val foroAEliminar = forosList.find { it.id == foroId }

        if (foroAEliminar != null) {
            forosList.remove(foroAEliminar)  // Eliminar el foro de la lista
            forosSet.remove(foroId)  // También eliminarlo del conjunto para evitar duplicados
            foroAdapter.notifyDataSetChanged()  // Notificar al adaptador que la lista ha cambiado
        }
    }

// Verificar si el usuario está baneado
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

    override fun onDestroy() {
        super.onDestroy()
        // Desregistrar broadcastReceiver solo si está inicializado
        if (::broadcastReceiver.isInitialized) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver)
        }
    }

    private fun recargarForos() {
        forosList.clear() // Limpia la lista actual
        forosSet.clear()  // Limpia el conjunto de IDs para evitar duplicados
        cargarForosDelUsuario() // Llama al método para cargar los foros del usuario
    }

    private fun cargarForosDelUsuario() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email

        if (userEmail != null) {
            val usuariosRef = FirebaseFirestore.getInstance().collection("Usuarios")
            usuariosRef.document(userEmail).get()
                .addOnSuccessListener { userDocument ->
                    if (userDocument.exists()) {
                        val forosIds = userDocument.get("foros") as? List<String> ?: emptyList()
                        Log.d("MisForosFragment", "Foros del usuario: $forosIds") // Log de foros obtenidos

                        // Aquí, cargamos los foros según los IDs
                        if (forosIds.isEmpty()) {
                            // Si no hay foros, notifica al adaptador
                            foroAdapter.notifyDataSetChanged() // Para que se refresque
                        } else {
                            forosIds.forEach { foroId ->
                                cargarForoPorId(foroId) // Llama a cargar el foro por su ID
                            }
                        }
                    } else {

                    }
                }
                .addOnFailureListener { e ->
                    FirebaseCrashlytics.getInstance().recordException(e) // Reporta la excepción a Crashlytics
                    Log.e("MisForosFragment", "Error al obtener datos del usuario: ${e.message}")
                }
        }
    }

    private fun cargarForoPorId(foroId: String) {
        val forosRef = FirebaseFirestore.getInstance().collection("Foros")
        forosRef.document(foroId).get()
            .addOnSuccessListener { foroDocument ->
                if (foroDocument.exists()) {
                    val titulo = foroDocument.getString("titulo") ?: ""
                    val descripcion = foroDocument.getString("descripcion") ?: ""
                    Log.d("MisForosFragment", "Cargando foro: ID: $foroId, Titulo: $titulo, Descripcion: $descripcion")

                    if (!forosSet.contains(foroId)) {
                        forosList.add(Foro(foroId, titulo, descripcion))
                        forosSet.add(foroId) // Evitar duplicados
                        foroAdapter.notifyDataSetChanged() // Notificar el adaptador
                    }
                } else {
                    Log.d("MisForosFragment", "Foro con ID $foroId no encontrado")
                }
            }
            .addOnFailureListener { e ->

                FirebaseCrashlytics.getInstance().recordException(e) // Reporta la excepción a Crashlytics
                Log.e("MisForosFragment", "Error al cargar foro: ${e.message}")
            }
    }

    override fun onResume() {
        super.onResume()
        // Recargar foros al volver al fragmento
        recargarForos()

        foroAdapter = ForosAdapter(forosList) { foro ->
            val userId = getCurrentUserId() // Asumiendo que tienes una función que obtiene el ID del usuario

            userId?.let { id ->
                verificarSiBaneado(foro.id, id) { baneado, tiempoRestante ->
                    if (baneado) {
                        // El usuario está baneado
                        val tiempoRestanteSegundos = tiempoRestante ?: 0L // Asegurarse de que no sea nulo
                        val dias = tiempoRestanteSegundos / (1000 * 60 * 60 * 24)
                        val horas = (tiempoRestanteSegundos / (1000 * 60 * 60)) % 24
                        val minutos = (tiempoRestanteSegundos / (1000 * 60)) % 60
                        val segundos = (tiempoRestanteSegundos / 1000) % 60

                        // Formato del mensaje
                        val mensaje = String.format(
                            "Baneado. Tiempo restante: %d días, %02d horas, %02d minutos, %02d segundos.",
                            dias, horas, minutos, segundos
                        )

                        Toast.makeText(activity, mensaje, Toast.LENGTH_LONG).show()
                    } else {
                        val intent = Intent(activity, ComentariosForoActivity::class.java)
                        intent.putExtra("foroId", foro.id)
                        startActivity(intent)

                    }
                }
            }
        }
        recyclerView.adapter = foroAdapter

        // Obtener foros del usuario
        cargarForosDelUsuario()
    }



}



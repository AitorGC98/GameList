package com.agc.gamelist.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R
import com.agc.gamelist.adapters.BaneadoAdapter
import com.agc.gamelist.adapters.UsuarioAdapter
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore

class ListaUsuariosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewBaneados: RecyclerView
    private lateinit var usuarioAdapter: UsuarioAdapter
    private lateinit var baneadoAdapter: BaneadoAdapter

    private val usuarios = mutableListOf<Map<String, String>>()
    private val baneados = mutableListOf<Map<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_usuarios)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatus) // Color oscuro
        recyclerView = findViewById(R.id.recyclerViewUsuarios)
        recyclerViewBaneados = findViewById(R.id.recyclerViewBaneados)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerViewBaneados.layoutManager = LinearLayoutManager(this)

        // Referencia al LinearLayout
        val container: LinearLayout = findViewById(R.id.main)

        // Detectar el modo actual: claro u oscuro
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK

        // Cambiar el fondo según el modo
        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // Modo oscuro: fondo negro
            container.setBackgroundResource(R.color.black2)  // Fondo oscuro (debes definir este color en colors.xml)
        } else {
            // Modo claro: fondo blanco
            container.setBackgroundResource(R.drawable.background_cuadrado)  // Fondo claro (debes definir este drawable en res/drawable)
        }

        usuarioAdapter = UsuarioAdapter(usuarios) { usuario, view ->
            mostrarMenuOpciones(view, usuario)
        }

        baneadoAdapter = BaneadoAdapter(baneados) { email ->
            desbanearUsuario(email) // Llamar al método para desbanear
        }

        recyclerView.adapter = usuarioAdapter
        recyclerViewBaneados.adapter = baneadoAdapter

        cargarUsuarios()
        cargarUsuariosBaneados() // Cargar inicialmente los baneados
    }

    private fun desbanearUsuario(email: String) {
        val foroId = intent.getStringExtra("FORO_ID") ?: return
        val baneadosRef = FirebaseFirestore.getInstance().collection("Foros")
            .document(foroId).collection("baneados")

        baneadosRef.document(email).delete()
            .addOnSuccessListener {

                Toast.makeText(this, "Usuario $email desbaneado.", Toast.LENGTH_SHORT).show()
                cargarUsuariosBaneados() // Recargar la lista de baneados
            }
            .addOnFailureListener { e ->
                Log.e("Desbanear", "Error al desbanear usuario $email: ${e.message}")
                FirebaseCrashlytics.getInstance().recordException(e)
            }
    }

    private fun cargarUsuariosBaneados() {
        val foroId = intent.getStringExtra("FORO_ID") ?: return
        val baneadosRef = FirebaseFirestore.getInstance().collection("Foros")
            .document(foroId).collection("baneados")

        // Usar SnapshotListener para escuchar cambios en los baneados
        baneadosRef.addSnapshotListener { querySnapshot, e ->
            if (e != null) {
                FirebaseCrashlytics.getInstance().recordException(e)
                return@addSnapshotListener
            }

            if (querySnapshot != null) {
                baneados.clear() // Limpiar la lista antes de llenarla con nuevos datos
                Log.d("CargaBaneados", "Cantidad de documentos recuperados: ${querySnapshot.size()}")

                for (document in querySnapshot.documents) {
                    val banData = document.data as? Map<String, Any> ?: continue
                    val fechaExpiracion = (banData["fechaExpiracion"] as? Long) ?: Long.MAX_VALUE

                    if (System.currentTimeMillis() < fechaExpiracion) {
                        // Añade el campo "email" a los datos usando el ID del documento
                        val banDataWithId = banData.toMutableMap()
                        banDataWithId["email"] = document.id // Usa el ID como "email"
                        baneados.add(banDataWithId as Map<String, String>)

                        Log.d("CargaBaneados", "Documento recuperado y añadido: $banDataWithId")
                    } else {
                        baneadosRef.document(document.id).delete()
                    }
                }
                baneadoAdapter.notifyDataSetChanged()
                Log.d("CargaBaneados", "Usuarios baneados cargados y adaptador notificado.")
            }
        }
    }

    private fun cargarUsuarios() {
        val foroId = intent.getStringExtra("FORO_ID") ?: return
        val usuariosForoRef = FirebaseFirestore.getInstance().collection("Foros")
            .document(foroId).collection("Usuarios")

        // Usar SnapshotListener para escuchar cambios
        usuariosForoRef.addSnapshotListener { querySnapshot, e ->
            if (e != null) {
                FirebaseCrashlytics.getInstance().recordException(e)
                return@addSnapshotListener
            }

            if (querySnapshot != null) {
                usuarios.clear() // Limpiar la lista antes de llenarla con nuevos datos
                for (document in querySnapshot.documents) {
                    val usuario = document.data as? Map<String, String>
                    if (usuario != null) {
                        usuarios.add(usuario)
                    }
                }
                usuarioAdapter.notifyDataSetChanged() // Notificar al adaptador sobre los cambios
            }
        }
    }

    private fun mostrarMenuOpciones(view: View, usuario: Map<String, String>) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.menu_usuario_opciones, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->

            // Obtén el ID y el correo del usuario seleccionado
            val userIdParaBanear = usuario["userId"]
            val correoUsuario = usuario["email"] // Obtener el correo electrónico


            when (menuItem.itemId) {
                R.id.opcionVerPerfil -> {
                    verPerfil(correoUsuario)
                    true
                }

                R.id.opcionPermaBan -> {
                    correoUsuario?.let {
                        banearUsuario(it, "permanente", Long.MAX_VALUE)

                    }
                    true
                }
                R.id.opcionBanearsemana -> {
                    val unaSemanaEnMilisegundos = 7 * 24 * 60 * 60 * 1000L
                    correoUsuario?.let {
                        banearUsuario(it, "1 semana", System.currentTimeMillis() + unaSemanaEnMilisegundos)

                    }
                    true
                }
                R.id.opcionBaneardia -> {
                    val unDiaEnMilisegundos = 24 * 60 * 60 * 1000L
                    correoUsuario?.let {
                        banearUsuario(it, "1 día", System.currentTimeMillis() + unDiaEnMilisegundos)

                    }
                    true
                }
                R.id.opcionBanearmin -> {
                    val quinceMinutosEnMilisegundos = 15 * 60 * 1000L
                    correoUsuario?.let {
                        banearUsuario(it, "15 minutos", System.currentTimeMillis() + quinceMinutosEnMilisegundos)

                    }
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    // Método para abrir la actividad de perfil del usuario
    private fun verPerfil(userId: String?) {
        val intent = Intent(this, PerfilUsuarioActivity::class.java)

        // Pasa el correo del usuario a la actividad
        intent.putExtra("usuario_email", userId) // Asegúrate de que el objeto comentario tenga el correo del usuario.

        // Inicia la actividad
        startActivity(intent)
    }

    // Método para banear a un usuario
    private fun banearUsuario(userId: String, tipoBaneo: String, fechaExpiracion: Long) {
        val db = FirebaseFirestore.getInstance()
        val foroId = intent.getStringExtra("FORO_ID")

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
                    cargarUsuariosBaneados() // Recargar la lista de baneados después de banear
                }
                .addOnFailureListener { e ->
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
        }
    }
}

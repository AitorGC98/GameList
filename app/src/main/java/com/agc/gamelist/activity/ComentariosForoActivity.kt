package com.agc.gamelist.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R
import com.google.firebase.firestore.FirebaseFirestore


import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.agc.gamelist.adapters.ComentariosAdapter
import com.agc.gamelist.model.Usuario
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

class ComentariosForoActivity : AppCompatActivity() {

    private lateinit var comentariosAdapter: ComentariosAdapter
    private var listaComentarios: MutableList<Comentario> = mutableListOf()
    private lateinit var foroId: String

    // Modificar el método onCreate en ComentariosForoActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comentarios_foro)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatus) // Color oscuro
        // Cambiar el color del ActionBar
        val colorDeseado = ContextCompat.getColor(this, R.color.colorMorado) // Asegúrate de tener un color definido en res/values/colors.xml
        supportActionBar?.setBackgroundDrawable(ColorDrawable(colorDeseado))
        // Habilitar la flecha de "volver atrás"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        foroId = intent.getStringExtra("foroId") ?: return

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_comentarios)
        // Obtener el ID del usuario actual
        val currentUserId = getCurrentUserId()
        // Pasar el currentUserId al adaptador
        comentariosAdapter = ComentariosAdapter(foroId, listaComentarios,
            { comentario -> mostrarDialogoRespuesta(comentario) },
            { comentario -> cargarRespuestas(comentario) },
            { comentario -> mostrarDialogoBorrado(comentario) },
            currentUserId // Agregar el ID del usuario actual aquí

        )

        // Referencia al LinearLayout
        val container: ConstraintLayout = findViewById(R.id.mainLayout)

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


        // Configurar el RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = comentariosAdapter

        // Cargar los comentarios del foro
        cargarComentarios()
        iniciarListenerBaneos()
        iniciarListenerComentarios()

        // Configurar el botón para enviar un comentario
        val buttonEnviarComentario = findViewById<ImageButton>(R.id.button_enviar_comentario)
        val editTextComentario = findViewById<EditText>(R.id.edit_text_comentario)

        // Listener del boton para enviar un comentario
        buttonEnviarComentario.setOnClickListener {
            val textoComentario = editTextComentario.text.toString().trim()
            if (textoComentario.isNotEmpty()) {
                enviarComentario(textoComentario)
                editTextComentario.text.clear() // Limpiar el campo de texto después de enviar
            } else {
                Toast.makeText(this, "Por favor, escribe un comentario", Toast.LENGTH_SHORT).show()
            }
        }
        cargarComentarioFijado()
    }

// Función encargada de cargar los comentarios del foro
    private fun iniciarListenerRespuestas(comentario: Comentario) {
        val db = FirebaseFirestore.getInstance()
        val respuestasRef = db.collection("Foros").document(foroId).collection("Comentarios")
            .whereEqualTo("idComentarioPadre", comentario.id) // Filtrar por el ID del comentario padre

    // Escuchar cambios en las respuestas
        respuestasRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("ComentariosForoActivity", "Error al escuchar cambios en respuestas.", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Limpiar las respuestas previas
                comentario.respuestas.clear()

                for (document in snapshot.documents) {
                    val respuesta = document.toObject(Comentario::class.java)
                    respuesta?.id = document.id // Asignar el ID del documento Firestore al campo id
                    comentario.respuestas.add(respuesta!!)
                }

                // Ordenar las respuestas por fecha
                comentario.respuestas.sortBy { it.fecha }


                // Notificar al adaptador que las respuestas han cambiado
                comentariosAdapter.notifyDataSetChanged()



                Log.d("ComentariosForoActivity", "Respuestas actualizadas para el comentario ID: ${comentario.id}")
            }
        }
    }

// Función encargada de copmenzar el listener de los cambios del foro
    private fun iniciarListenerComentarios() {
        val db = FirebaseFirestore.getInstance()
        val comentariosRef = db.collection("Foros").document(foroId).collection("Comentarios")

        // Escuchar cambios en los comentarios
        comentariosRef.whereEqualTo("idComentarioPadre", null) // Solo comentarios principales
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ComentariosForoActivity", "Error al escuchar cambios en comentarios.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val nuevosComentarios = mutableListOf<Comentario>()

                    // Limpiar la lista de comentarios actual
                    listaComentarios.clear()

                    for (document in snapshot.documents) {
                        val comentario = document.toObject(Comentario::class.java)
                        comentario?.id = document.id  // Asignar el ID del documento Firestore al campo id
                        nuevosComentarios.add(comentario!!)
                    }

                    // Actualizar la lista de comentarios y ordenar
                    listaComentarios.addAll(nuevosComentarios)
                    listaComentarios.sortBy { it.fecha }

                    // Notificar al adaptador que los datos han cambiado
                    comentariosAdapter.notifyDataSetChanged()


                    // Log para verificar cuántos comentarios se han cargado
                    Log.d("ComentariosForoActivity", "Comentarios actualizados: ${listaComentarios.size}")
                }
            }
    }

// Función encargada de iniciar el listener de los baneos del foro de firebase
    private fun iniciarListenerBaneos() {
        val foroId = intent.getStringExtra("foroId")
        val userId = getCurrentUserId() // Obtén el ID del usuario actual

        if (foroId != null) {
            val baneadosRef = FirebaseFirestore.getInstance().collection("Foros")
                .document(foroId).collection("baneados")

            baneadosRef.document(userId).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ForoActivity", "Error al obtener baneos.", e)
                    return@addSnapshotListener
                }

                // Verificar si el documento existe
                if (snapshot != null && snapshot.exists()) {
                    // El usuario está baneado
                    val fechaExpiracion = snapshot.getLong("fechaExpiracion") ?: 0L
                    val estaBaneado = snapshot.getBoolean("estaBaneado") ?: false

                    // Verifica si el baneo ha expirado
                    if (estaBaneado && fechaExpiracion > System.currentTimeMillis()) {
                        Log.w("ForoActivity", "El usuario está baneado.")
                        finish() // Cierra la actividad si está baneado
                    } else {
                        // El baneo ha expirado o el usuario no está baneado
                        Log.d("ForoActivity", "El baneo ha expirado o el usuario no está baneado.")
                    }
                } else {
                    // El documento no existe, el usuario no está baneado
                    Log.d("ForoActivity", "No existe el documento, el usuario NO está baneado.")
                }
            }
        }
    }





// Función encargada de inflar el menú
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_comentarios_foro, menu)

        // Obtener el email del usuario actual
        val userEmail = getCurrentUserId()

        // Verificar si el usuario es administrador del foro
        val rolesRef = FirebaseFirestore.getInstance().collection("Foros").document(foroId).collection("Roles")
        rolesRef.document(userEmail).get().addOnSuccessListener { document ->
            if (document.exists() && document.getString("rol") == "Administrador") {
                // Mostrar opciones solo para administradores
                menu?.findItem(R.id.action_borrar_foro)?.isVisible = true
                menu?.findItem(R.id.action_ver_usuarios)?.isVisible = true
                // Ocultar la opción "Salir del foro" para administradores
                menu?.findItem(R.id.action_salir_foro)?.isVisible = false
                menu?.findItem(R.id.action_fijado)?.isVisible = true
            } else {
                // Mostrar la opción "Salir del foro" para usuarios normales
                menu?.findItem(R.id.action_salir_foro)?.isVisible = true
            }
        }

        return true
    }



// Función encargada de inflar el menú de seleeción
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Al hacer clic en la flecha, finaliza la actividad
                finish()
                true
            }
            R.id.action_borrar_foro -> {
                // Lógica para borrar el foro
                borrarForo(foroId)
                true
            }
            R.id.action_ver_usuarios -> {
                // Lógica para ver los usuarios del foro
                val intent = Intent(this, ListaUsuariosActivity::class.java)
                intent.putExtra("FORO_ID", foroId)
                startActivity(intent)

                true
            }
            R.id.action_salir_foro -> {
                // Lógica para salir del foro
                salirDelForo(foroId)
                true
            }
            R.id.action_fijado -> {
                // Lógica para salir del foro
                mostrarDialogo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Función encargada de cargar el comentario fijado
    private fun cargarComentarioFijado() {
        val db = FirebaseFirestore.getInstance()
        val textViewMensaje = findViewById<TextView>(R.id.text_view_mensaje)
        val relative=findViewById<RelativeLayout>(R.id.layout_mensaje_fijado)

        // Obtener el comentario fijado
        db.collection("Foros")
            .document(foroId)
            .collection("ComentariosFijados")
            .limit(1)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result?.documents?.firstOrNull()
                    if (document != null) {
                        val mensaje = document.getString("mensaje")
                        val expiracion = document.getLong("expiracion") ?: 0L
                        val tiempoActual = System.currentTimeMillis()

                        Log.d("ComentariosForoActivity", "Mensaje: $mensaje, Expiración: $expiracion, Tiempo Actual: $tiempoActual")
                        Log.d("ComentariosForoActivity", "Comparando tiempo actual y expiración: Actual: $tiempoActual, Expiración: $expiracion")

                        // Verificar si el comentario ha expirado
                        if (tiempoActual < expiracion) {
                            textViewMensaje.text = mensaje
                            textViewMensaje.movementMethod = LinkMovementMethod.getInstance() // Habilitar clics en los enlaces

                            // Hacer invisible el TextView si el mensaje está vacío
                            textViewMensaje.visibility= if (mensaje.isNullOrEmpty()) View.GONE else View.VISIBLE
                            relative.visibility= if (mensaje.isNullOrEmpty()) View.GONE else View.VISIBLE

                            Log.d("ComentariosForoActivity", "El comentario se muestra.")
                        } else {
                            Log.d("ComentariosForoActivity", "El comentario ha expirado. Procediendo a eliminar...")
                            eliminarComentario(document.id)
                        }
                    } else {
                        textViewMensaje.visibility=View.GONE
                        relative.visibility=  View.GONE

                        Log.d("ComentariosForoActivity", "No hay comentario fijado.")
                    }
                } else {
                    textViewMensaje.visibility = View.GONE // Ocultar si hay un error
                    Log.d("Firebase", "Error al obtener el comentario: ", task.exception)
                }
            }
    }


// Función encargada de eliminar el comentario
    private fun eliminarComentario(documentId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Foros")
            .document(foroId)
            .collection("ComentariosFijados")
            .document(documentId) // Identificador del documento
            .delete()
            .addOnSuccessListener {
                Log.d("ComentariosForoActivity", "Comentario eliminado por expiración.")
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)

            }
    }

    // Función encargada de fijar el comentario
    private fun fijarComentario(mensaje: String) {
        val db = FirebaseFirestore.getInstance()
        // Primero, eliminar cualquier comentario fijado existente
        db.collection("Foros")
            .document(foroId) // Cambia esto al ID correcto de tu foro
            .collection("ComentariosFijados") // Nueva colección
            .limit(1)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result?.documents?.firstOrNull()
                    if (document != null) {
                        eliminarComentario(document.id) // Eliminar el comentario existente
                    }
                    // Después de eliminar, agregar el nuevo comentario
                    val comentarioFijado = hashMapOf(
                        "mensaje" to mensaje,
                        "expiracion" to System.currentTimeMillis() + (60 * 60 * 1000) // Expiración en 1 hora
                    )

                    db.collection("Foros")
                        .document(foroId)
                        .collection("ComentariosFijados")
                        .add(comentarioFijado)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Comentario fijado", Toast.LENGTH_SHORT).show()
                            cargarComentarioFijado() // Recargar el comentario fijado para actualizar la vista
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "No se ha podido fijar el comentario", Toast.LENGTH_SHORT).show()
                            FirebaseCrashlytics.getInstance().recordException(e)

                        }
                } else {
                    // Si no hay comentarios existentes, agregar el nuevo directamente
                    val comentarioFijado = hashMapOf(
                        "mensaje" to mensaje,
                        "expiracion" to System.currentTimeMillis() + (60 * 60 * 1000) // Expiración en 1 hora
                    )

                    db.collection("Foros")
                        .document(foroId)
                        .collection("ComentariosFijados")
                        .add(comentarioFijado)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Comentario fijado correctamente", Toast.LENGTH_SHORT).show()
                            cargarComentarioFijado() // Recargar el comentario fijado para actualizar la vista
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "No se ha podido fijar el comentario", Toast.LENGTH_SHORT).show()
                            FirebaseCrashlytics.getInstance().recordException(e)

                        }
                }
            }
    }

    // Función encargada de mostrar el diálogo
    private fun mostrarDialogo() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Fijar Comentario")
        val textViewMensaje = findViewById<TextView>(R.id.text_view_mensaje)
        val dialogView = layoutInflater.inflate(R.layout.dialogo_mensaje, null)
        builder.setView(dialogView)

        val editTextMensaje: EditText = dialogView.findViewById(R.id.edit_text_mensaje)
        val buttonGuardar: Button = dialogView.findViewById(R.id.button_guardar)

        // Establecer un límite de caracteres de 400
        editTextMensaje.filters = arrayOf(InputFilter.LengthFilter(400))

        // Crear el diálogo
        val dialog = builder.create()

        buttonGuardar.setOnClickListener {
            val mensaje = editTextMensaje.text.toString()
            // No es necesario comprobar la fecha ni la hora
            fijarComentario(mensaje) // Guarda el mensaje en Firebase
            dialog.dismiss() // Cerrar el diálogo
        }

        // Configuración del botón de cancelar
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }

        // Mostrar el diálogo
        dialog.show()
    }



// Función encargada de borrar el foro
    private fun borrarForo(foroId: String) {
        // Referencia a la colección de usuarios en el foro
        val usuariosForoRef = FirebaseFirestore.getInstance().collection("Foros").document(foroId).collection("Usuarios")
        val comentariosRef = FirebaseFirestore.getInstance().collection("Foros").document(foroId).collection("Comentarios")
        val rolesRef = FirebaseFirestore.getInstance().collection("Foros").document(foroId).collection("Roles")

        // Obtener el correo del usuario actual
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        // Obtener todos los usuarios que están en el foro
        usuariosForoRef.get()
            .addOnSuccessListener { userDocuments ->
                // Crear una lista para almacenar las tareas de eliminación
                val tareasDeEliminacion = mutableListOf<Task<Void>>()

                // Recorrer cada documento de usuario en la subcolección y añadir tareas de eliminación
                for (document in userDocuments) {
                    // Eliminar el documento del usuario en la subcolección
                    tareasDeEliminacion.add(document.reference.delete())
                }

                // También eliminar el foro del usuario actual
                currentUserEmail?.let { email ->
                    val currentUserRef = FirebaseFirestore.getInstance().collection("Usuarios").document(email)
                    tareasDeEliminacion.add(currentUserRef.update("foros", FieldValue.arrayRemove(foroId)))
                }

                // Esperar a que todas las tareas de eliminación se completen
                Tasks.whenAllSuccess<Void>(tareasDeEliminacion)
                    .addOnSuccessListener {
                        // Ahora eliminar los comentarios de la subcolección de comentarios
                        comentariosRef.get().addOnSuccessListener { commentDocuments ->
                            val commentTasks = commentDocuments.map { it.reference.delete() }
                            Tasks.whenAllSuccess<Void>(commentTasks)
                                .addOnSuccessListener {
                                    // Una vez eliminados los comentarios, eliminar los roles de la subcolección de roles
                                    rolesRef.get().addOnSuccessListener { roleDocuments ->
                                        val roleTasks = roleDocuments.map { it.reference.delete() }
                                        Tasks.whenAllSuccess<Void>(roleTasks)
                                            .addOnSuccessListener {
                                                // Finalmente, eliminar el foro principal
                                                val foroRef = FirebaseFirestore.getInstance().collection("Foros").document(foroId)
                                                foroRef.delete()
                                                    .addOnSuccessListener {
                                                        Toast.makeText(this, "Foro eliminado", Toast.LENGTH_SHORT).show()
                                                        val intentVuelta = Intent("FORO_BORRADO")
                                                        intentVuelta.putExtra("foroId", foroId)
                                                        LocalBroadcastManager.getInstance(this).sendBroadcast(intentVuelta)
                                                        finish() // Cierra la actividad o navega a la actividad anterior
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(this, "No se ha podido borrar el foro", Toast.LENGTH_SHORT).show()
                                                        FirebaseCrashlytics.getInstance().recordException(e)
                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                FirebaseCrashlytics.getInstance().recordException(e)
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    FirebaseCrashlytics.getInstance().recordException(e)
                                }
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





// Función encargada de salir del foro
    private fun salirDelForo(foroId: String) {
        val userEmail =getCurrentUserId()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // Referencia a la subcolección de usuarios del foro
        val usuariosForoRef = FirebaseFirestore.getInstance().collection("Foros").document(foroId).collection("Usuarios")

        // Eliminar el usuario de la subcolección de usuarios del foro
        usuariosForoRef.document(userId!!).delete()
            .addOnSuccessListener {
                // Eliminar el ID del foro de la lista de foros del usuario
                val forosRef = FirebaseFirestore.getInstance().collection("Usuarios").document(userEmail!!)
                forosRef.update("foros", FieldValue.arrayRemove(foroId))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Has salido del foro", Toast.LENGTH_SHORT).show()
                        finish() // Cierra la actividad o navega a la actividad anterior
                    }
                    .addOnFailureListener { e ->
                        FirebaseCrashlytics.getInstance().recordException(e)
                        Toast.makeText(this, "No se ha podido salir del foro, intentalo de nuevo", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se ha podido salir del foro, intentalo de nuevo", Toast.LENGTH_SHORT).show()
            }
    }

//// Función encargada de cargar los comentarios
    private fun cargarComentarios() {
        val db = FirebaseFirestore.getInstance()

        db.collection("Foros").document(foroId).collection("Comentarios")
            .whereEqualTo("idComentarioPadre", null)  // Cargar solo los comentarios principales
            .get()
            .addOnSuccessListener { result ->
                // Crear un mapa temporal para almacenar los comentarios existentes y sus respuestas
                val comentariosTemp = mutableMapOf<String, Comentario>()

                // Agregar comentarios existentes a comentariosTemp
                for (comentario in listaComentarios) {
                    comentariosTemp[comentario.id] = comentario
                }

                // Limpiar la lista de comentarios y recargar
                listaComentarios.clear()
                for (document in result) {
                    val comentario = document.toObject(Comentario::class.java)
                    comentario.id = document.id  // Asignar el ID del documento Firestore al campo id

                    // Si hay respuestas ya cargadas en comentariosTemp, asignarlas al nuevo comentario
                    comentariosTemp[comentario.id]?.let {
                        comentario.respuestas = it.respuestas.toMutableList() // Mantener las respuestas
                    }

                    listaComentarios.add(comentario)
                }

                // Log para verificar cuántos comentarios se han cargado
                Log.d("ComentariosForoActivity", "Comentarios cargados: ${listaComentarios.size}")

                // Ordenar los comentarios por fecha
                listaComentarios.sortBy { it.fecha }

                comentariosAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se han podido cargar los comentarios", Toast.LENGTH_SHORT).show()
            }
    }




/// Función encargada de cargar las respuestas
    private fun cargarRespuestas(comentario: Comentario) {
        val db = FirebaseFirestore.getInstance()

        Log.d("ComentariosForoActivity", "Intentando cargar respuestas para el comentario ID: ${comentario.id}")

        db.collection("Foros").document(foroId).collection("Comentarios")
            .whereEqualTo("idComentarioPadre", comentario.id)  // Obtener respuestas de este comentario
            .get()
            .addOnSuccessListener { result ->
                comentario.respuestas.clear() // Limpiar respuestas previas
                Log.d("ComentariosForoActivity", "Respuestas encontradas: ${result.size()}")

                for (document in result) {
                    val respuesta = document.toObject(Comentario::class.java)
                    respuesta.id = document.id  // Asignar el ID del documento Firestore al campo id
                    comentario.respuestas.add(respuesta)
                    Log.d("ComentariosForoActivity", "Respuesta cargada: ${respuesta.id}, Texto: ${respuesta.texto}")
                }

                // Ordenar las respuestas por fecha
                comentario.respuestas.sortBy { it.fecha }
                iniciarListenerRespuestas(comentario)
                // Notificar al adaptador que las respuestas han cambiado
                comentariosAdapter.notifyDataSetChanged() // Esto podría ser el problema, no se está actualizando la vista.
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se han podido cargar algunos comentarios", Toast.LENGTH_SHORT).show()
            }
    }

/// Función encargada de ganar experiencia
    private fun ganarExperiencia(cantidad: Int) {
        val usuarioId =getCurrentUserId()// Reemplaza con el ID real del usuario
        val db = FirebaseFirestore.getInstance()
        // Obtener la experiencia actual del usuario de Firestore
        db.collection("Usuarios").document(usuarioId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Inicializar el usuario desde el documento
                    val usuario = document.toObject(Usuario::class.java) ?: Usuario()

                    Log.d("HomeActivity", "Usuario actual: $usuario")

                    // Inicializa los campos si no existen
                    var experienciaActual = document.getLong("experienciaActual")?.toInt() ?: 0
                    var nivel = document.getLong("nivel")?.toInt() ?: 1
                    var experienciaNecesaria = document.getLong("experienciaNecesaria")?.toInt() ?: calcularNuevaExperienciaNecesaria(nivel)

                    // Aumentar la experiencia actual
                    experienciaActual += cantidad

                    // Verificar si el usuario ha subido de nivel
                    while (experienciaActual >= experienciaNecesaria) {
                        nivel++
                        experienciaActual -= experienciaNecesaria
                        experienciaNecesaria = calcularNuevaExperienciaNecesaria(nivel)
                    }

                    // Actualizar la base de datos con los nuevos valores, solo si son diferentes
                    val updates = hashMapOf(
                        "experienciaActual" to experienciaActual,
                        "nivel" to nivel,
                        "experienciaNecesaria" to experienciaNecesaria
                    )

                    db.collection("Usuarios").document(usuarioId)
                        .set(updates, SetOptions.merge()) // Usa merge para no sobreescribir datos existentes
                        .addOnSuccessListener {
                            Log.d("HomeActivity", "Datos actualizados correctamente")

                        }
                        .addOnFailureListener { exception ->
                            FirebaseCrashlytics.getInstance().recordException(exception)
                        }
                } else {
                    Log.w("HomeActivity", "El documento no existe, no se puede ganar experiencia.")
                }
            }
            .addOnFailureListener { exception ->
                FirebaseCrashlytics.getInstance().recordException(exception)

            }
    }


    private fun calcularNuevaExperienciaNecesaria(nivel: Int): Int {
        // Lógica para calcular la nueva experiencia necesaria para el próximo nivel
        return nivel * 100 // Por ejemplo, 100, 200, 300, etc.
    }

    /// Función encargada de enviar un comentario
    private fun enviarComentario(textoComentario: String, comentarioPadre: Comentario? = null) {
        val db = FirebaseFirestore.getInstance()
        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)
        // Recupera el email y el proveedor de la última sesión guardada.
        val imagenPerfil = prefs.getString("profileImage", "") ?: ""
        val nombreUsuario = prefs.getString("userName", "Anónimo") ?: "Anónimo"

        val comentario = Comentario(
            texto = textoComentario,
            fecha = System.currentTimeMillis(),
            idComentarioPadre = comentarioPadre?.id,
            userId = getCurrentUserId(),
            nombreUsuario = nombreUsuario, // Agregar el nombre de usuario
            fotoPerfil = imagenPerfil // Agregar la foto de perfil
        )

        db.collection("Foros").document(foroId).collection("Comentarios")
            .add(comentario)
            .addOnSuccessListener {
                ganarExperiencia(30)
                if (comentarioPadre != null) {
                    // Cargar las respuestas y actualizar la vista del comentario padre
                    cargarRespuestas(comentarioPadre)
                    // Actualizar el estado del comentario padre para mantener el hilo abierto
                    comentarioPadre.respuestasVisible = true
                    val indexPadre = listaComentarios.indexOfFirst { it.id == comentarioPadre.id }
                    if (indexPadre != -1) {
                        comentariosAdapter.notifyItemChanged(indexPadre) // Actualizar solo el comentario padre
                    }
                } else {
                    // Si es un comentario principal, recargar la lista completa
                    cargarComentarios()
                }

            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se ha podido enviar el comentario", Toast.LENGTH_SHORT).show()
            }
    }




    // Mostrar un diálogo para que el usuario responda a un comentario
    private fun mostrarDialogoRespuesta(comentario: Comentario) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Responder a comentario")

        val input = EditText(this)
        input.hint = "Escribe tu respuesta..."
        builder.setView(input)

        builder.setPositiveButton("Enviar") { dialog, _ ->
            val textoRespuesta = input.text.toString().trim()
            if (textoRespuesta.isNotEmpty()) {
                enviarComentario(textoRespuesta, comentario)
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun getCurrentUserId(): String {

        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)
        // Recupera el email y el proveedor de la última sesión guardada.
        val email = prefs.getString("email", null)
        return email.toString()
        Log.d("ComentariosForoActivity",  email.toString())
    }

    private fun mostrarDialogoBorrado(comentario: Comentario) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Comentario")
            .setMessage("¿Estás seguro de que deseas eliminar este comentario?")
            .setPositiveButton("Sí") { dialog, which ->
                // Llama al método para borrar el comentario en Firestore
                eliminarComentario(comentario)
            }
            .setNegativeButton("No", null)
            .show()
    }


/// Función encargada de borrar el comentario
    private fun eliminarComentario(comentario: Comentario) {
        // Actualizar el comentario para marcarlo como borrado
        comentario.isBorrado = true
        comentario.texto = "Comentario borrado"  // Cambiar el texto

        val db = FirebaseFirestore.getInstance()

        // Actualizar el comentario en Firestore
        db.collection("Foros").document(foroId).collection("Comentarios").document(comentario.id)
            .set(comentario)  // Usamos set para sobrescribir el documento con el nuevo estado
            .addOnSuccessListener {
                Log.d("ComentariosForoActivity", "Comentario marcado como borrado con éxito")
                // Notificar al adaptador que el comentario ha cambiado
                comentariosAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)

            }
    }


}


data class Comentario(
    var id: String = "",
    var texto: String = "",
    var fecha: Long = 0L,
    var idComentarioPadre: String? = null,
    var respuestas: MutableList<Comentario> = mutableListOf(),
    var userId: String = "",
    var isBorrado: Boolean = false,
    var respuestasVisible: Boolean = false,
    var fotoPerfil: String = "", // Nombre o URL de la foto de perfil
    var nombreUsuario: String = "", // Nombre del usuario
    var numLikes: Int = 0, // Nuevo campo para contar likes
    var numDislikes: Int = 0, // Nuevo campo para contar dislikes
    var userVotes: MutableMap<String, Int> = mutableMapOf() // Mapa para almacenar votos por usuario
)






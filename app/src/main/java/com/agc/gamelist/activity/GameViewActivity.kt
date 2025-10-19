package com.agc.gamelist.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agc.gamelist.R
import com.agc.gamelist.adapters.ResennaAdapter
import com.agc.gamelist.api.RetrofitClient.retrofit
import com.agc.gamelist.databinding.ActivityGameViewBinding
import com.agc.gamelist.model.Juego
import com.agc.gamelist.model.Resenna
import com.agc.gamelist.model.Usuario
import com.agc.gamelist.network.RawgApi
import com.bumptech.glide.Glide
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Date


class GameViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameViewBinding
    private lateinit var rawgApi: RawgApi
    private var isFirstSelection = true
    private lateinit var resennaAdapter: ResennaAdapter
    private val resennasList = mutableListOf<Resenna>()
    private lateinit var db: FirebaseFirestore
    private var generoSeleccionado: String? = null
    val generosValidos = listOf("Action", "Shooter", "Adventure", "Platformer")
    private var dialog: AlertDialog? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
// Ocultar el ActionBar
        supportActionBar?.hide()
        // Desactivar la rotación de pantalla
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        var JuegoId=0
        // Usa el binding correctamente
        binding = ActivityGameViewBinding.inflate(layoutInflater)
        setContentView(binding.root)  // Cambia esto para usar el binding

        // Recuperación de las preferencias
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        // Recupera el valor guardado de 'email' y 'provider'
        val email = prefs.getString("email", null)


        // Detectar el modo actual: claro u oscuro
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK

        // Cambiar el fondo según el modo
        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // Modo oscuro: fondo negro
            binding.mainLayout.setBackgroundResource(R.color.black2)  // O cualquier color que desees para el fondo oscuro
        } else {
            // Modo claro: fondo blanco
            binding.mainLayout.setBackgroundResource(R.drawable.background_cuadrado)  // O cualquier color que desees para el fondo claro
        }

        val recyclerViewResenas = findViewById<RecyclerView>(R.id.recyclerViewResennas)
        recyclerViewResenas.layoutManager = LinearLayoutManager(this)
        resennaAdapter = ResennaAdapter(resennasList)
        recyclerViewResenas.adapter = resennaAdapter

        val spinner: Spinner = findViewById(R.id.spinner)
        val usuarioId = email ?: "email"  // Lo obtienes al loguear el usuario

        // Instancia de Firestore
         db = FirebaseFirestore.getInstance()

        // Crear una lista de opciones
        val opciones = listOf("Deseleccionado","Pendiente", "Jugando", "Terminado","Abandonado")

        // Crear un adaptador personalizado
        val adapter = object : ArrayAdapter<String>(this, R.layout.spinner_item_selected, opciones) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                // Vista seleccionada del Spinner
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item_selected, parent, false)

                // Obtener el TextView y el ImageView
                val textView = view.findViewById<TextView>(R.id.spinner_text)
                val imageView = view.findViewById<ImageView>(R.id.spinner_icon)

                // Si ningún elemento ha sido seleccionado, mostrar "Añadir a lista"
                textView.text = if (position == -1) {
                    "Añadir a lista"
                } else {
                    getItem(position)
                }

                // Mostrar la flecha solo en la vista seleccionada
                imageView.visibility = View.VISIBLE

                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                // Vista del menú desplegable
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item_dropdown, parent, false)

                // Obtener el TextView del desplegable
                val textView = view.findViewById<TextView>(R.id.spinner_text)

                // Establecer el texto del spinner
                textView.text = getItem(position)

                if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    // Cambiar el color del texto del desplegable a blanco
                    textView.setTextColor(Color.WHITE)  // Establecer el color a blanco
                }


                return view
            }
        }

        // Aplicar el adaptador al Spinner
        spinner.adapter = adapter

        // Ajustar el desplazamiento vertical del menú desplegable
        spinner.dropDownVerticalOffset = 20

        // Manejo de insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        rawgApi = retrofit.create(RawgApi::class.java)
        // Recupera los datos del Bundle
        val bundle = intent.extras
        // Asegúrate de que el Bundle no sea nulo
        bundle?.let {
            // Extraer cada uno de los datos del Bundle
            JuegoId = it.getInt("id")

            fetchGameDetails(JuegoId)
        } ?: run {
            // Manejo del caso donde no se recibió el objeto
            FirebaseCrashlytics.getInstance().log("No se ha recibido el objeto Juego")
        }

        // Verificar el estado actual del juego y seleccionarlo en el spinner si existe
        if (usuarioId.isNotEmpty()) {
            for (estado in opciones.subList(1, opciones.size)) {  // Excluye "Deseleccionado"
                db.collection("Usuarios").document(usuarioId)
                    .collection(estado).document(JuegoId.toString())
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val posicion = opciones.indexOf(estado)
                            if (posicion != -1) {
                                spinner.setSelection(posicion)
                                isFirstSelection = true
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        confirmationMessage("No se ha podido establecer conexión")
                        FirebaseCrashlytics.getInstance().recordException(e)
                    }
            }
        }



        // Detectar selección en el spinner
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val estadoSeleccionado = opciones[position]

                if (isFirstSelection) {
                    isFirstSelection = false
                    return
                }

                if (estadoSeleccionado == "Deseleccionado") {
                    // Contador para verificar cuando todas las eliminaciones hayan finalizado
                    var eliminacionesPendientes = opciones.size - 1
                    for (estado in opciones.subList(1, opciones.size)) {  // Excluye "Deseleccionado"
                        db.collection("Usuarios").document(usuarioId)
                            .collection(estado).document(JuegoId.toString())
                            .delete()
                            .addOnSuccessListener {
                                eliminacionesPendientes--
                                // Cuando todas las eliminaciones hayan finalizado
                                if (eliminacionesPendientes == 0) {
                                    confirmationMessage("Juego eliminado de la lista")
                                }

                            }
                            .addOnFailureListener { e ->
                                FirebaseCrashlytics.getInstance().recordException(e)
                                confirmationMessage("No se ha podido eliminar el juego de la lista $estado")
                            }
                    }
                } else {

                    if (estadoSeleccionado == "Terminado") {
                        Log.d(
                            "PRUEBAAAS",
                            "Juego agregado al género: $generoSeleccionado")
                        // Comprobamos si el género seleccionado es uno de los géneros válidos
                        if (generosValidos.contains(generoSeleccionado)) {
                            Log.d(
                                "PRUEBAAAS IF",
                                "Juego agregado al género: $generoSeleccionado")
                            // Guardamos el juego en la colección correspondiente a su género
                            generoSeleccionado?.let {
                                db.collection("Usuarios")
                                    .document(usuarioId)
                                    .collection(it)  // Nombre de la colección según el género
                                    .document(JuegoId.toString())
                                    .set(
                                        mapOf(
                                            "JuegoId" to JuegoId,

                                            )
                                    )
                                    .addOnSuccessListener {
                                        Log.d(
                                            "StatisticsActivity",
                                            "Juego agregado al género: $generoSeleccionado"
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        FirebaseCrashlytics.getInstance().recordException(e)
                                        confirmationMessage("No se ha podido agregar el juego al género $generoSeleccionado")
                                    }
                            }
                        }
                    }
                    // Primero eliminar el juego de todas las listas (si estaba en alguna)
                    for (estado in opciones.subList(1, opciones.size)) {  // Excluye "Deseleccionado"
                        db.collection("Usuarios").document(usuarioId)
                            .collection(estado).document(JuegoId.toString())
                            .delete()
                    }

                    // Añadir el juego a la colección correspondiente al estado seleccionado
                    val juego = Juego(JuegoId, estadoSeleccionado)
                    db.collection("Usuarios").document(usuarioId)
                        .collection(estadoSeleccionado).document(JuegoId.toString())
                        .set(juego)
                        .addOnSuccessListener {
                            ganarExperiencia(15)
                            confirmationMessage("Juego añadido a la lista $estadoSeleccionado")
                        }
                        .addOnFailureListener { e ->
                            FirebaseCrashlytics.getInstance().recordException(e)
                            confirmationMessage("No se ha podido añadir el juego a la lista $estadoSeleccionado")
                        }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Cargar reseñas desde Firestore
        cargarResennas(JuegoId)



        // Publicar una nueva reseña
        val editTextResena = findViewById<EditText>(R.id.editTextComment)
        val buttonPublicar = findViewById<ImageButton>(R.id.buttonPublishComment)
        val buttonBorrar = findViewById<ImageButton>(R.id.buttonDeleteComment)
        val textViewCaracteres = findViewById<TextView>(R.id.textViewCaracteres)

        // Establecer el máximo de caracteres
        editTextResena.filters = arrayOf(InputFilter.LengthFilter(1000)) // Establecer límite de caracteres en EditText

        // Actualizar el contador de caracteres
        editTextResena.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val caracteresRestantes = 1000 - s!!.length // Calcular caracteres restantes
                textViewCaracteres.text = "Caracteres restantes: $caracteresRestantes" // Actualizar TextView
            }

            override fun afterTextChanged(s: Editable?) {}
        })

// Código principal
        buttonPublicar.setOnClickListener {
            val comentario = editTextResena.text.toString().trim()

            if (comentario.isNotEmpty()) {
                // Verificar si existe un nombre para el usuario
                db.collection("Usuarios").document(email ?: "email")
                    .get()
                    .addOnSuccessListener { document ->
                        var nombreUsuario = "Anónimo" // Por defecto será "Anónimo"

                        if (document.exists()) {
                            // Si el documento existe, obtener el campo 'nombre'
                            nombreUsuario = document.getString("nombre") ?: "Anónimo"
                        }

                        // Crear la reseña con el nombre obtenido o "Anónimo" y asignar la fecha actual
                        val resena = Resenna(
                            userId = email ?: "email",
                            comentario = comentario,
                            nombreUsuario = nombreUsuario,
                            fecha = Date() // Fecha actual
                        )

                        // Mostrar diálogo para preguntar si recomienda el juego
                        val dialog = AlertDialog.Builder(this)
                            .setTitle("¿Recomiendas este juego?")
                            .setPositiveButton("Sí") { dialog, _ ->
                                val resenaConRecomendacion = resena.copy(recomendado = true)
                                guardarResenna(JuegoId, resenaConRecomendacion)
                                dialog.dismiss()
                            }
                            .setNegativeButton("No") { dialog, _ ->
                                val resenaConRecomendacion = resena.copy(recomendado = false)
                                guardarResenna(JuegoId, resenaConRecomendacion)
                                dialog.dismiss()
                            }
                            .create()

                        dialog.window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.fondoNews)))// Color de fondo rojo, puedes cambiarlo
                        val titleTextView = dialog.findViewById<TextView>(android.R.id.title)
                        titleTextView?.setTextColor(ContextCompat.getColor(this, R.color.colorFuente))
                        val messageTextView = dialog.findViewById<TextView>(android.R.id.message)
                        messageTextView?.setTextColor(ContextCompat.getColor(this, R.color.colorFuente))
                        dialog.show()
                        editTextResena.text.clear()
                    }
                    .addOnFailureListener { e ->
                        FirebaseCrashlytics.getInstance().recordException(e)
                        Toast.makeText(this, "Error al cargar usuario", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "La reseña no puede estar vacía", Toast.LENGTH_SHORT).show()
            }
        }

        buttonBorrar.setOnClickListener {
            val userId = email ?: "email"
            val resennaExistente = resennasList.find { it.userId == userId }

            if (resennaExistente != null) {
                borrarResenna(JuegoId, userId)
            } else {
                Toast.makeText(this, "No tienes ninguna reseña para borrar", Toast.LENGTH_SHORT).show()
            }
        }

// Llamar a verificarResenaPublicado al iniciar para configurar la visibilidad inicial
        verificarResenaPublicado(JuegoId, email ?: "email")


    }

    private fun ganarExperiencia(cantidad: Int) {
        val usuarioId =getCurrentUserId()// Reemplaza con el ID real del usuario

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


    private fun getCurrentUserId(): String {

        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)
        // Recupera el email y el proveedor de la última sesión guardada.
        val email = prefs.getString("email", null)
        return email.toString()
        Log.d("ComentariosForoActivity",  email.toString())
    }

// Función para verificar si el usuario tiene una reseña publicada
    private fun verificarResenaPublicado(juegoId: Int, userId: String) {
        // Obtener la colección de reseñas para el juego específico
        db.collection("Juegos").document(juegoId.toString())
            .collection("Resenas")
            .document(userId)  // Document ID será el userId
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Si el documento existe, significa que el usuario tiene una reseña publicada
                    ocultarBotonPublicar()
                } else {
                    // Si el documento no existe, significa que el usuario no tiene una reseña
                    ocultarBotonBorrar()
                }
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)

            }
    }

    private fun ocultarBotonPublicar() {
        // Aquí se oculta el layout que contiene el botón publicar
        val layoutPublicar = findViewById<LinearLayout>(R.id.LayoutPublicarBoton)
        val buttonDeleteComment = findViewById<LinearLayout>(R.id.LayoutBorrarBoton)
        layoutPublicar.visibility = View.GONE
        buttonDeleteComment.visibility = View.VISIBLE
    }

    private fun ocultarBotonBorrar() {
        // Aquí se oculta el botón de borrar
        val layoutPublicar = findViewById<LinearLayout>(R.id.LayoutPublicarBoton)
        val buttonDeleteComment = findViewById<LinearLayout>(R.id.LayoutBorrarBoton)
        buttonDeleteComment.visibility = View.GONE
        layoutPublicar.visibility = View.VISIBLE
    }

// Función para cargar reseñas desde Firestore
    private fun cargarResennas(juegoId: Int) {
        db.collection("Juegos").document(juegoId.toString())
            .collection("Resenas")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val resena = document.toObject(Resenna::class.java)
                    resennasList.add(resena)
                }
                resennaAdapter.notifyDataSetChanged()  // Refrescar la lista
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)

            }
    }

    // Función para guardar reseña en Firebase
    private fun guardarResenna(juegoId: Int, resena: Resenna) {
        guardarResennaEnFirebase(juegoId, resena)
        verificarResenaPublicado(juegoId, resena.userId) // Actualizar botones tras publicar
    }

    // Función para guardar reseña en Firebase y actualizar visibilidad
    private fun guardarResennaEnFirebase(juegoId: Int, resena: Resenna) {
        db.collection("Juegos").document(juegoId.toString())
            .collection("Resenas")
            .document(resena.userId)
            .set(resena)
            .addOnSuccessListener {
                Toast.makeText(this, "Reseña publicada", Toast.LENGTH_SHORT).show()
                resennasList.add(resena)
                ganarExperiencia(25)
                resennaAdapter.notifyDataSetChanged()
                verificarResenaPublicado(juegoId, resena.userId)
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se ha podido publicar la reseña", Toast.LENGTH_SHORT).show()
            }
    }

    // Función para borrar reseña y actualizar visibilidad
    private fun borrarResenna(juegoId: Int, userId: String) {
        db.collection("Juegos").document(juegoId.toString())
            .collection("Resenas")
            .document(userId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Reseña borrada", Toast.LENGTH_SHORT).show()
                resennasList.removeIf { it.userId == userId }
                resennaAdapter.notifyDataSetChanged()
                verificarResenaPublicado(juegoId, userId)
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se ha podido borrar la reseña", Toast.LENGTH_SHORT).show()
            }
    }

// Función para mostrar un diálogo con un mensaje
    private fun confirmationMessage(message: String) {
        // Solo muestra el diálogo si no está ya visible
        if (dialog == null || !dialog!!.isShowing) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("")
            builder.setMessage(message)
            builder.setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()  // Cierra el diálogo cuando el usuario pulsa "Aceptar"
            }

            // Crear y mostrar el diálogo
            dialog = builder.create()
            dialog?.setCancelable(false)  // Evita que se cierre tocando fuera del diálogo
            dialog?.show()
        }
    }

    // Función para establecer el color del rating
    private fun setGameRatingColor(metacriticScore: Int) {
        val ratingTextView = binding.gameRating
        val background = ratingTextView.background as GradientDrawable // Obtener el drawable del fondo

        val backgroundColor = when {
            metacriticScore in 70..100 -> Color.parseColor("#4CAF50") // Verde
            metacriticScore in 50..69 -> Color.parseColor("#FF9800") // Naranja
            metacriticScore in 0..49 -> Color.parseColor("#F44336") // Rojo
            else -> Color.parseColor("#CCCCCC") // Gris para calificaciones fuera de rango
        }

        // Establecer el color de fondo
        background.setColor(backgroundColor)
        ratingTextView.text = "$metacriticScore" // Mostrar el puntaje de Metacritic
    }




    // Función para obtener los detalles del juego
    private fun fetchGameDetails(gameId: Int) {
        val apiKey = getString(R.string.rawg_api_key)  // Tu API key de RAWG

        // Ejecuta la llamada a la API en un hilo de fondo
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = rawgApi.getGameDetails(gameId, apiKey)
                if (response.isSuccessful) {
                    val gameDetails = response.body()
                    gameDetails?.let { game ->
                        // Actualiza la UI en el hilo principal
                        runOnUiThread {
                            generoSeleccionado=game.genres?.firstOrNull()?.name ?: "No disponible"
                            binding.gameTitle.text = game.name
                            binding.gameDescription.text = game.description_raw ?: "Sin descripción"
                            val metacriticScore = game.metacritic ?: 0 // Valor predeterminado si es nulo
                            setGameRatingColor(metacriticScore)
                            binding.gameWebsite.text = game.website ?: "Sin sitio web"

                            // Fecha de lanzamiento en negrita
                            val releaseDate = game.released ?: "Sin fecha de lanzamiento"
                            val releaseDateText = SpannableStringBuilder()
                            releaseDateText.append("Fecha de lanzamiento: ", StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            releaseDateText.append(releaseDate)
                            binding.gameDate.text = releaseDateText

                            // Duración en negrita
                            val playtime = game.playtime ?: "N/A"
                            val playtimeText = SpannableStringBuilder()
                            playtimeText.append("Duración: ", StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            playtimeText.append("$playtime h")
                            binding.gamePlaytime.text = playtimeText

                            // Verifica si la lista de publishers no está vacía y extrae el nombre
                            val developerName = game.publishers?.firstOrNull()?.name ?: "N/A"
                            binding.gameDeveloper.text = developerName



                            // Generos en negrita
                            val genres = game.genres?.joinToString { it.name } ?: "No disponible"
                            val genresText = SpannableStringBuilder()
                            genresText.append("Géneros: ", StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            genresText.append(genres)
                            binding.gameGenres.text = genresText

                            // Plataformas en negrita
                            val platforms = game.platforms?.joinToString { it.platform.name } ?: "No disponible"
                            val platformsText = SpannableStringBuilder()
                            platformsText.append("Plataformas: ", StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            platformsText.append(platforms)
                            binding.gamePlatforms.text = platformsText

                            // Cargar la imagen del juego
                            Glide.with(this@GameViewActivity)
                                .load(game.background_image)
                                .into(binding.gameImage)


                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@GameViewActivity, "No se ha podido establecer la conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    FirebaseCrashlytics.getInstance().recordException(e)

                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Si el diálogo está visible, lo destruimos para evitar que siga en la pila
        dialog?.dismiss()
        dialog = null
    }
}
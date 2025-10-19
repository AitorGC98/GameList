package com.agc.gamelist.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.agc.gamelist.MainActivity
import com.agc.gamelist.R
import com.agc.gamelist.core.BaseActivity
import com.agc.gamelist.databinding.ActivityHomeBinding
import com.agc.gamelist.databinding.NavHeaderBinding
import com.agc.gamelist.model.Usuario
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Enumeración que representa los diferentes tipos de proveedores de autenticación.
 */
enum class ProviderType {
    BASIC,
    GOOGLE
}

/**
 * Actividad principal de la aplicación
 */
class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navHeaderBinding: NavHeaderBinding // Declarar el binding para el Nav Header
    private var dialog: AlertDialog? = null
    // Variables para manejar el usuario
    private lateinit var db: FirebaseFirestore
    private lateinit var usuario: Usuario // Clase Usuario para almacenar los datos
    private lateinit var levelText: TextView // TextView para el nivel
    private lateinit var experienceProgressBar: ProgressBar // ProgressBar para la experiencia
    private lateinit var experienceText: TextView // TextView para la experiencia
    private lateinit var listenerRegistration: ListenerRegistration
    private var primeraEjecucion:Boolean=true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Inicializa el binding del Nav Header
        val drawerNavView: NavigationView = binding.navViewDrawer // Renombrar aquí
        navHeaderBinding = NavHeaderBinding.bind(drawerNavView.getHeaderView(0))

        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatus) // Color oscuro
        db = FirebaseFirestore.getInstance()

        // Obtener la preferencia de tema guardada, si existe
        val prefsTheme = getSharedPreferences("settings", MODE_PRIVATE)
        val isNightMode = prefsTheme.getBoolean("night_mode", false)

        // Aplicar el tema guardado o el del sistema por defecto
        AppCompatDelegate.setDefaultNightMode(
            if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )


        // Referencia al ConstraintLayout
        val container: ConstraintLayout = findViewById(R.id.container)
        // Detectar el modo actual: claro u oscuro
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK

        // Cambiar el fondo según el modo
        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // Modo oscuro: fondo negro
            container.setBackgroundResource(R.color.black2)  // O cualquier color que desees para el fondo oscuro
        } else {
            // Modo claro: fondo blanco
            container.setBackgroundResource(R.drawable.background_cuadrado)  // O cualquier color que desees para el fondo claro
        }

        // Configurar elementos de la UI para el nivel y experiencia usando navHeaderBinding
        levelText = navHeaderBinding.levelText // Acceder al TextView en el NavHeader
        experienceProgressBar = navHeaderBinding.experienceProgressBar // Acceder al ProgressBar en el NavHeader
        experienceText = navHeaderBinding.experienceText // Acceder al TextView en el NavHeader

        // Obtención las preferencias del usuario
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val showNews = sharedPreferences.getBoolean("display_news", true)
        val showRecommended = sharedPreferences.getBoolean("display_recommended", true)
        val showSearch = sharedPreferences.getBoolean("display_search", true)
        val showForums = sharedPreferences.getBoolean("display_forums", true)
        val showLists = sharedPreferences.getBoolean("display_lists", true)

        // Configuración los ítems del BottomNavigationView según las preferencias
        val bottomNavMenu = binding.navView.menu

        // Ocultar o mostrar los ítems según las preferencias
        bottomNavMenu.findItem(R.id.navigation_home)?.isVisible = showNews
        bottomNavMenu.findItem(R.id.navigation_filter)?.isVisible = showRecommended
        bottomNavMenu.findItem(R.id.navigation_dashboard)?.isVisible = showSearch
        bottomNavMenu.findItem(R.id.navigation_new_feature)?.isVisible = showForums
        bottomNavMenu.findItem(R.id.navigation_notifications)?.isVisible = showLists

        // Establecer la Toolbar como la ActionBar
        setSupportActionBar(binding.toolbar)

        // Configura el NavController
        val navController = findNavController(R.id.nav_host_fragment_activity_home)

        // Configuración del AppBar con el NavController y el DrawerLayout
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_new_feature, R.id.navigation_filter
            ),
            binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        val sharedPreferencesCambio = this.getSharedPreferences("estado", Context.MODE_PRIVATE)

// Lee el valor de "cambio", con un valor predeterminado de "false" si no existe
        val cambio = sharedPreferencesCambio.getBoolean("cambio", false)

        if (cambio) {
            val editor = sharedPreferencesCambio.edit()
            editor.putBoolean("cambio", false)
            editor.apply() // Guarda los cambios de forma asíncrona

            val currentDestination = navController.currentDestination?.id

            //Control del fragmento que debe mostrarse
            if (!showNews && !showRecommended && !showSearch && !showForums && !showLists) {
                if (currentDestination != R.id.navigation_home) {
                    navController.navigate(R.id.navigation_home)  // Esto es solo un ejemplo
                }
            } else {
                // Recorre las preferencias y establece el primer fragmento visible
                when {
                    showNews -> {
                        navController.navigate(R.id.navigation_home)  // Noticias
                    }

                    showRecommended -> {
                        navController.navigate(R.id.navigation_filter)  // Recomendaciones
                    }

                    showSearch -> {
                        navController.navigate(R.id.navigation_dashboard)  // Búsqueda
                    }

                    showForums -> {
                        navController.navigate(R.id.navigation_new_feature)  // Foros
                    }

                    showLists -> {
                        navController.navigate(R.id.navigation_notifications)  // Listas
                    }
                }
            }
        }else if(primeraEjecucion){
            primeraEjecucion=false
            val editor = sharedPreferencesCambio.edit()
            editor.putBoolean("cambio", false)
            editor.apply() // Guarda los cambios de forma asíncrona

            val currentDestination = navController.currentDestination?.id

            if (!showNews && !showRecommended && !showSearch && !showForums && !showLists) {
                if (currentDestination != R.id.navigation_home) {
                    navController.navigate(R.id.navigation_home)  // Esto es solo un ejemplo
                }
            } else {
                // Recorre las preferencias y establece el primer fragmento visible
                when {
                    showNews -> {
                        navController.navigate(R.id.navigation_home)  // Noticias
                    }

                    showRecommended -> {
                        navController.navigate(R.id.navigation_filter)  // Recomendaciones
                    }

                    showSearch -> {
                        navController.navigate(R.id.navigation_dashboard)  // Búsqueda
                    }

                    showForums -> {
                        navController.navigate(R.id.navigation_new_feature)  // Foros
                    }

                    showLists -> {
                        navController.navigate(R.id.navigation_notifications)  // Listas
                    }
                }
            }

        }
        // Configura el BottomNavigationView
        val navView: BottomNavigationView = binding.navView
        navView.setupWithNavController(navController)

        // Configurar Crashlytics para manejar excepciones no capturadas
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            FirebaseCrashlytics.getInstance().recordException(throwable)
            // Puedes hacer cualquier limpieza aquí si es necesario
        }

        // Configura el icono de la Toolbar
        binding.toolbar.setNavigationIcon(R.drawable.baseline_person_24)
        binding.toolbar.setNavigationOnClickListener {
            // Obtener el modo de tema actual y alternarlo
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            // Forzar la actualización del menú para cambiar el icono
            drawerNavView.menu.findItem(R.id.nav_item4).icon = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                getDrawable(R.drawable.ic_night) // Icono para tema claro
            } else {
                getDrawable(R.drawable.ic_light) // Icono para tema oscuro
            }


            //Abre el menu lateral
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }




        //Listener de cambios en la navegación, para poner el icono de la app en cada fragmento
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home -> {
                    binding.toolbar.setNavigationIcon(R.drawable.baseline_person_24)
                }
                R.id.navigation_dashboard, R.id.navigation_notifications -> {
                    // Cambiar a un ícono de hamburguesa o mantener el icono actual
                    binding.toolbar.setNavigationIcon(R.drawable.baseline_person_24) // Cambia esto según lo que desees
                }
                R.id.navigation_filter -> {
                    binding.toolbar.setNavigationIcon(R.drawable.baseline_person_24)
                }
                R.id.navigation_new_feature -> {
                    binding.toolbar.setNavigationIcon(R.drawable.baseline_person_24)
                }

            }
        }

        // Manejar las selecciones del menú del NavigationView
        drawerNavView.setNavigationItemSelectedListener { menuItem -> // Cambiar aquí
            when (menuItem.itemId) {
                R.id.nav_item1 -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
                }
                R.id.nav_item2 -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_item3 -> {
                    confirmationMessage("Autor:Aitor García Curado\nVersión: 1.0\nAño: 2024")
                }
                R.id.nav_item4 -> {
                    // Obtener el modo de tema actual y alternarlo
                    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
                    val editor = prefs.edit()

                    if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        editor.putBoolean("night_mode", false)  // Guardar preferencia de tema
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        editor.putBoolean("night_mode", true)  // Guardar preferencia de tema
                    }
                    editor.apply()


                }
                R.id.nav_item5 -> {
                    // Limpia las preferencias guardadas
                    val prefs = getSharedPreferences(
                        getString(R.string.prefs_file),
                        Context.MODE_PRIVATE
                    ).edit()
                    prefs.clear()
                    prefs.apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }

                R.id.nav_item6 -> {
                    showReportDialog()
                }

            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Obtiene los datos del Intent del email
        val bundle = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")

        // Guardado del inicio de sesión en las preferencias
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()


        cargarUsuario(getCurrentUserId())

        // Agregar el click listener al ImageButton
        navHeaderBinding.imageButton.setOnClickListener {
            try {
                // Manejar la acción de clic aquí
                val intent = Intent(this, PerfilActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                // Maneja el error según sea necesario, como mostrar un mensaje al usuario
            }
        }
        cargarNavHeader()

    }

// Función para cargar los datos del encabezado de la navegación
    private fun cargarNavHeader(){
        val prefsEdit = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val imagenPerfil = prefsEdit.getString("profileImage", "baseline_person_24") // Cambia "defaultName" según sea necesario
        val nombrePerfil = prefsEdit.getString("userName", "Anónimo")
        val correoPerfil = prefsEdit.getString("email", "correo@correo.com")

        val resourceId = resources.getIdentifier(imagenPerfil, "drawable", packageName)
        navHeaderBinding.profileName.text = nombrePerfil
        navHeaderBinding.profileEmail.text = correoPerfil

        navHeaderBinding.profileImage.setImageResource(resourceId)
    }


    private fun showReportDialog() {
        // Crear el EditText programáticamente
        val editText = EditText(this).apply {
            hint = "Describe el problema"
            maxLines = 5
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = Gravity.TOP or Gravity.START
            // Cambiar el color de fondo y el color del texto
            setBackgroundColor(ContextCompat.getColor(this@HomeActivity, R.color.fondoNews)) // Color de fondo
            setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.colorFuente)) // Color del texto
            setHintTextColor(ContextCompat.getColor(this@HomeActivity, R.color.colorFuenteSemiTrasparente)) // Color del hint
            setPadding(16, 16, 16, 16)  // Agregar padding para que el texto no quede pegado a los bordes
            // Hacer el EditText de tamaño adecuado y evitar corte
            setMinHeight(200)  // Definir una altura mínima si es necesario
            isVerticalScrollBarEnabled = true // Habilitar la barra de desplazamiento
            setHorizontallyScrolling(false) // No permite desplazamiento horizontal, solo vertical
            movementMethod = ScrollingMovementMethod() // Hacer que el EditText sea desplazable
        }

        // Crear el AlertDialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Escribe tu reporte,soporte no tardará en atenderlo y solucionarlo")
        builder.setView(editText)  // Establecer el EditText en el diálogo

        // Crear y personalizar los botones
        builder.setPositiveButton("Enviar") { _, _ ->
            val description = editText.text.toString().trim()

            if (description.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa una descripción", Toast.LENGTH_SHORT).show()
            } else {
                // Crear el reporte en Firebase
                val report = hashMapOf(
                    "description" to description,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "user" to FirebaseAuth.getInstance().currentUser?.uid  // Agregar UID de usuario si está autenticado
                )

                val db = FirebaseFirestore.getInstance()
                db.collection("Reportes")
                    .add(report)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reporte enviado a soporte. Gracias por su paciencia", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {e ->
                        FirebaseCrashlytics.getInstance().recordException(e)
                    }
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()  // Cierra el diálogo
        }

        // Mostrar el diálogo
        val dialog = builder.create()

        // Personalizar el fondo y el color de los textos
        dialog.setOnShowListener {
            // Establecer el color de fondo del diálogo
            dialog.window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.fondoNews)))  // Fondo del diálogo

            // Personalizar el color del título
            val title = dialog.findViewById<TextView>(android.R.id.title)
            title?.setTextColor(ContextCompat.getColor(this, R.color.colorFuente)) // Color del texto del título

            // Personalizar el color de los botones
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.colorFuente)) // Color del texto del botón positivo

            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.colorFuente)) // Color del texto del botón negativo
        }

        // Mostrar el diálogo
        dialog.show()
    }



//Carga de los datos de usuario
    private fun cargarUsuario(userId: String) {
        Log.d("HomeActivity", "Iniciando la carga del usuario con ID: $userId")

        // Escuchar cambios en el documento del usuario en Firestore
        listenerRegistration = db.collection("Usuarios").document(userId)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.w("HomeActivity", "Error al escuchar el documento", e)
                    return@addSnapshotListener
                }

                if (documentSnapshot != null) {
                    Log.d("HomeActivity", "Documento encontrado: ${documentSnapshot.id}")

                    if (documentSnapshot.exists()) {
                        // Obtener los datos del documento
                        usuario = documentSnapshot.toObject(Usuario::class.java) ?: Usuario()
                        Log.d("HomeActivity", "Datos del usuario: $usuario")
                        Log.d("HomeActivity", "Datos del usuario: ${usuario.experienciaActual}")
                        actualizarUI()
                    } else {
                        Log.d("HomeActivity", "El documento no existe")
                        // Manejar el caso en que el documento no exista
                    }
                } else {
                    Log.d("HomeActivity", "documentSnapshot es nulo")
                }
            }
    }

    //  Función para actualizar la UI con los datos del usuario
    private fun actualizarUI() {
        Log.d("HomeActivity", "Actualizando la UI con los datos del usuario.")

        // Asegúrate de que los datos del usuario no sean nulos
        if (::usuario.isInitialized) {
            // Actualizar el texto del nivel
            navHeaderBinding.profileName.text = usuario.nombre
            levelText.text = "Nivel: ${usuario.nivel}"
            Log.d("HomeActivity", "Nivel del usuario: ${usuario.nivel}")

            // Calcular el progreso del ProgressBar
            val progress = if (usuario.experienciaNecesaria > 0) {
                (usuario.experienciaActual / usuario.experienciaNecesaria.toFloat() * 100).toInt()
            } else {
                0 // Evitar división por cero
            }
            experienceProgressBar.progress = progress
            Log.d("HomeActivity", "Progreso del usuario: $progress%")

            // Actualizar el texto de la experiencia
            experienceText.text = "Exp: ${usuario.experienciaActual}/${usuario.experienciaNecesaria}"
            Log.d("HomeActivity", "Experiencia actual: ${usuario.experienciaActual}, necesaria: ${usuario.experienciaNecesaria}")
        } else {
            Log.w("HomeActivity", "El usuario no ha sido inicializado.")
        }
    }


    private fun confirmationMessage(message: String) {
        // Solo muestra el diálogo si no está ya visible
        if (dialog == null || !dialog!!.isShowing) {
            val builder = AlertDialog.Builder(this)

            // Establecer el título del diálogo
            builder.setTitle("GameList")  // Título del diálogo
            builder.setMessage(message)  // Mensaje a mostrar

            builder.setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()  // Cierra el diálogo cuando el usuario pulsa "Aceptar"
            }

            // Crear y mostrar el diálogo
            dialog = builder.create()
            dialog?.setCancelable(false)  // Evita que se cierre tocando fuera del diálogo

            // Personalizar el fondo y los textos
            dialog?.setOnShowListener {
                // Establecer el color de fondo del diálogo
                dialog?.window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.fondoNews)))  // Fondo del diálogo

                // Personalizar el color del título
                val title = dialog?.findViewById<TextView>(android.R.id.title)
                title?.setTextColor(ContextCompat.getColor(this, R.color.colorFuente)) // Color del texto del título

                // Personalizar el color de los botones
                val positiveButton = dialog?.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton?.setTextColor(ContextCompat.getColor(this, R.color.colorFuente)) // Color del texto del botón positivo
            }

            // Mostrar el diálogo
            dialog?.show()
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        // Si el diálogo está visible, lo destruimos para evitar que siga en la pila
        dialog?.dismiss()
        dialog = null
    }

    // Recuerda desregistrar el listener cuando ya no lo necesites
    override fun onStop() {
        super.onStop()
        listenerRegistration.remove() // Detener el listener al salir de la actividad
    }

    private fun getCurrentUserId(): String {

        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)
        // Recupera el email y el proveedor de la última sesión guardada.
        val email = prefs.getString("email", null)
        return email.toString()
        Log.d("ComentariosForoActivity",  email.toString())
    }

    override fun onResume() {
        super.onResume()
        cargarNavHeader()
        cargarUsuario(getCurrentUserId())
        val sharedPreferencesCambio = this.getSharedPreferences("estado", Context.MODE_PRIVATE)
        val cambio = sharedPreferencesCambio.getBoolean("cambio", false)

        if (cambio) {
            // Restablece el estado de cambio en las preferencias para evitar bucles de reinicio
            val editor = sharedPreferencesCambio.edit()
            editor.putBoolean("cambio", false)
            editor.apply()

            // Reinicia la actividad para aplicar los cambios
            val intent = intent
            finish() // Cierra la actividad actual
            startActivity(intent) // Inicia una nueva instancia de la actividad
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_home)
        return navController.navigateUp(binding.drawerLayout) || super.onSupportNavigateUp()
    }
}

package com.agc.gamelist

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.agc.gamelist.activity.SignUpActivity
import com.agc.gamelist.core.BaseActivity
import com.agc.gamelist.databinding.ActivityMainBinding
import com.agc.gamelist.activity.HomeActivity
import com.agc.gamelist.activity.ProviderType
import com.agc.gamelist.model.Usuario
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Clase principal de la aplicación.
 */
    class MainActivity : BaseActivity() {

    // Se define una variable para manejar el resultado del inicio de sesión con Google.
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    // Se define una variable para manejar el binding de la vista, lo que permite acceder a los elementos de la UI.
    private lateinit var binding: ActivityMainBinding

    // Método que se ejecuta cuando la actividad es creada.
    override fun onCreate(savedInstanceState: Bundle?) {
        // Oculta la barra de acción para esta actividad.
        supportActionBar?.hide()
        // Llama al método onCreate de la superclase (BaseActivity).
        super.onCreate(savedInstanceState)
        // Bloquear la orientación
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Inicializa el binding con el layout de la actividad.
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Establece la vista raíz del layout como la vista de contenido de la actividad.
        setContentView(binding.root)

        // Obtener la preferencia de tema guardada, si existe
        val prefsTheme = getSharedPreferences("settings", MODE_PRIVATE)
        val isNightMode = prefsTheme.getBoolean("night_mode", false)

        // Aplicar el tema guardado o el del sistema por defecto
        AppCompatDelegate.setDefaultNightMode(
            if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Inicializa el ActivityResultLauncher, el cual manejará los resultados del flujo de inicio de sesión.
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Comprobación del éxito de la operación
            if (result.resultCode == Activity.RESULT_OK) {
                // Obtiene los datos devueltos por el intent
                val data: Intent? = result.data
                // Llama al método para manejar el resultado del inicio de sesión de Google.
                handleSignInResult(data)
            }
        }

        // Habilita el modo edge-to-edge para la interfaz de usuario.
        enableEdgeToEdge()
        // Ajusta los márgenes de la vista principal para que no colisione con las barras del sistema (barra de estado y de navegación).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_activity)) { v, insets ->
            // Obtiene los márgenes de las barras del sistema.
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Aplica los márgenes a la vista.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            // Devuelve los insets para seguir propagándolos.
            insets
        }

        // Configura la interfaz de usuario y los botones.
        setUp()
        // Verifica si ya hay una sesión activa de usuario.(evita bugs con el inicio de sesion)
        session()
        //Recuperar contraseña
        recuperarContrasena()

        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatus) // Color oscuro


    }


// Muestra el mensaje indicando que se ha enviado el enlace de restablecimiento de contraseña
    private fun showResetPasswordDialog() {
        // Crea el AlertDialog Builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Correo enviado")
        builder.setMessage("Se ha enviado un enlace para restablecer tu contraseña al correo proporcionado.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        // Muestra el Dialog
        builder.create().show()
    }

// Función para manejar la recuperación de la contraseña
    private fun recuperarContrasena() {
    // Configura un listener para cuando el usuario haga clic en el TextView para olvidar la contraseña
    binding.forgetPasswordTextView.setOnClickListener {
            val email = binding.editTextEmail.text.toString()

            if (email.isNotEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Notifica al usuario que el email se envió correctamente
                            binding.errorTextView.apply {
                                showResetPasswordDialog()
                            }
                        } else {
                            // Muestra un mensaje si ocurre algún error
                            binding.errorTextView.apply {
                                text = "Error al enviar el correo de restablecimiento. Verifica que el correo sea correcto."
                                setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                                visibility = View.VISIBLE
                            }
                        }
                    }
            } else {
                // Muestra un mensaje si el campo de email está vacío
                binding.errorTextView.apply {
                    text = "Por favor ingresa tu correo electrónico para restablecer la contraseña."
                    setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                    visibility = View.VISIBLE
                }
            }
        }

    }

    // Método que comprueba si ya hay una sesión de usuario activa.
    private fun session() {
        // Obtiene las preferencias almacenadas en la aplicación (datos del usuario).
        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)
        // Recupera el email y el proveedor de la última sesión guardada.
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        // Si hay un email y un proveedor almacenados, se redirige a la pantalla principal.
        if (email != null && provider != null) {
            showHome(email, ProviderType.valueOf(provider))
        }
    }

    // Método que configura los botones y la lógica de autenticación.
    private fun setUp() {
        // Cargar las animaciones
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)
        val pulsar = AnimationUtils.loadAnimation(this, R.anim.use_animation)

        // Iniciar la animación de pulso, pero asegúrate de detener cualquier animación previa
        binding.loginbutton.clearAnimation()  // Detenemos cualquier animación anterior
        binding.loginbutton.startAnimation(pulseAnimation)  // Iniciamos la animación de pulso

        // Configura el botón que redirige a la actividad de registro (SignUpActivity)
        binding.textviewbutton.setOnClickListener {


            // Redirigir a la actividad de registro
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Obtén referencia al TextView de error
        val errorTextView = findViewById<TextView>(R.id.errorTextView)
        // Configura el botón de inicio de sesión con email y contraseña.
        binding.loginbutton.setOnClickListener {
            // Detenemos cualquier animación anterior antes de aplicar la nueva
            binding.loginbutton.clearAnimation()

            // Iniciar la animación de "presionar" (como si fuera un efecto de clic)
            binding.loginbutton.startAnimation(pulsar)
            errorTextView.visibility = View.GONE // Oculta el mensaje antes de intentar iniciar sesión
            // Comprueba que los campos de email y contraseña no estén vacíos.
            if (binding.editTextEmail.text.isNotEmpty() && binding.editTextPassword.text.isNotEmpty()) {
                // Llama a FirebaseAuth para iniciar sesión con email y contraseña.
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(
                        binding.editTextEmail.text.toString(),
                        binding.editTextPassword.text.toString()
                    ).addOnCompleteListener { task ->
                        // Si la autenticación es exitosa, redirige al usuario a la pantalla principal.
                        if (task.isSuccessful) {
                            // Obtener datos del usuario desde Firestore
                            val userEmail = task.result?.user?.email ?: ""
                            obtenerDatosUsuario(userEmail)
                        } else {
                            // Verifica que el error no se deba a credenciales inválidas
                            task.exception?.let { exception ->
                                when {
                                    exception.message?.contains("password") == true -> {
                                        errorTextView.text = "Contraseña incorrecta"
                                    }
                                    exception.message?.contains("no user record") == true -> {
                                        errorTextView.text = "El usuario no existe"
                                    }
                                    exception.message?.contains("email") == true -> {
                                        errorTextView.text = "Formato de correo inválido"
                                    }
                                    else -> {
                                        binding.loginbutton.clearAnimation()  // Detenemos cualquier animación anterior
                                        binding.loginbutton.startAnimation(pulseAnimation)  // Iniciamos la animación de pulso
                                        errorTextView.text = "El usuario o contraseña son incorrectos, intentelo de nuevo"
                                        FirebaseCrashlytics.getInstance().recordException(exception)
                                    }
                                }
                                errorTextView.visibility = View.VISIBLE // Muestra el mensaje de error
                            }

                        }
                    }
            } else {
                binding.loginbutton.clearAnimation()  // Detenemos cualquier animación anterior
                binding.loginbutton.startAnimation(pulseAnimation)  // Iniciamos la animación de pulso
                errorTextView.text = "Ingrese su email y contraseña"
                errorTextView.visibility = View.VISIBLE // Muestra el mensaje si los campos están vacíos
            }
        }

        // Configura el botón de inicio de sesión con Google.
        binding.googlebutton.setOnClickListener {
            // Configuración de Google Sign In
            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            // Crea un cliente de Google Sign-In con la configuración anterior.
            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut() // Cierra la sesión anterior
            signInLauncher.launch(googleClient.signInIntent) // Iniciar el flujo de inicio de sesión
        }

        binding.googletext.setOnClickListener{
            // Configuración de Google Sign In
            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            // Crea un cliente de Google Sign-In con la configuración anterior.
            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut() // Cierra la sesión anterior
            signInLauncher.launch(googleClient.signInIntent) // Iniciar el flujo de inicio de sesión

        }

    }

    // Método que maneja el resultado del inicio de sesión con Google.
    private fun handleSignInResult(data: Intent?) {
        // Obtiene el resultado de la cuenta de Google desde el intent.
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
        // Intenta obtener la cuenta de Google del resultado.
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            if (account != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                // Autentica al usuario en Firebase con las credenciales obtenidas.
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Obtener datos del usuario desde Firestore
                            val userEmail = task.result?.user?.email ?: ""
                            obtenerDatosUsuario(userEmail)
                        } else {
                            // Reportar el error en Crashlytics sin lanzar excepción
                            FirebaseCrashlytics.getInstance().log("Se ha producido un error autenticando al usuario con Google")
                        }
                    }
            }
        } catch (e: ApiException) { // Si ocurre una excepción, muestra un mensaje de error.
            FirebaseCrashlytics.getInstance().recordException(e)

        }
    }

    // Método para obtener datos del usuario desde Firestore
    private fun obtenerDatosUsuario(email: String) {
        val usuariosRef = FirebaseFirestore.getInstance().collection("Usuarios")
        usuariosRef.document(email).get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Si el documento ya existe, obtenemos los datos del usuario
                val nombre = document.getString("nombre") ?: ""
                val fotoPerfil = document.getString("fotoPerfil") ?: ""
                val sobreTi = document.getString("sobreTi") ?: ""  // Obtener "Sobre ti"
                val foros = document.get("foros") as? List<String> ?: emptyList()

                // Crear un objeto Usuario
                val usuario = Usuario(nombre, email, fotoPerfil, foros)

                // Guardar datos en SharedPreferences
                val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)
                with(prefs.edit()) {
                    putString("profileImage", usuario.fotoPerfil)
                    putString("userName", usuario.nombre)
                    putString("email", usuario.correo)
                    putString("aboutMe", sobreTi)  // Guardar "Sobre ti"
                    apply() // Guardar los cambios
                }

                // Redirigir a la pantalla principal
                showHome(email, ProviderType.BASIC)
            } else {
                // Si el documento no existe (es la primera vez que entra el usuario), creamos el perfil
                crearNuevoUsuario(email)
            }
        }.addOnFailureListener { e ->
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    // Método para crear un nuevo usuario si no existe en la base de datos
    private fun crearNuevoUsuario(email: String) {
        val usuariosRef = FirebaseFirestore.getInstance().collection("Usuarios")

        // Aquí puedes definir valores predeterminados para los nuevos usuarios, como nombre y foto de perfil.
        val nuevoUsuario = hashMapOf(
            "nombre" to "Nuevo Usuario",
            "fotoPerfil" to "",
            "sobreTi" to "",
            "foros" to emptyList<String>()
        )

        // Guardar el nuevo usuario en la colección "Usuarios"
        usuariosRef.document(email).set(nuevoUsuario).addOnSuccessListener {
            // Después de crear el usuario, guardamos los datos en SharedPreferences y redirigimos
            val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)
            with(prefs.edit()) {
                putString("profileImage", "")
                putString("userName", "Anónimo")
                putString("email", email)
                putString("aboutMe", "")  // Guardar "Sobre ti"
                apply() // Guardar los cambios
            }

            // Redirigir al usuario a la pantalla principal
            showHome(email, ProviderType.BASIC)
        }.addOnFailureListener { e ->
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }



    // Método que redirige al usuario a la pantalla principal.
    private fun showHome(email: String, provider: ProviderType) {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
        finish()
    }
}


package com.agc.gamelist.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.agc.gamelist.R
import com.agc.gamelist.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase

/**
 * Clase que representa la actividad de registro de usuarios.
 */
class SignUpActivity : AppCompatActivity() {

    // Variable para el ViewBinding que permite acceder a las vistas del layout sin usar findViewById.
    private lateinit var binding: ActivitySignUpBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatus) // Color oscuro

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Configura la interfaz de usuario y las acciones de los botones.
        binding.volvertext.setOnClickListener {
            finish()
        }
        setUp()
    }

    private fun setUp() {
        // Cargar las animaciones
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)
        val pulsar = AnimationUtils.loadAnimation(this, R.anim.use_animation)

        // Iniciar la animación de pulso, pero asegúrate de detener cualquier animación previa
        binding.signinbutton.clearAnimation()  // Detenemos cualquier animación anterior
        binding.signinbutton.startAnimation(pulseAnimation)  // Iniciamos la animación de pulso
        // Configura el botón de registro de usuario.
        binding.signinbutton.setOnClickListener {
            binding.signinbutton.clearAnimation()  // Detenemos cualquier animación anterior
            binding.signinbutton.startAnimation(pulsar)  // Iniciamos la animación de pulso
            val email = binding.edemail.text.toString()
            val password = binding.edpassword.text.toString()
            val repeatPassword = binding.repeatPassword.text.toString()

            // Inicializa el TextView de error
            binding.errorTextView.visibility = View.GONE

            // Verifica el formato del email
            if (!isValidEmail(email)) {
                binding.signinbutton.clearAnimation()  // Detenemos cualquier animación anterior
                binding.signinbutton.startAnimation(pulseAnimation)  // Iniciamos la animación de pulso
                binding.errorTextView.text = "El formato del correo electrónico es incorrecto."
                binding.errorTextView.visibility = View.VISIBLE
                return@setOnClickListener

            }

            // Verifica que la contraseña sea lo suficientemente fuerte
            if (!isValidPassword(password)) {
                binding.signinbutton.clearAnimation()  // Detenemos cualquier animación anterior
                binding.signinbutton.startAnimation(pulseAnimation)  // Iniciamos la animación de pulso
                binding.errorTextView.text = "La contraseña debe tener al menos 8 caracteres, incluyendo al menos una letra mayúscula, un número y un carácter especial."
                binding.errorTextView.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Verifica que las contraseñas coincidan
            if (password != repeatPassword) {
                binding.signinbutton.clearAnimation()  // Detenemos cualquier animación anterior
                binding.signinbutton.startAnimation(pulseAnimation)  // Iniciamos la animación de pulso
                binding.errorTextView.text = "Las contraseñas no coinciden."
                binding.errorTextView.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Si todo es válido, crea el usuario en Firebase
            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        showHome(it.result?.user?.email ?: "", ProviderType.BASIC)
                    } else {
                        binding.signinbutton.clearAnimation()  // Detenemos cualquier animación anterior
                        binding.signinbutton.startAnimation(pulseAnimation)  // Iniciamos la animación de pulso
                        val exception = it.exception
                        if (exception is FirebaseAuthUserCollisionException) {

                            binding.errorTextView.text = "La dirección de correo electrónico ya está en uso."
                            binding.errorTextView.visibility = View.VISIBLE
                        } else {
                            // Registra el error en Crashlytics
                            FirebaseCrashlytics.getInstance().recordException(exception ?: Exception("Error desconocido durante el registro."))
                            binding.errorTextView.text = "Fallo en el proceso de registro, intentelo de nuevo"
                            binding.errorTextView.visibility = View.VISIBLE

                        }
                    }
                }
        }
    }

    // Método para redirigir al usuario a la pantalla principal (HomeActivity) después del registro.
    private fun showHome(email:String,provider: ProviderType){
        val homeIntent:Intent= Intent(this, HomeActivity::class.java).apply {
            putExtra("email",email)
            putExtra("provider",provider.name)
        }
        startActivity(homeIntent)
        finish()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Método para verificar si la contraseña es válida
    private fun isValidPassword(password: String): Boolean {
        // Expresión regular que verifica si la contraseña tiene al menos 8 caracteres,
        // una letra mayúscula, un número y un carácter especial
        val pattern = Regex("^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?])(?=.{8,})")
        return pattern.containsMatchIn(password)
    }

}
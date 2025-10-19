package com.agc.gamelist.activity

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.agc.gamelist.R
import java.io.InputStreamReader

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatus) // Color oscuro
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportActionBar?.title = "GameList"

    }

    // Manejar el clic en la flecha para cerrar la actividad
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()  // Cierra la actividad y vuelve a la anterior en la pila
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
// Fragment para mostrar las preferencias
    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var sharedPreferences: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)


            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

            // Definir las claves de las preferencias
            val keys = arrayOf("display_news", "display_recommended", "display_search", "display_forums", "display_lists")

            // Inicializar el listener
            val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
                handlePreferenceChange(sharedPrefs, key, keys)
            }

            // Registrar el listener para detectar cambios
            sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

            // Verificar el estado inicial de las preferencias
            handlePreferenceChange(sharedPreferences, null, keys)


            // Acceso a cada opción de la categoría de visualización
            val newsPreference = findPreference<SwitchPreferenceCompat>("display_news")
            val recommendedPreference = findPreference<SwitchPreferenceCompat>("display_recommended")
            val searchPreference = findPreference<SwitchPreferenceCompat>("display_search")
            val forumsPreference = findPreference<SwitchPreferenceCompat>("display_forums")
            val listsPreference = findPreference<SwitchPreferenceCompat>("display_lists")

            // Obtener las preferencias de Términos de Uso y Política de Privacidad
            val termsPreference = findPreference<Preference>("terms_of_use")
            val privacyPreference = findPreference<Preference>("privacy_policy")
            val licencias = findPreference<Preference>("licencia")

            // Configurar los clics
            licencias?.setOnPreferenceClickListener {
                showLicense()
                true
            }

            // Configurar los clics
            termsPreference?.setOnPreferenceClickListener {
                showTermsOfUseDialog()
                true
            }

            privacyPreference?.setOnPreferenceClickListener {
                showPrivacyPolicyDialog()
                true
            }

            // Listener para cada preferencia (si necesitas ejecutar acciones al cambiar)
            newsPreference?.setOnPreferenceChangeListener { _, newValue ->
                val isEnabled = newValue as Boolean
                // Crea una instancia de SharedPreferences, aquí usando "miPreferencia" como nombre del archivo
                val sharedPreferences = requireContext().getSharedPreferences("estado", Context.MODE_PRIVATE)

                // Escribe la variable "cambio" en SharedPreferences y ponla a "true"
                val editor = sharedPreferences.edit()
                editor.putBoolean("cambio", true)
                editor.apply() // Guarda los cambios de forma asíncrona
                // Lógica para "Noticias"
                true
            }

            recommendedPreference?.setOnPreferenceChangeListener { _, newValue ->
                val isEnabled = newValue as Boolean
                // Crea una instancia de SharedPreferences, aquí usando "miPreferencia" como nombre del archivo
                val sharedPreferences = requireContext().getSharedPreferences("estado", Context.MODE_PRIVATE)

                // Escribe la variable "cambio" en SharedPreferences y ponla a "true"
                val editor = sharedPreferences.edit()
                editor.putBoolean("cambio", true)
                editor.apply() // Guarda los cambios de forma asíncrona
                // Lógica para "Recomendados"
                true
            }

            searchPreference?.setOnPreferenceChangeListener { _, newValue ->
                val isEnabled = newValue as Boolean
                // Crea una instancia de SharedPreferences, aquí usando "miPreferencia" como nombre del archivo
                val sharedPreferences = requireContext().getSharedPreferences("estado", Context.MODE_PRIVATE)

                // Escribe la variable "cambio" en SharedPreferences y ponla a "true"
                val editor = sharedPreferences.edit()
                editor.putBoolean("cambio", true)
                editor.apply() // Guarda los cambios de forma asíncrona
                // Lógica para "Búsqueda"
                true
            }

            forumsPreference?.setOnPreferenceChangeListener { _, newValue ->
                val isEnabled = newValue as Boolean
                // Crea una instancia de SharedPreferences, aquí usando "miPreferencia" como nombre del archivo
                val sharedPreferences = requireContext().getSharedPreferences("estado", Context.MODE_PRIVATE)

                // Escribe la variable "cambio" en SharedPreferences y ponla a "true"
                val editor = sharedPreferences.edit()
                editor.putBoolean("cambio", true)
                editor.apply() // Guarda los cambios de forma asíncrona
                // Lógica para "Foros"
                true
            }

            listsPreference?.setOnPreferenceChangeListener { _, newValue ->
                val isEnabled = newValue as Boolean
                // Crea una instancia de SharedPreferences, aquí usando "miPreferencia" como nombre del archivo
                val sharedPreferences = requireContext().getSharedPreferences("estado", Context.MODE_PRIVATE)

                // Escribe la variable "cambio" en SharedPreferences y ponla a "true"
                val editor = sharedPreferences.edit()
                editor.putBoolean("cambio", true)
                editor.apply() // Guarda los cambios de forma asíncrona
                // Lógica para "Listas"
                true
            }
        }
// Mostrar la licencia
        private fun showLicense() {
            val builder = AlertDialog.Builder(requireContext())

            // Crear un WebView
            val webView = WebView(requireContext())

            // Habilitar JavaScript si es necesario (opcional)
            webView.settings.javaScriptEnabled = true

            // Cargar el archivo HTML desde res/raw
            val termsInputStream = resources.openRawResource(R.raw.license) // R.raw.terms_of_use es el nombre del archivo HTML sin extensión
            val reader = InputStreamReader(termsInputStream)
            val content = reader.readText()

            // Cargar el contenido HTML en el WebView
            webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)

            // Establecer el WebView en el AlertDialog
            builder.setView(webView)

            // Botones de acción: Aceptar y Cancelar
            builder.setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            // Mostrar el diálogo
            builder.create().show()
        }
// Mostrar los términos de uso
        private fun showTermsOfUseDialog() {
            val builder = AlertDialog.Builder(requireContext())

            // Crear un WebView
            val webView = WebView(requireContext())

            // Habilitar JavaScript si es necesario (opcional)
            webView.settings.javaScriptEnabled = true

            // Cargar el archivo HTML desde res/raw
            val termsInputStream = resources.openRawResource(R.raw.terms_of_use) // R.raw.terms_of_use es el nombre del archivo HTML sin extensión
            val reader = InputStreamReader(termsInputStream)
            val content = reader.readText()

            // Cargar el contenido HTML en el WebView
            webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)

            // Establecer el WebView en el AlertDialog
            builder.setView(webView)

            // Botones de acción: Aceptar y Cancelar
            builder.setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            // Mostrar el diálogo
            builder.create().show()
        }

    // Mostrar la política de privacidad
        private fun showPrivacyPolicyDialog() {
            val builder = AlertDialog.Builder(requireContext())

            // Crear un WebView
            val webView = WebView(requireContext())

            // Habilitar JavaScript si es necesario (opcional)
            webView.settings.javaScriptEnabled = true

            // Cargar el archivo HTML desde res/raw
            val termsInputStream = resources.openRawResource(R.raw.privacy_policy) // R.raw.terms_of_use es el nombre del archivo HTML sin extensión
            val reader = InputStreamReader(termsInputStream)
            val content = reader.readText()

            // Cargar el contenido HTML en el WebView
            webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)

            // Establecer el WebView en el AlertDialog
            builder.setView(webView)

            // Botones de acción: Aceptar y Cancelar
            builder.setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            // Mostrar el diálogo
            builder.create().show()
        }

// Actualizar el estado de las preferencias
        private fun handlePreferenceChange(sharedPrefs: SharedPreferences, changedKey: String?, keys: Array<String>) {
            // Contamos cuántas preferencias están activadas
            val enabledPreferences = keys.filter { sharedPrefs.getBoolean(it, false) }

            if (enabledPreferences.size == 1) {
                // Si solo queda una preferencia activada, bloqueamos esa preferencia
                val lastActiveKey = enabledPreferences.first() // La preferencia activada
                val lastActivePref = findPreference<SwitchPreferenceCompat>(lastActiveKey)
                lastActivePref?.isEnabled = false // Bloqueamos la opción
            } else {
                // Si hay más de una opción activada, desbloqueamos las opciones
                keys.forEach { key ->
                    val pref = findPreference<SwitchPreferenceCompat>(key)
                    if (pref != null && sharedPrefs.getBoolean(key, false)) {
                        pref.isEnabled = true
                    }
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            // Eliminar el listener cuando el fragmento se destruya
            sharedPreferences.unregisterOnSharedPreferenceChangeListener { _, _ -> }
        }

    }


}
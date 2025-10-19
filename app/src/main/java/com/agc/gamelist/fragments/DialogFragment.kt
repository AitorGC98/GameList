package com.agc.gamelist.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.DialogFragment
import com.agc.gamelist.R

class ImageSelectionDialogFragment : DialogFragment() {
    private var listener: ((String) -> Unit)? = null

    fun setOnImageSelectedListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_image_selection, null)
        dialog.setView(view)

        // Obtén el LinearLayout donde se agregarán las imágenes
        val imageContainer = view.findViewById<LinearLayout>(R.id.imageContainer)

        // Define un tamaño más pequeño para las imágenes
        val imageSize = resources.displayMetrics.widthPixels / 5 // Ajusta el tamaño a 1/5 del ancho de la pantalla

        // Agrega las imágenes al LinearLayout en fila horizontal
        for (i in 1..20) {
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(imageSize, imageSize).apply {
                    setMargins(8, 8, 8, 8) // Margen entre las imágenes
                }
                setImageResource(resources.getIdentifier("a$i", "drawable", context?.packageName))
                setOnClickListener {
                    listener?.invoke("a$i") // Retorna el nombre de la imagen
                    dismiss() // Cierra el diálogo
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                adjustViewBounds = true
            }
            imageContainer.addView(imageView)
        }

        return dialog.create()
    }
}



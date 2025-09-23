package com.example.vetgobeta.ui  // <-- ajusta si tu paquete es otro

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.vetgobeta.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText

class CitaInmediataBottomSheet : BottomSheetDialogFragment(R.layout.bottomsheet_cita) {

    private var fotoUri: Uri? = null

    // Selector de imagen desde la galería (no requiere permisos extras)
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            fotoUri = it
            view?.findViewById<ImageView>(R.id.ivFoto)?.setImageURI(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etNombre = view.findViewById<TextInputEditText>(R.id.etNombre)
        val etMascota = view.findViewById<TextInputEditText>(R.id.etMascota)
        val etCorreo  = view.findViewById<TextInputEditText>(R.id.etCorreo)
        val etCelular = view.findViewById<TextInputEditText>(R.id.etCelular)
        val etDireccion = view.findViewById<TextInputEditText>(R.id.etDireccion)
        val etMotivo = view.findViewById<TextInputEditText>(R.id.etMotivo)

        view.findViewById<LinearLayout>(R.id.btnAgregarFoto).setOnClickListener {
            pickImage.launch("image/*")
        }

        view.findViewById<Button>(R.id.btnCancelar).setOnClickListener { dismiss() }

        view.findViewById<Button>(R.id.btnEnviar).setOnClickListener {
            val nombre = etNombre.text?.toString()?.trim().orEmpty()
            val mascota = etMascota.text?.toString()?.trim().orEmpty()
            val correo  = etCorreo.text?.toString()?.trim().orEmpty()
            val celular = etCelular.text?.toString()?.trim().orEmpty()
            val direccion = etDireccion.text?.toString()?.trim().orEmpty()
            val motivo = etMotivo.text?.toString()?.trim().orEmpty()

            // Validación mínima
            if (nombre.isEmpty() || mascota.isEmpty() || correo.isEmpty() || celular.isEmpty()) {
                Toast.makeText(requireContext(), "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Entregar datos al Activity que abrió el sheet (usaremos Fragment Result API en el paso 3)
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY_CITA,
                Bundle().apply {
                    putString("nombre", nombre)
                    putString("mascota", mascota)
                    putString("correo", correo)
                    putString("celular", celular)
                    putString("direccion", direccion)
                    putString("motivo", motivo)
                    putString("fotoUri", fotoUri?.toString() ?: "")
                }
            )
            dismiss()
        }
    }

    companion object {
        const val REQUEST_KEY_CITA = "citaRequestKey"
    }
}

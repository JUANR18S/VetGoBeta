package com.example.vetgobeta

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Flujo:
 * 1) Usuario llena formulario y pulsa "Crear cuenta".
 * 2) Validamos en cliente (campos y formatos).
 * 3) Creamos usuario en Firebase Auth (email/clave).
 * 4) Guardamos datos extra en Firestore (/users/{uid}).
 * 5) Enviamos email de verificación y volvemos al Login.
 * Botón "Cancelar": confirma si hay datos escritos y sale.
 */
class RegisterActivity : AppCompatActivity() {

    // --- UI ---
    private lateinit var tilName: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilDoc: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilP1: TextInputLayout
    private lateinit var tilP2: TextInputLayout

    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etDoc: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etP1: TextInputEditText
    private lateinit var etP2: TextInputEditText
    private lateinit var btnCreate: Button
    private lateinit var btnCancel: Button

    // --- Firebase ---
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Instancias Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Llamamos a los elementos del XML
        tilName  = findViewById(R.id.tilName)
        tilPhone = findViewById(R.id.tilPhone)
        tilDoc   = findViewById(R.id.tilDoc)
        tilEmail = findViewById(R.id.tilEmailReg)
        tilP1    = findViewById(R.id.tilPassReg)
        tilP2    = findViewById(R.id.tilPassReg2)

        etName  = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etDoc   = findViewById(R.id.etDoc)
        etEmail = findViewById(R.id.etEmailReg)
        etP1    = findViewById(R.id.etPassReg)
        etP2    = findViewById(R.id.etPassReg2)

        btnCreate = findViewById(R.id.btnCrearCuenta)
        btnCancel = findViewById(R.id.btnCancelar)

        // Crear cuenta
        btnCreate.setOnClickListener { createAccount() }

        // Cancelar (y confirmación si hay datos escritos)
        btnCancel.setOnClickListener { handleCancel() }
        onBackPressedDispatcher.addCallback(this) { handleCancel() }
    }

    // --- Crear cuenta completa ---
    private fun createAccount() {
        // 1) Limpia errores previos
        listOf(tilName, tilPhone, tilDoc, tilEmail, tilP1, tilP2)
            .forEach { it.error = null }

        // 2) Lee valores
        val name  = etName.text?.toString()?.trim().orEmpty()
        val phone = etPhone.text?.toString()?.trim().orEmpty()
        val doc   = etDoc.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val p1    = etP1.text?.toString()?.trim().orEmpty()
        val p2    = etP2.text?.toString()?.trim().orEmpty()

        // 3) Validaciones rápidas de cliente
        var ok = true
        if (name.length < 3) { tilName.error = "Nombre muy corto"; ok = false }
        if (phone.length < 7) { tilPhone.error = "Celular inválido"; ok = false }
        if (doc.isBlank())    { tilDoc.error = "Documento requerido"; ok = false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Correo inválido"; ok = false
        }
        if (p1.length < 6) { tilP1.error = "Mínimo 6 caracteres"; ok = false }
        if (p1 != p2)      { tilP2.error = "Las contraseñas no coinciden"; ok = false }
        if (!ok) return

        // Deshabilita botón para evitar toques repetidos
        btnCreate.isEnabled = false

        // 4) Crear usuario en Auth
        auth.createUserWithEmailAndPassword(email, p1)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    toast("No se pudo obtener el usuario")
                    btnCreate.isEnabled = true
                    return@addOnSuccessListener
                }

                // 5) Guardar datos extra en Firestore: /users/{uid}
                val data = hashMapOf(
                    "name" to name,
                    "phone" to phone,
                    "document" to doc,
                    "email" to email,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("users").document(uid).set(data)
                    .addOnSuccessListener {
                        // 6) Enviar verificación y cerrar
                        auth.currentUser?.sendEmailVerification()
                        toast("Cuenta creada. Revisa tu correo para verificar.")
                        finish() // volver a Login
                    }
                    .addOnFailureListener { e ->
                        toast(e.localizedMessage ?: "Error guardando datos")
                        btnCreate.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                toast(e.localizedMessage ?: "Error al registrar")
                btnCreate.isEnabled = true
            }
    }

    // --- Cancelar con confirmación si el formulario no está vacío ---
    private fun handleCancel() {
        if (isFormEmpty()) {
            finish()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Cancelar registro")
            .setMessage("Se perderán los datos ingresados. ¿Deseas salir?")
            .setNegativeButton("Seguir aquí", null)
            .setPositiveButton("Salir") { _, _ -> finish() }
            .show()
    }

    // ¿Está todo en blanco?
    private fun isFormEmpty(): Boolean =
        listOf(etName, etPhone, etDoc, etEmail, etP1, etP2)
            .all { it.text.isNullOrBlank() }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

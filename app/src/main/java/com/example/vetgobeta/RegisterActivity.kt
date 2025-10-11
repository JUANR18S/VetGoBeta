package com.example.vetgobeta

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
 * 4) (DEFERIDO) Guardar datos extra en Firestore al primer login verificado.
 * 5) Enviamos email de verificación y volvemos al Login.
 * Botón "Cancelar": confirma si hay datos escritos y sale.
 *
 * 🔐 Importante:
 * - NO permitimos entrar si !emailVerified (se controla en LoginActivity).
 * - Las reglas de Firestore deben usar request.auth.token.email_verified == true.
 */
class RegisterActivity : AppCompatActivity() {

    // --- UI ---
    private lateinit var tilName: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilDoc: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilP1: TextInputLayout
    private lateinit var tilP2: TextInputLayout

    private val CHANNEL_ID = "vetgo_general" // Define el ID del canal

    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etDoc: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etP1: TextInputEditText
    private lateinit var etP2: TextInputEditText
    private lateinit var btnCreate: Button
    private lateinit var btnCancel: Button
    private lateinit var btnSignIn: Button

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
        tilName = findViewById(R.id.tilName)
        tilPhone = findViewById(R.id.tilPhone)
        tilDoc = findViewById(R.id.tilDoc)
        tilEmail = findViewById(R.id.tilEmailReg)
        tilP1 = findViewById(R.id.tilPassReg)
        tilP2 = findViewById(R.id.tilPassReg2)

        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etDoc = findViewById(R.id.etDoc)
        etEmail = findViewById(R.id.etEmailReg)
        etP1 = findViewById(R.id.etPassReg)
        etP2 = findViewById(R.id.etPassReg2)

        btnCreate = findViewById(R.id.btnCrearCuenta)
        btnCancel = findViewById(R.id.btnCancelar)
        btnSignIn = findViewById(R.id.btnSignIn)

        btnSignIn.setOnClickListener {
            val i = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(i)
            finish()
        }

        // Crear cuenta
        btnCreate.setOnClickListener { createAccount() }

        // Cancelar (y confirmación si hay datos escritos)
        btnCancel.setOnClickListener { handleCancel() }
        onBackPressedDispatcher.addCallback(this) { handleCancel() }

        // Crear canal de notificación
        createNotificationChannel()
    }


    // --- Crear cuenta completa ---
    private fun createAccount() {
        // 1) Limpia errores previos
        listOf(tilName, tilPhone, tilDoc, tilEmail, tilP1, tilP2)
            .forEach { it.error = null }

        // 2) Lee valores
        val name = etName.text?.toString()?.trim().orEmpty()
        val phone = etPhone.text?.toString()?.trim().orEmpty()
        val doc = etDoc.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val p1 = etP1.text?.toString()?.trim().orEmpty()
        val p2 = etP2.text?.toString()?.trim().orEmpty()

        // 3) Validaciones rápidas de cliente
        var ok = true
        if (name.length < 3) {
            tilName.error = "Nombre muy corto"; ok = false
        }
        if (phone.length < 7) {
            tilPhone.error = "Celular inválido"; ok = false
        }
        if (doc.isBlank()) {
            tilDoc.error = "Documento requerido"; ok = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Correo inválido"; ok = false
        }
        // 🔧 CHANGE: subir mínimo de 6 → 8 para mejor seguridad
        if (p1.length < 8) {
            tilP1.error = "Mínimo 8 caracteres"; ok = false
        }
        if (p1 != p2) {
            tilP2.error = "Las contraseñas no coinciden"; ok = false
        }
        if (!ok) return

        // Deshabilita botón para evitar toques repetidos
        btnCreate.isEnabled = false
        btnCreate.isPressed = false
        btnCreate.isSelected = false

        // 4) Crear usuario en Auth
        auth.createUserWithEmailAndPassword(email, p1)
            .addOnSuccessListener { result ->
                // 🔧 CHANGE: no intentamos escribir en Firestore aquí si rules requieren email verificado.
                // Deferimos el guardado a primer login verificado (en LoginActivity).
                // Si quisieras crear "pending_users", aquí sería el sitio.

                auth.currentUser?.sendEmailVerification()
                    ?.addOnCompleteListener {
                        toast("Cuenta creada. Revisa tu correo para verificar.")
                        // Notificación local
                        notifySuccess()
                        // 🔧 CHANGE: cerramos sesión para forzar verificación antes del primer login
                        auth.signOut()

                        // Mostrar botón "Iniciar sesión" y bloquear "Crear cuenta"
                        btnSignIn.visibility = View.VISIBLE
                        btnSignIn.isEnabled = true
                        btnCreate.isEnabled = false

                        // 🔧 KEEP: no hacemos finish() para permitir que el usuario toque "Iniciar sesión"
                    }
            }
            // 🔧 CHANGE: tenías dos addOnFailureListener; dejamos UNO claro y con mensaje consistente
            .addOnFailureListener { e ->
                toast(e.localizedMessage ?: "Error al registrar")
                btnCreate.isEnabled = true
            }
    }

    // --- Cancelar con confirmación si el formulario no está vacío ---
    private fun handleCancel() {
        if (isFormEmpty()) {
            // Si está vacío, solo vuelve al login y fuerza a quedarse ahí
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Cancelar registro")
            .setMessage("Se perderán los datos ingresados. ¿Deseas salir?")
            .setNegativeButton("Seguir aquí", null)
            .setPositiveButton("Salir") { _, _ ->
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                finish()
            }
            .show()
    }

    // ¿Está todo en blanco?
    private fun isFormEmpty(): Boolean =
        listOf(etName, etPhone, etDoc, etEmail, etP1, etP2)
            .all { it.text.isNullOrBlank() }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun notifySuccess() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pi = PendingIntent.getActivity(
            this,                                   // context
            0,                                      // requestCode
            intent,                                 // intent
            PendingIntent.FLAG_UPDATE_CURRENT or    // flags_
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, "vetgo_general")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("VetGo")
            .setContentText("Usuario creado con éxito.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        // Verifica el permiso de notificación antes de mostrarla
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(1001, notif)
        } else {
            // Solicita el permiso si no está concedido
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1002 // Código de solicitud para identificar esta solicitud
            )
            // Opcionalmente, puedes informar al usuario que necesita conceder el permiso.
            toast("Por favor, activa las notificaciones para recibir alertas importantes.")
        }
    }

    // --- Crear Canal de Notificación (Obligatorio para Android 8.0 Oreo y superior) ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "VetGo General" // Nombre del canal visible para el usuario
            val descriptionText = "Notificaciones generales de VetGo" // Descripción del canal
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply { // Usa el CHANNEL_ID
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Maneja el resultado de la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002) { // Comprueba si es la solicitud de permiso de notificación
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permiso concedido, intenta mostrar la notificación de nuevo
                notifySuccess()
            } else {
                // Permiso denegado
                toast("No se podrán mostrar notificaciones importantes sin el permiso.")
            }
        }
    }
}

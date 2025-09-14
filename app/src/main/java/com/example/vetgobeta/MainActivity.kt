package com.example.vetgobeta

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import android.view.View


class MainActivity : AppCompatActivity() {

    // 1) Referencias XML
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPass:  TextInputLayout
    private lateinit var etEmail:  TextInputEditText
    private lateinit var etPass:   TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var tvForgot:   TextView

    // 2) ACCESO A FIREBASE (OK)
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.main)
        root?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
        /*

         */
        // 3) INSTANCIA DE FIREBASE Auth (OK)
        auth = FirebaseAuth.getInstance()

        // 4) VINCULACIÓN DE REFERENCIAS XML (OK)
        tilEmail   = findViewById(R.id.tilEmail)
        tilPass    = findViewById(R.id.tilPass)
        etEmail    = findViewById(R.id.etEmail)
        etPass     = findViewById(R.id.etPass)
        btnLogin   = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgot   = findViewById(R.id.tvForgot)   // <- Recien creada✔️

        // 5) INICIO DE SESIÓN📝
        btnLogin.setOnClickListener {

            // 5.1 LIMPIAR ERRORES🆑
            tilEmail.error = null
            tilPass.error  = null

            // 5.2 Toma los valores ingresados por el usuario
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass  = etPass.text?.toString()?.trim().orEmpty()

            // 5.3 Validaciones mínimas en cliente
            var ok = true
            if (email.isBlank()) {
                tilEmail.error = "Ingresa tu email"; ok = false }
            if (pass.isBlank())  {
                tilPass.error  = "Ingresa tu contraseña"; ok = false }
            if (!ok) return@setOnClickListener

            // 5.4 Llama a Firebase (asincronico)
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    // 5.5 Navega del mapa (pantalla principal)
                    startActivity(Intent(
                        this, Maps_Activity::class.java))
                    finish() // evita volver al login con Back
                }

                .addOnFailureListener { e ->
                    // 5.6 Muestra el motivo (credenciales inválidas, usuario no existe, etc.)
                    Toast.makeText(
                        this,
                        e.localizedMessage ?: "No se pudo iniciar sesión",
                        Toast.LENGTH_SHORT).show()
                }
        }

        // 6) Ir a Registro (pantalla para crear cuenta)
        tvRegister.setOnClickListener {
            startActivity(Intent(this,
                RegisterActivity::class.java))
        }

        // 7) Restablecer contraseña (enlace por correo)
        tvForgot.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            if (email.isBlank()) {
                Toast.makeText(this,
                    "Escribe tu email para enviarte el enlace",
                    Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this,
                            "Te enviamos un enlace para restablecer",
                            Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this,
                            e.localizedMessage ?: "No se pudo enviar el correo",
                            Toast.LENGTH_SHORT).show()
                    }
            }
        }

        //  Ajuste visual edge-to-edge si tu raíz tiene id @id/main
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()


        // 8) Auto-login: si ya hay sesión, entra directo al mapa
        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, Maps_Activity::class.java))
            finish()
        }
    }
}

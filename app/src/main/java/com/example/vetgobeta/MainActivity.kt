package com.example.vetgobeta

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts   // 🔵 GOOGLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.view.View

// 🔵 GOOGLE - imports
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPass:  TextInputLayout
    private lateinit var etEmail:  TextInputEditText
    private lateinit var etPass:   TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var tvForgot:   TextView
    private lateinit var btnGoogle:  SignInButton // 🔵 GOOGLE

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // 🔵 GOOGLE - launcher moderno (en vez de onActivityResult)
    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this) { authTask ->
                    if (authTask.isSuccessful) {
                        // Con Google el correo ya viene verificado → pasa las reglas de Firestore
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            // crear /users/{uid} si no existe (idempotente)
                            val uid = user.uid
                            val userDoc = db.collection("users").document(uid)
                            userDoc.get().addOnSuccessListener { snap ->
                                if (!snap.exists()) {
                                    val data = mapOf(
                                        "email" to user.email,
                                        "provider" to "google",
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                    userDoc.set(data)
                                }
                            }
                        }
                        startActivity(Intent(this, Maps_Activity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Error al iniciar con Google", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: ApiException) {
            Toast.makeText(this, "Fallo Google: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // --- Logo "VetGo" coloreado ---
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val fullText = getString(R.string.title)
        val spannable = SpannableStringBuilder(fullText)
        val greenColor = ContextCompat.getColor(this, R.color.green)
        val startIndex = fullText.indexOf("Go")
        if (startIndex != -1) {
            spannable.setSpan(
                ForegroundColorSpan(greenColor),
                startIndex,
                startIndex + "Go".length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        tvTitle.text = spannable

        val root = findViewById<View>(R.id.main)
        root?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- UI ---
        tilEmail   = findViewById(R.id.tilEmail)
        tilPass    = findViewById(R.id.tilPass)
        etEmail    = findViewById(R.id.etEmail)
        etPass     = findViewById(R.id.etPass)
        btnLogin   = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgot   = findViewById(R.id.tvForgot)
        btnGoogle  = findViewById(R.id.btnGoogleSignIn) // 🔵 GOOGLE

        // 🔵 GOOGLE - configuración del cliente
        // Usa el client ID que viene en google-services.json como default_web_client_id
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, gso)

        btnGoogle.setOnClickListener {
            googleLauncher.launch(googleClient.signInIntent)
        }

        // 🔑 LOGIN Email/Password (tu flujo original)
        btnLogin.setOnClickListener {
            tilEmail.error = null
            tilPass.error  = null

            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass  = etPass.text?.toString()?.trim().orEmpty()

            var ok = true
            if (email.isBlank()) { tilEmail.error = "Ingresa tu email"; ok = false }
            if (pass.isBlank())  { tilPass.error  = "Ingresa tu contraseña"; ok = false }
            if (!ok) return@setOnClickListener

            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    val user = it.user
                    if (user != null && user.isEmailVerified) {
                        // crea documento /users/{uid} si aún no existe
                        val uid = user.uid
                        val userDoc = db.collection("users").document(uid)
                        userDoc.get().addOnSuccessListener { snap ->
                            if (!snap.exists()) {
                                val data = mapOf(
                                    "email" to user.email,
                                    "provider" to "password",
                                    "createdAt" to System.currentTimeMillis()
                                )
                                userDoc.set(data)
                            }
                        }
                        startActivity(Intent(this, Maps_Activity::class.java))
                        finish()
                    } else {
                        auth.signOut()
                        Toast.makeText(this, "Verifica tu correo antes de ingresar.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.localizedMessage ?: "No se pudo iniciar sesión", Toast.LENGTH_SHORT).show()
                }
        }

        // Ir al registro
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Restablecer contraseña
        tvForgot.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            if (email.isBlank()) {
                Toast.makeText(this, "Escribe tu email para enviarte el enlace", Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Te enviamos un enlace para restablecer", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, e.localizedMessage ?: "No se pudo enviar el correo", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Auto-login solo si correo verificado (para password)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && (currentUser.isEmailVerified || currentUser.providerData.any { it.providerId == "google.com" })) {
            // Si es Google, no necesita emailVerified (ya viene verificado por Google)
            startActivity(Intent(this, Maps_Activity::class.java))
            finish()
        } else if (currentUser != null) {
            FirebaseAuth.getInstance().signOut()
        }
    }
}


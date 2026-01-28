package com.example.vetgobeta

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import com.example.vetgobeta.ui.CitaInmediataBottomSheet
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class Maps_Activity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val googleClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // ---------- MAPA ----------
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // ---------- FAB (menú flotante) ----------
        val fab = findViewById<FloatingActionButton>(R.id.btnMenuNav)
        // Garantiza que quede por encima del fragment del mapa
        fab.bringToFront()
        ViewCompat.setElevation(fab, 16f)

        fab.setOnClickListener { anchor ->
            Toast.makeText(this, "Abriendo menú.", Toast.LENGTH_SHORT).show()

            val popup = PopupMenu(this, anchor)
            popup.menuInflater.inflate(R.menu.menu_maps, popup.menu)

            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.Mi_perfil -> {
                        // TODO: startActivity(Intent(this, PerfilActivity::class.java))
                        Toast.makeText(this, "Perfil", Toast.LENGTH_SHORT).show(); true
                    }
                    R.id.Mis_mascotas -> {
                        // TODO: startActivity(Intent(this, MisMascotasActivity::class.java))
                        Toast.makeText(this, "Mis Mascotas", Toast.LENGTH_SHORT).show(); true
                    }
                    R.id.VetWallet -> {
                        // TODO: startActivity(Intent(this, VetWalletActivity::class.java))
                        Toast.makeText(this, "VetWallet", Toast.LENGTH_SHORT).show(); true
                    }
                    R.id.Directorio -> {
                        // TODO: startActivity(Intent(this, DirectorioActivity::class.java))
                        Toast.makeText(this, "Directorio", Toast.LENGTH_SHORT).show(); true
                    }
                    R.id.VetShop -> {
// TODO: startActivity(Intent(this, VetShopActivity::cl
                        Toast.makeText(this, "VetShop", Toast.LENGTH_SHORT).show(); true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // ---------- Botón "Cita Inmediata" ----------
        val btnCitaInmediata = findViewById<Button>(R.id.btnCitaInmediata)
        btnCitaInmediata.setOnClickListener {
            val bottomSheet = CitaInmediataBottomSheet()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        // ---------- Botón "Cerrar Sesión" ----------
        val btnCerrarSesion = findViewById<Button>(R.id.btnVolverLogin)
        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            googleClient.signOut().addOnCompleteListener {
                navigateToLogin()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        currentUser.reload().addOnCompleteListener {
            val refreshedUser = FirebaseAuth.getInstance().currentUser
            if (!isValidSession(refreshedUser)) {
                FirebaseAuth.getInstance().signOut()
                googleClient.signOut().addOnCompleteListener { navigateToLogin() }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Mover la cámara a una ubicación específica (Medellín, Colombia)
        val medellin = LatLng(6.2442, -75.5812)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(medellin, 12f))

        // Puedes añadir marcadores, polígonos, etc. aquí
    }

    private fun isValidSession(user: FirebaseUser?): Boolean {
        return user != null && (user.isEmailVerified || user.providerData.any { it.providerId == "google.com" })
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

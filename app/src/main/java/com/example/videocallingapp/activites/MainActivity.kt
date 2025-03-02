package com.example.videocallingapp.activites

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.videocallingapp.R
import com.example.videocallingapp.databinding.ActivityMainBinding
import com.example.videocallingapp.models.User
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    private var user: User? = null
    private var coins: Long = 0
    private val requestCode = 1
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val currentUser: FirebaseUser? = auth.currentUser

        currentUser?.let {
            database.reference.child("profiles")
                .child(it.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        progressDialog.dismiss()
                        user = snapshot.getValue(User::class.java)
                        coins = user?.coins ?: 0

                        binding.coins.text = "you have: $coins"

                        Glide.with(this@MainActivity)
                            .load(user?.profile)
                            .into(binding.profilePicture)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        progressDialog.dismiss()
                        Log.e("MainActivity", "Database error: ${error.message}")
                    }
                })
        } ?: run {
            progressDialog.dismiss()
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }

        binding.findButton.setOnClickListener {
            if (isPermissionsGranted()) {
                Log.d("MainActivity", "Permissions granted")
                if (coins > 5) {
                    coins -= 5
                    currentUser?.let {
                        database.reference.child("profiles")
                            .child(it.uid)
                            .child("coins")
                            .setValue(coins)
                    }
                    val intent = Intent(this, ConnectingActivity::class.java)
                    intent.putExtra("profile", user?.profile)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Insufficient Coins", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("MainActivity", "Permissions not granted")
                askPermissions()
            }
        }

        binding.rewardBtn.setOnClickListener {
            startActivity(Intent(this, RewardActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the Google Mobile Ads SDK on a background thread
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            MobileAds.initialize(this@MainActivity) {}
        }
    }

    private fun askPermissions() {
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    private fun isPermissionsGranted(): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false
        }
        return true
    }
}
package com.example.videocallingapp.activites

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.videocallingapp.R
import com.example.videocallingapp.databinding.ActivityConnectingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ConnectingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectingBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var isOkay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityConnectingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val profile = intent.getStringExtra("profile")
        Glide.with(this)
            .load(profile)
            .into(binding.profile)

        val username = auth.uid

        if (username != null) {
            database.reference.child("users")
                .orderByChild("status")
                .equalTo(0.0).limitToFirst(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.childrenCount > 0) {
                            isOkay = true
                            // Room Available
                            for (childSnap in snapshot.children) {
                                database.reference
                                    .child("users")
                                    .child(childSnap.key!!)
                                    .child("incoming")
                                    .setValue(username)
                                database.reference
                                    .child("users")
                                    .child(childSnap.key!!)
                                    .child("status")
                                    .setValue(1)
                                val intent = Intent(this@ConnectingActivity, CallActivity::class.java)
                                val incoming = childSnap.child("incoming").getValue(String::class.java)
                                val createdBy = childSnap.child("createdBy").getValue(String::class.java)
                                val isAvailable = childSnap.child("isAvailable").getValue(Boolean::class.java)
                                intent.putExtra("username", username)
                                intent.putExtra("incoming", incoming)
                                intent.putExtra("createdBy", createdBy)
                                intent.putExtra("isAvailable", isAvailable)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            // Room Not Available
                            val room = hashMapOf(
                                "incoming" to username,
                                "createdBy" to username,
                                "isAvailable" to true,
                                "status" to 0
                            )

                            database.reference
                                .child("users")
                                .child(username)
                                .setValue(room).addOnSuccessListener {
                                    database.reference
                                        .child("users")
                                        .child(username).addValueEventListener(object : ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                if (snapshot.child("status").exists()) {
                                                    if (snapshot.child("status").getValue(Int::class.java) == 1) {
                                                        if (isOkay) return

                                                        isOkay = true
                                                        val intent = Intent(this@ConnectingActivity, CallActivity::class.java)
                                                        val incoming = snapshot.child("incoming").getValue(String::class.java)
                                                        val createdBy = snapshot.child("createdBy").getValue(String::class.java)
                                                        val isAvailable = snapshot.child("isAvailable").getValue(Boolean::class.java)
                                                        intent.putExtra("username", username)
                                                        intent.putExtra("incoming", incoming)
                                                        intent.putExtra("createdBy", createdBy)
                                                        intent.putExtra("isAvailable", isAvailable)
                                                        startActivity(intent)
                                                        finish()
                                                    }
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                // Handle error
                                            }
                                        })
                                }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
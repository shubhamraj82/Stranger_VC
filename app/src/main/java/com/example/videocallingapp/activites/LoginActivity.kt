package com.example.videocallingapp.activites

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.videocallingapp.R
import com.example.videocallingapp.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 11
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var loginBtn: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        if (mAuth.currentUser != null) {
            goToNextActivity()
        }

        database = FirebaseDatabase.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        loginBtn = findViewById(R.id.loginBtn)
        loginBtn.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun goToNextActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result
            if (account != null) {
                authWithGoogle(account.idToken!!)
            } else {
                Log.e("err", "Google Sign-In failed.")
            }
        }
    }

    private fun authWithGoogle(idToken: String) {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = mAuth.currentUser
                    val firebaseUser = user?.let {
                        User(it.uid, it.displayName ?: "", it.photoUrl.toString(), "Unknown", 500)
                    }
                    if (firebaseUser != null) {
                        database.reference
                            .child("profiles")
                            .child(firebaseUser.uId)
                            .setValue(firebaseUser)
                            .addOnCompleteListener { task: Task<Void> ->
                                if (task.isSuccessful) {
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finishAffinity()
                                } else {
                                    Toast.makeText(this, task.exception!!.localizedMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    Log.e("err", task.exception!!.localizedMessage!!)
                }
            }
    }
}
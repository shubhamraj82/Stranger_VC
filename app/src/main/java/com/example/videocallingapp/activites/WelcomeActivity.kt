package com.example.videocallingapp.activites
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.videocallingapp.R
import com.google.firebase.auth.FirebaseAuth


class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)

        auth=FirebaseAuth.getInstance()
        if(auth.currentUser!=null){
            goToNextActivity()
        }

        findViewById<View>(R.id.get_started).setOnClickListener {
            goToNextActivity()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun goToNextActivity() {
        startActivity(Intent(this,LoginActivity::class.java))
    }
}
package com.example.videocallingapp.activites

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.videocallingapp.R
import com.example.videocallingapp.databinding.ActivityCallBinding
import com.example.videocallingapp.models.InterfaceJava
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.videocallingapp.models.User
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private var uniqueId=""
    private lateinit var auth: FirebaseAuth
    private var username=""
    private var friendsUsername=""
    private var isPeerConnected=false
    private lateinit var firebaseRef:DatabaseReference
    private var isAudio = true
    private var isVideo = true
    private var createdBy: String? = null
    private var pageExit = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_call)

        binding=ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth=FirebaseAuth.getInstance()
        firebaseRef=FirebaseDatabase.getInstance().getReference("users")

        username=intent.getStringExtra("username")?:""
        val incoming=intent.getStringExtra("incoming")?:""
        createdBy=intent.getStringExtra("createdBy")

        friendsUsername=incoming

        setupWebView()

        binding.micBtn.setOnClickListener{
            isAudio=!isAudio
            callJavaScriptFunction("javascript:toggleAudio(\"$isAudio\")")
            binding.micBtn.setImageResource(if(isAudio)R.drawable.btn_unmute_normal else R.drawable.btn_mute_normal)
        }

        binding.videoBtn.setOnClickListener {
            isVideo=!isVideo
            callJavaScriptFunction("javascript:toggleVideo(\"$isVideo\")")
            binding.videoBtn.setImageResource(if(isVideo)R.drawable.btn_video_normal else R.drawable.btn_video_muted)
        }

        binding.endCall.setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupWebView() {
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.resources)
                }
            }
        }
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false
        binding.webView.addJavascriptInterface(InterfaceJava(this), "Android")

        loadVideoCall()
    }

    private fun loadVideoCall() {
        val filePath = "file:android_asset/call.html"
        binding.webView.loadUrl(filePath)

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                initializePeer()
            }
        }
    }

    private fun initializePeer(){
        uniqueId= getUniqueid()

        callJavaScriptFunction("javascript:init(\"$uniqueId\")")

        if(createdBy.equals(username, ignoreCase = true)){
            if(pageExit) return
            firebaseRef.child(username).child("connId").setValue(uniqueId)
            firebaseRef.child(username).child("isAvailable").setValue(true)

            binding.loadingGroup.visibility= View.GONE
            binding.controls.visibility= View.VISIBLE

            FirebaseDatabase.getInstance().getReference("profiles")
                .child(friendsUsername)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            Glide.with(this@CallActivity).load(it.profile).into(binding.profile)
                            binding.name.text = it.name
                            binding.city.text = it.city
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }else {
            Handler().postDelayed({
                friendsUsername = createdBy ?: ""
                FirebaseDatabase.getInstance().getReference("profiles")
                    .child(friendsUsername)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(User::class.java)
                            user?.let {
                                Glide.with(this@CallActivity).load(it.profile).into(binding.profile)
                                binding.name.text = it.name
                                binding.city.text = it.city
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                FirebaseDatabase.getInstance().getReference("users")
                    .child(friendsUsername)
                    .child("connId")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.value != null) {
                                sendCallRequest()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }, 3000)
        }
    }

    fun onPeerConnected() {
        isPeerConnected = true
    }

    private fun sendCallRequest(){
        if(!isPeerConnected){
            Toast.makeText(this, "You are not connected. Please check your internet.", Toast.LENGTH_SHORT).show()
            return
        }
        listenConnId()
    }

    private fun listenConnId() {
        firebaseRef.child(friendsUsername).child("connId").addValueEventListener(object :
            ValueEventListener {
             override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == null) return

                binding.loadingGroup.visibility = View.GONE
                binding.controls.visibility = View.VISIBLE
                val connId = snapshot.getValue(String::class.java)
                callJavaScriptFunction("javascript:startCall(\"$connId\")")
            }

             override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun callJavaScriptFunction(function: String) {
        binding.webView.post { binding.webView.evaluateJavascript(function, null) }
    }

   private fun getUniqueid(): String {
       return UUID.randomUUID().toString()
   }

    override fun onDestroy() {
        super.onDestroy()
        pageExit = true
        firebaseRef.child(createdBy ?: "").setValue(null)
        finish()
    }
}
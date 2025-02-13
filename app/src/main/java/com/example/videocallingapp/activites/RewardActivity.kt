package com.example.videocallingapp.activites

import android.app.Activity
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.videocallingapp.R
import com.example.videocallingapp.databinding.ActivityRewardBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RewardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardBinding
    private var mRewardedAd: RewardedAd? = null
    private lateinit var database: FirebaseDatabase
    private lateinit var currentUid: String
    private var coins = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reward)

        binding = ActivityRewardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        currentUid = FirebaseAuth.getInstance().uid.toString()
        loadAd()

        database.reference.child("profiles")
            .child(currentUid)
            .child("coins")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    coins = snapshot.getValue(Int::class.java) ?: 0
                    binding.coins.text = coins.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })

        binding.video1.setOnClickListener {
            mRewardedAd?.let { ad ->
                val activityContext: Activity = this@RewardActivity
                ad.show(activityContext, OnUserEarnedRewardListener { rewardItem: RewardItem ->
                    loadAd()
                    coins += 200
                    database.reference.child("profiles")
                        .child(currentUid)
                        .child("coins")
                        .setValue(coins)
                    binding.video1Icon.setImageResource(R.drawable.check)
                })
            } ?: run {
                // Handle the case when the ad is not loaded
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                // Handle the error
                mRewardedAd = null
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
            }
        })
    }
}
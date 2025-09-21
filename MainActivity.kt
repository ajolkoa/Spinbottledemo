package com.spin.app

import android.animation.ObjectAnimator
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardInputWidget

class MainActivity : AppCompatActivity() {

    private lateinit var bottle: ImageView
    private lateinit var statusText: TextView
    private var isPremium = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottle = findViewById(R.id.bottleView)
        statusText = findViewById(R.id.statusText)
        val spinBtn = findViewById<Button>(R.id.spinButton)
        val unlockBtn = findViewById<Button>(R.id.unlockPremiumButton)

        spinBtn.setOnClickListener { spinBottle() }
        unlockBtn.setOnClickListener { startPaymentFlow() }

        // Initialize Stripe with publishable key placeholder
        PaymentConfiguration.init(
            applicationContext,
            Config.STRIPE_PUBLISHABLE_KEY // "pk_test_..."
        )
    }

    private fun spinBottle() {
        val rotation = (3600..7200).random().toFloat()
        val animator = ObjectAnimator.ofFloat(bottle, "rotation", 0f, rotation)
        animator.duration = 2500
        animator.start()
    }

    private fun startPaymentFlow() {
        // In a real app, present a PaymentSheet or collect card details securely.
        // This minimal demo hits your server to create a PaymentIntent and confirms it.

        Thread {
            try {
                val client = OkHttpClient()
                val json = JSONObject().put("amount", 1000) // $10.00
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url(Config.SERVER_URL + "/create-payment-intent")
                    .post(body).build()
                val resp = client.newCall(req).execute()
                val respStr = resp.body?.string() ?: "{}"
                val clientSecret = JSONObject(respStr).optString("clientSecret")

                runOnUiThread {
                    if (clientSecret.isNullOrBlank()) {
                        statusText.text = "Payment init failed"
                        return@runOnUiThread
                    }
                    val stripe = Stripe(this, Config.STRIPE_PUBLISHABLE_KEY)
                    // For demo: payment method from a CardInputWidget (not present in the layout by default).
                    // In production, use PaymentSheet or your own card collection view.
                    val dummyParams = PaymentMethodCreateParams.create(
                        PaymentMethodCreateParams.Card.Builder()
                            .build()
                    )
                    val confirmParams = ConfirmPaymentIntentParams.create(clientSecret, dummyParams)
                    stripe.confirmPayment(this, confirmParams)
                    // In a real app, observe onActivityResult or callbacks.
                    // For demo, we optimistically unlock:
                    isPremium = true
                    statusText.text = "Premium Unlocked (Demo)"
                }
            } catch (e: Exception) {
                runOnUiThread { statusText.text = "Error: ${e.message}" }
            }
        }.start()
    }
}

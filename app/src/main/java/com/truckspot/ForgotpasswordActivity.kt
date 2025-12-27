package com.truckspot

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.truckspot.databinding.ActivityForgotpasswordBinding

class ForgotpasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotpasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding: ActivityForgotpasswordBinding = ActivityForgotpasswordBinding.inflate(layoutInflater)


        setContentView(binding.root)

        binding.back.setOnClickListener {
            finish()
        }

        binding.submit.setOnClickListener {
                if (binding.forgotpassmain.visibility == View.VISIBLE){
                    if (binding.etEmailid.text.toString().isNullOrEmpty()) {
                        Toast.makeText(this, "email id cannot be empty ", Toast.LENGTH_SHORT).show()
                    }
                    else
                        binding.mainpasswordsent.visibility  = View.VISIBLE
                    binding.forgotpassmain.visibility  = View.GONE
                }
                else{
                    val int = Intent(this, HomeActivity::class.java)
                    startActivity(int)
                }




        }
    }
}
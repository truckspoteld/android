package com.eagleye.eld

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eagleye.eld.databinding.ActivityUploadDocumentsBinding

class UploadDocumentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadDocumentsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadDocumentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Upload Documents"

        // Upload Button Click (no API - UI only)
        binding.btnUploadLicense.setOnClickListener {
            Toast.makeText(this, "Driver License selected (Demo)", Toast.LENGTH_SHORT).show()
        }
        binding.btnUploadRegistration.setOnClickListener {
            Toast.makeText(this, "Vehicle Registration selected (Demo)", Toast.LENGTH_SHORT).show()
        }
        binding.btnUploadInsurance.setOnClickListener {
            Toast.makeText(this, "Insurance Document selected (Demo)", Toast.LENGTH_SHORT).show()
        }
        binding.btnUploadMedical.setOnClickListener {
            Toast.makeText(this, "Medical Certificate selected (Demo)", Toast.LENGTH_SHORT).show()
        }
        binding.btnSubmit.setOnClickListener {
            Toast.makeText(this, "Document submission requires backend integration", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

package com.joeycarlson.qrscanner.export

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.joeycarlson.qrscanner.databinding.ActivityExportMethodBinding
import java.time.LocalDate

class ExportMethodActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityExportMethodBinding
    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportMethodBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get dates from intent
        startDate = LocalDate.parse(intent.getStringExtra("start_date"))
        endDate = LocalDate.parse(intent.getStringExtra("end_date"))
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Export Method"
        
        // Set up export methods list
        setupExportMethods()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupExportMethods() {
        val exportMethods = listOf(
            ExportMethod(
                "Save to Downloads",
                "Save JSON files to your device's Downloads folder",
                "📁",
                true
            ),
            ExportMethod(
                "Share via Android",
                "Share files using any installed app (email, messaging, cloud)",
                "📤",
                true
            ),
            ExportMethod(
                "Email",
                "Send files as email attachments",
                "📧",
                false
            ),
            ExportMethod(
                "SMS/Text",
                "Send file links via text message",
                "💬",
                false
            ),
            ExportMethod(
                "Slack",
                "Upload to Slack channel or DM",
                "🔗",
                false
            ),
            ExportMethod(
                "S3 Bucket",
                "Upload directly to AWS S3",
                "☁️",
                false
            ),
            ExportMethod(
                "Google Drive",
                "Save to Google Drive",
                "📊",
                false
            ),
            ExportMethod(
                "Dropbox",
                "Save to Dropbox",
                "📦",
                false
            )
        )
        
        val adapter = ExportMethodAdapter(exportMethods) { method ->
            handleExportMethodClick(method)
        }
        
        binding.exportMethodsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.exportMethodsRecyclerView.adapter = adapter
    }
    
    private fun handleExportMethodClick(method: ExportMethod) {
        when (method.name) {
            "Save to Downloads" -> {
                // TODO: Implement local file export
                Toast.makeText(this, "Save to Downloads - Coming soon!", Toast.LENGTH_SHORT).show()
            }
            "Share via Android" -> {
                // TODO: Implement Android share
                Toast.makeText(this, "Share via Android - Coming soon!", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "${method.name} - Coming in future update", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class ExportMethod(
    val name: String,
    val description: String,
    val icon: String,
    val isAvailable: Boolean
)

package com.example.emoji

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.emoji.databinding.ActivityMainBinding

/**
 * @author y.gladkikh
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as App).appComponent.inject(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}

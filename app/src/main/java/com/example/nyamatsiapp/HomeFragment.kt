package com.example.nyamatsiapp // Make sure this matches your package name!

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 1. Link to the XML layout
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // 2. Find the buttons
        val btnViewCleaning = view.findViewById<MaterialButton>(R.id.btnViewCleaning)
        val btnViewConstruction = view.findViewById<MaterialButton>(R.id.btnViewConstruction)

        // 3. Set click listeners to open the new detail pages
        btnViewCleaning.setOnClickListener {
            val intent = Intent(requireContext(), CleaningDetailsActivity::class.java)
            startActivity(intent)
        }

        btnViewConstruction.setOnClickListener {
            val intent = Intent(requireContext(), ConstructionDetailsActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}
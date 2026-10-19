package com.example.nyamatsiapp // Make sure this matches your package name!

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class ContactFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 1. Link to the XML layout
        val view = inflater.inflate(R.layout.fragment_contact, container, false)

        // 2. Find the cards using the new IDs
        val btnWhatsApp = view.findViewById<MaterialCardView>(R.id.btnWhatsApp)
        val btnPhone = view.findViewById<MaterialCardView>(R.id.btnPhone)
        val btnTikTok = view.findViewById<MaterialCardView>(R.id.btnTikTok)
        val btnGmail = view.findViewById<MaterialCardView>(R.id.btnGmail)

        // 3. Set up click listeners
        btnWhatsApp.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://wa.me/27790927401")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            }
        }

        btnPhone.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:+27790927401")
            startActivity(intent)
        }

        btnTikTok.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://www.tiktok.com/@nyamatsi.holdings")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not open TikTok", Toast.LENGTH_SHORT).show()
            }
        }

        btnGmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:nyamatsiholdings@gmail.com")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
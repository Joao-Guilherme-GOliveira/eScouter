package com.example.escouter.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNavigation = view.findViewById(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
                    true
                }

                R.id.nav_perfil -> {
                    findNavController().navigate(R.id.perfilFragment)
                    true
                }

                else -> false
            }
        }

        bottomNavigation.selectedItemId = R.id.nav_inicio
    }

    override fun onResume() {
        super.onResume()

        // Sempre que voltar para o Home,
        // marca o ícone de início como selecionado.
        if (::bottomNavigation.isInitialized) {
            bottomNavigation.selectedItemId = R.id.nav_inicio
        }
    }
}
package com.example.escouter.ui.home

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.model.Peneira
import com.example.escouter.model.Usuario
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var cardPeneira: View
    private lateinit var txtTime: TextView
    private lateinit var txtData: TextView
    private lateinit var btnCriarPeneira: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNavigation = view.findViewById(R.id.bottomNavigation)

        cardPeneira = view.findViewById(R.id.cardPeneira)
        txtTime = view.findViewById(R.id.txtTime)
        txtData = view.findViewById(R.id.txtData)
        btnCriarPeneira = view.findViewById(R.id.btnCriarPeneira)

        configurarBottomNavigation()
    }

    override fun onResume() {
        super.onResume()

        if (::bottomNavigation.isInitialized) {
            bottomNavigation.selectedItemId = R.id.nav_inicio
        }

        configurarTelaPorTipoDeUsuario()
    }

    private fun configurarBottomNavigation() {

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
                    true
                }

                R.id.nav_perfil -> {
                    findNavController().navigate(
                        R.id.perfilFragment
                    )
                    true
                }

                else -> false
            }
        }
    }

    private fun configurarTelaPorTipoDeUsuario() {

        val usuario = carregarUsuario()

        if (usuario == null) {
            Toast.makeText(
                requireContext(),
                "Usuário não encontrado.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val isClubeOuOlheiro =
            usuario.tipoUsuario.equals(
                "Clube/Olheiro",
                ignoreCase = true
            )

        if (isClubeOuOlheiro) {

            btnCriarPeneira.visibility = View.VISIBLE

            btnCriarPeneira.setOnClickListener {
                abrirCriarPeneira()
            }

        } else {

            btnCriarPeneira.visibility = View.GONE
        }

        exibirProximaPeneira()
    }

    private fun carregarUsuario(): Usuario? {

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = preferences.getString(
            "usuario",
            null
        ) ?: return null

        return try {
            Gson().fromJson(
                json,
                Usuario::class.java
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun carregarPeneiras(): List<Peneira> {

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = preferences.getString(
            "peneiras",
            null
        ) ?: return emptyList()

        return try {

            val tipoLista =
                object : TypeToken<List<Peneira>>() {}.type

            Gson().fromJson(
                json,
                tipoLista
            ) ?: emptyList()

        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun exibirProximaPeneira() {

        val peneiras = carregarPeneiras()

        if (peneiras.isEmpty()) {
            cardPeneira.visibility = View.GONE
            return
        }

        val formato = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )

        val hoje = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val proximaPeneira = peneiras
            .mapNotNull { peneira ->

                try {
                    val dataPeneira = formato.parse(peneira.data)

                    if (dataPeneira != null && !dataPeneira.before(hoje)) {
                        Pair(peneira, dataPeneira)
                    } else {
                        null
                    }

                } catch (e: Exception) {
                    null
                }
            }
            .minByOrNull { it.second }
            ?.first

        if (proximaPeneira == null) {
            cardPeneira.visibility = View.GONE
            return
        }

        cardPeneira.visibility = View.VISIBLE

        txtTime.text = proximaPeneira.nomeTime

        txtData.text =
            "${proximaPeneira.data} • " +
                    "${proximaPeneira.hora} • " +
                    proximaPeneira.local
    }

    private fun abrirCriarPeneira() {

        val dialog = CriarPeneiraFragment()

        dialog.onPeneiraCriada = {

            exibirProximaPeneira()
        }

        dialog.show(
            parentFragmentManager,
            "CriarPeneiraFragment"
        )
    }
}
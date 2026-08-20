package com.example.escouter.ui.home

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentPerfilBinding
import com.example.escouter.model.Midia
import com.example.escouter.model.Usuario
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPerfilBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        carregarUsuario()
        configurarBottomNavigation()
    }

    private fun carregarUsuario() {

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = preferences.getString("usuario", null)

        if (json == null) {
            return
        }

        val usuario = Gson().fromJson(
            json,
            Usuario::class.java
        )

        // =========================
        // DADOS PRINCIPAIS
        // =========================

        binding.txtNome.text = usuario.nome

        binding.txtPosicao.text =
            usuario.posicao

        binding.txtLocalizacao.text =
            "${usuario.cidade} - ${usuario.estado}"


        // =========================
        // INFORMAÇÕES
        // =========================

        val idade = calcularIdade(usuario.dataNascimento)
        binding.txtIdade.text =
            "$idade anos"

        binding.txtPeso.text =
            usuario.peso

        binding.txtAltura.text =
            usuario.altura

        binding.txtExperiencia.text =
            usuario.experiencia


        // =========================
        // MÍDIAS
        // =========================

        carregarMidias(usuario.midias)
    }

    private fun calcularIdade(dataNascimento: String): Int {

        return try {

            val formato = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            formato.isLenient = false

            val dataNascimentoDate = formato.parse(dataNascimento)
                ?: return 0

            val nascimento = Calendar.getInstance()
            nascimento.time = dataNascimentoDate

            val hoje = Calendar.getInstance()

            var idade = hoje.get(Calendar.YEAR) -
                    nascimento.get(Calendar.YEAR)

            if (
                hoje.get(Calendar.MONTH) < nascimento.get(Calendar.MONTH) ||
                (
                        hoje.get(Calendar.MONTH) == nascimento.get(Calendar.MONTH) &&
                                hoje.get(Calendar.DAY_OF_MONTH) < nascimento.get(Calendar.DAY_OF_MONTH)
                        )
            ) {
                idade--
            }

            idade

        } catch (e: Exception) {
            0
        }
    }

    private fun carregarMidias(midias: List<Midia>) {

        if (midias.isEmpty()) {

            binding.layoutMidias.visibility = View.GONE

            return
        }

        binding.layoutMidias.visibility = View.VISIBLE

        binding.gridMidias.removeAllViews()

        for (midia in midias) {

            val item = layoutInflater.inflate(
                R.layout.item_midia,
                binding.gridMidias,
                false
            )

            val imgMidia = item.findViewById<android.widget.ImageView>(
                R.id.imgMidia
            )

            val txtNome = item.findViewById<android.widget.TextView>(
                R.id.txtNomeMidia
            )

            val txtDuracao = item.findViewById<android.widget.TextView>(
                R.id.txtDuracaoMidia
            )

            txtNome.text = midia.nome
            txtDuracao.text = midia.duracao

            if (midia.uri.isNotEmpty()) {
                imgMidia.setImageURI(Uri.parse(midia.uri))
            }

            val params = android.widget.GridLayout.LayoutParams()

            params.width = 0
            params.height =
                android.widget.GridLayout.LayoutParams.WRAP_CONTENT

            params.columnSpec =
                android.widget.GridLayout.spec(
                    android.widget.GridLayout.UNDEFINED,
                    1f
                )

            item.layoutParams = params

            binding.gridMidias.addView(item)
        }
    }

    private fun configurarBottomNavigation() {

        val navController = findNavController()

        binding.bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {

                    navController.popBackStack()

                    true
                }

                R.id.nav_perfil -> {

                    true
                }

                else -> false
            }
        }

        // Deixa o ícone de perfil selecionado
        binding.bottomNavigation.selectedItemId =
            R.id.nav_perfil
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
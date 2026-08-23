package com.example.escouter.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentMinhasPeneirasBinding
import com.example.escouter.model.Peneira
import com.example.escouter.model.Usuario
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MinhasPeneirasFragment : Fragment() {

    private var _binding: FragmentMinhasPeneirasBinding? = null
    private val binding get() = _binding!!
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMinhasPeneirasBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomNavigation = view.findViewById(R.id.bottomNavigation)

        configurarBottomNavigation()


        carregarMinhasPeneiras()
    }
    private fun configurarBottomNavigation() {

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
                    findNavController().navigate(R.id.homeFragment)
                    true
                }

                R.id.nav_perfil -> {
                    findNavController().navigate(R.id.perfilFragment)
                    true
                }

                else -> false
            }
        }
    }

    private fun carregarMinhasPeneiras() {

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        // Pega o usuário atualmente logado
        val jsonUsuario = preferences.getString("usuario", null)

        if (jsonUsuario == null) {
            mostrarMensagem("Nenhum usuário encontrado.")
            return
        }

        val usuario = Gson().fromJson(
            jsonUsuario,
            Usuario::class.java
        )

        // Pega todas as peneiras salvas
        val jsonPeneiras = preferences.getString(
            "peneiras",
            null
        )

        if (jsonPeneiras == null) {
            mostrarMensagem("Você ainda não criou nenhuma peneira.")
            return
        }

        val tipoLista = object : TypeToken<List<Peneira>>() {}.type

        val todasPeneiras: List<Peneira> =
            Gson().fromJson(jsonPeneiras, tipoLista)
                ?: emptyList()

        // Filtra somente as peneiras desse clube
        val minhasPeneiras = todasPeneiras
            .filter {
                it.emailClube.equals(
                    usuario.email,
                    ignoreCase = true
                )
            }
            .reversed()

        if (minhasPeneiras.isEmpty()) {
            mostrarMensagem("Você ainda não criou nenhuma peneira.")
            return
        }

        binding.txtMensagem.visibility = View.GONE

        // Adiciona cada peneira na tela
        minhasPeneiras.forEach { peneira ->

            val item = layoutInflater.inflate(
                R.layout.item_minhas_peneiras,
                binding.containerPeneiras,
                false
            )

            val txtNomeTime = item.findViewById<TextView>(
                R.id.txtNomeTime
            )

            val txtData = item.findViewById<TextView>(
                R.id.txtData
            )

            val txtLocal = item.findViewById<TextView>(
                R.id.txtLocal
            )

            txtNomeTime.text = peneira.nomeTime

            txtData.text =
                "${peneira.data} • ${peneira.hora}"

            txtLocal.text = peneira.local

            binding.containerPeneiras.addView(item)
        }
    }

    private fun mostrarMensagem(mensagem: String) {

        binding.txtMensagem.visibility = View.VISIBLE
        binding.txtMensagem.text = mensagem
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
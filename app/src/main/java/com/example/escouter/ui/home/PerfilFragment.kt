package com.example.escouter.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentPerfilBinding
import com.example.escouter.model.Usuario
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

        binding.btnEditarPerfil.setOnClickListener {

            val preferences = requireContext().getSharedPreferences(
                "eScouter",
                Context.MODE_PRIVATE
            )

            val json = preferences.getString(
                "usuario",
                null
            )

            if (json == null) {
                return@setOnClickListener
            }

            val usuario = Gson().fromJson(
                json,
                Usuario::class.java
            )

            if (usuario.tipoUsuario.equals("Atleta", ignoreCase = true)) {

                findNavController().navigate(
                    R.id.action_perfilFragment_to_editarPerfilAtletaFragment
                )

            } else if (usuario.tipoUsuario.equals("Clube/Olheiro", ignoreCase = true)) {

                findNavController().navigate(
                    R.id.action_perfilFragment_to_editarPerfilClubeFragment
                )
            }
        }
    }

    private fun carregarUsuario() {

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = preferences.getString(
            "usuario",
            null
        ) ?: return

        val usuario = Gson().fromJson(
            json,
            Usuario::class.java
        )

        if (usuario.tipoUsuario.equals("Clube/Olheiro", ignoreCase = true)) {
            binding.txtPositionAge.visibility = View.GONE
            binding.cardInfo.visibility = View.GONE

        } else {
            binding.txtPositionAge.visibility = View.VISIBLE
            binding.cardInfo.visibility = View.VISIBLE
        }

        // =========================
        // NOME
        // =========================

        binding.txtName.text = usuario.nome

        // =========================
        // IDADE E POSIÇÃO
        // =========================

        val idade = calcularIdade(
            usuario.dataNascimento
        )

        binding.txtPositionAge.text =
            "${usuario.posicao} • $idade anos"

        // =========================
        // LOCALIZAÇÃO E DATA
        // =========================

        val anoCadastro = if (usuario.dataCadastro.isNotEmpty()) {
            usuario.dataCadastro.takeLast(4)
        } else {
            ""
        }

        binding.txtLocationDate.text =
            if (anoCadastro.isNotEmpty()) {
                "📍 ${usuario.cidade}, ${usuario.estado}     🗓 Desde $anoCadastro"
            } else {
                "📍 ${usuario.cidade}, ${usuario.estado}"
            }

        // =========================
        // DESCRIÇÃO
        // =========================

        if (usuario.descricao.isEmpty()) {

            binding.txtBio.visibility = View.GONE

        } else {

            binding.txtBio.visibility = View.VISIBLE
            binding.txtBio.text = usuario.descricao
        }

        // =========================
        // INFORMAÇÕES
        // =========================

        binding.statIdade.txtStatLabel.text = "Idade"
        binding.statIdade.txtStatValue.text =
            "$idade anos"

        binding.statPeso.txtStatLabel.text = "Peso"
        binding.statPeso.txtStatValue.text =
            usuario.peso

        binding.statAltura.txtStatLabel.text = "Altura"
        binding.statAltura.txtStatValue.text =
            usuario.altura

        binding.statExperiencia.txtStatLabel.text = "Experiência"
        binding.statExperiencia.txtStatValue.text =
            usuario.experiencia

        // =========================
        // MÍDIAS
        // =========================

        carregarMidias(usuario.midias)
    }

    private fun calcularIdade(dataNascimento: String): Int {

        if (dataNascimento.isBlank()) {
            return 0
        }

        val formatos = listOf(
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd"
        )

        for (padrao in formatos) {

            try {

                val formato = SimpleDateFormat(
                    padrao,
                    Locale.getDefault()
                )

                formato.isLenient = false

                val data = formato.parse(dataNascimento)
                    ?: continue

                val nascimento = Calendar.getInstance()
                nascimento.time = data

                val hoje = Calendar.getInstance()

                var idade =
                    hoje.get(Calendar.YEAR) -
                            nascimento.get(Calendar.YEAR)

                if (
                    hoje.get(Calendar.MONTH) <
                    nascimento.get(Calendar.MONTH)
                ) {

                    idade--

                } else if (
                    hoje.get(Calendar.MONTH) ==
                    nascimento.get(Calendar.MONTH) &&
                    hoje.get(Calendar.DAY_OF_MONTH) <
                    nascimento.get(Calendar.DAY_OF_MONTH)
                ) {

                    idade--
                }

                return idade

            } catch (_: Exception) {
                // tenta o próximo formato
            }
        }

        return 0
    }

    private fun carregarMidias(
        midias: List<com.example.escouter.model.Midia>
    ) {

        if (midias.isEmpty()) {

            binding.recyclerMedia.visibility =
                View.GONE

            return
        }

        binding.recyclerMedia.visibility =
            View.VISIBLE

        // O Adapter das mídias será configurado aqui futuramente.
    }

    private fun configurarBottomNavigation() {

        val navController = findNavController()

        binding.bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {

                    navController.navigate(
                        R.id.homeFragment
                    )

                    true
                }

                R.id.nav_perfil -> {

                    true
                }

                else -> false
            }
        }

        binding.bottomNavigation.selectedItemId =
            R.id.nav_perfil
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
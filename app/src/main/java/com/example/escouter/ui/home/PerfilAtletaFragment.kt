package com.example.escouter.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentPerfilAtletaBinding
import com.example.escouter.model.Usuario
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PerfilAtletaFragment : Fragment() {

    private var _binding: FragmentPerfilAtletaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPerfilAtletaBinding.inflate(
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
            findNavController().navigate(
                R.id.action_perfilAtletaFragment_to_editarPerfilAtletaFragment
            )
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

        // =========================
        // CABEÇALHO
        // =========================

        binding.txtName.text = usuario.nome

        val idade = calcularIdade(
            usuario.dataNascimento
        )

        binding.txtPositionAge.text =
            "${usuario.posicao} • $idade anos"

        val anoCadastro =
            usuario.dataCadastro.takeLast(4)

        binding.txtLocationDate.text =
            "📍 ${usuario.cidade}, ${usuario.estado}     🗓 Desde $anoCadastro"

        // =========================
        // BIO
        // =========================

        if (usuario.descricao.isNullOrEmpty()) {

            binding.txtBio.visibility = View.GONE

        } else {

            binding.txtBio.visibility = View.VISIBLE

            binding.txtBio.text =
                usuario.descricao
        }

        // =========================
        // INFORMAÇÕES
        // =========================

        binding.statIdade.txtValor.text =
            "$idade anos"

        binding.statPeso.txtValor.text =
            usuario.peso

        binding.statAltura.txtValor.text =
            usuario.altura

        binding.statExperiencia.txtValor.text =
            usuario.experiencia

        // =========================
        // FOTO
        // =========================

        if (!usuario.foto.isNullOrEmpty()) {

            try {
                binding.imgAvatar.setImageURI(
                    android.net.Uri.parse(usuario.foto)
                )
            } catch (e: Exception) {
                binding.imgAvatar.setImageResource(
                    R.drawable.ic_perfil
                )
            }
        }

        // =========================
        // MÍDIAS
        // =========================

        carregarMidias(usuario.midias)
    }

    private fun calcularIdade(
        dataNascimento: String
    ): Int {

        return try {

            val formato = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            formato.isLenient = false

            val dataNascimentoDate =
                formato.parse(dataNascimento)
                    ?: return 0

            val nascimento =
                Calendar.getInstance()

            nascimento.time =
                dataNascimentoDate

            val hoje =
                Calendar.getInstance()

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

            idade

        } catch (e: Exception) {

            0
        }
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

        /*
         * Aqui você vai colocar o Adapter
         * do RecyclerView quando formos
         * configurar as mídias.
         */
    }

    private fun configurarBottomNavigation() {

        val navController =
            findNavController()

        binding.bottomNavigation
            .setOnItemSelectedListener { item ->

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

        binding.bottomNavigation.selectedItemId =
            R.id.nav_perfil
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
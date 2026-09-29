package com.example.escouter.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentBuscaClubesBinding
import com.example.escouter.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore

class FragmentBuscaClubes : Fragment() {

    private var _binding: FragmentBuscaClubesBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBuscaClubesBinding.inflate(
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

        configurarListeners()

        // Carrega os clubes assim que abrir a tela
        pesquisarClubes()

        configurarBottomNavigation()
    }

    private fun configurarListeners() {

        // Pesquisa pelo nome, cidade ou estado
        binding.edtBusca.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                pesquisarClubes()
            }

            override fun afterTextChanged(
                s: Editable?
            ) {
            }
        })

        // Botão TODOS
        binding.btnTodos.setOnClickListener {

            findNavController().navigate(
                R.id.action_fragmentBuscaClubes_to_fragmentBuscaTodos
            )
        }

        // Botão ATLETAS

        // Botão CLUBES
        binding.btnClubes.setOnClickListener {

            pesquisarClubes()
        }
    }

    private fun configurarBottomNavigation() {

        binding.bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
                    findNavController().navigate(
                        R.id.homeFragment
                    )
                    true
                }

                R.id.nav_pesquisar -> {
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

    private fun pesquisarClubes() {

        // Pega o texto digitado
        val textoBusca = binding.edtBusca
            .text
            .toString()
            .trim()
            .lowercase()

        binding.progressBar.visibility = View.VISIBLE

        binding.txtMensagem.visibility = View.GONE

        binding.containerClubes.removeAllViews()

        // Busca somente clubes/olheiros
        db.collection("usuarios")
            .whereEqualTo(
                "tipoUsuario",
                "Clube/Olheiro"
            )
            .get()
            .addOnSuccessListener { resultado ->

                binding.progressBar.visibility = View.GONE

                val clubes = resultado.documents
                    .mapNotNull { documento ->

                        documento.toObject(
                            Usuario::class.java
                        )
                    }
                    .filter { clube ->

                        // Se a pesquisa estiver vazia,
                        // mostra todos os clubes.

                        if (textoBusca.isEmpty()) {

                            true

                        } else {

                            // Pesquisa por nome
                            // OU cidade
                            // OU estado

                            clube.nome
                                .lowercase()
                                .contains(textoBusca) ||

                                    clube.cidade
                                        .lowercase()
                                        .contains(textoBusca) ||

                                    clube.estado
                                        .lowercase()
                                        .contains(textoBusca)
                        }
                    }

                mostrarClubes(clubes)
            }
            .addOnFailureListener { erro ->

                binding.progressBar.visibility = View.GONE

                Toast.makeText(
                    requireContext(),
                    "Erro ao buscar clubes: ${erro.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarClubes(
        clubes: List<Usuario>
    ) {

        binding.containerClubes.removeAllViews()

        // Nenhum clube encontrado
        if (clubes.isEmpty()) {

            binding.txtMensagem.text =
                "Nenhum clube encontrado."

            binding.txtMensagem.visibility =
                View.VISIBLE

            return
        }

        binding.txtMensagem.visibility =
            View.GONE

        // Cria um card para cada clube
        clubes.forEach { clube ->

            val item = layoutInflater.inflate(
                R.layout.item_busca_atleta,
                binding.containerClubes,
                false
            )

            val txtNome = item.findViewById<TextView>(
                R.id.txtNomeAtleta
            )

            val txtPosicao = item.findViewById<TextView>(
                R.id.txtPosicaoAtleta
            )

            val btnVerPerfil = item.findViewById<Button>(
                R.id.btnVerPerfil
            )

            // Nome do clube
            txtNome.text = clube.nome

            // Cidade e estado
            txtPosicao.text =
                "${clube.cidade} - ${clube.estado}"

            btnVerPerfil.setOnClickListener {

                Toast.makeText(
                    requireContext(),
                    "Perfil de ${clube.nome}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            binding.containerClubes.addView(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
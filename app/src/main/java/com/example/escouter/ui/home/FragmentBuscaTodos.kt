package com.example.escouter.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.escouter.R
import com.example.escouter.databinding.FragmentBuscaTodosBinding
import com.example.escouter.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import android.text.Editable
import android.text.TextWatcher
import androidx.navigation.fragment.findNavController

class FragmentBuscaTodos : Fragment() {

    private var _binding: FragmentBuscaTodosBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    // Guarda a posição que está sendo filtrada.
    // null = todas as posições
    private var posicaoSelecionada: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBuscaTodosBinding.inflate(
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

        // Carrega os atletas assim que abrir a tela
        buscarAtletas()
        configurarBottomNavigation()
    }

    private fun configurarBottomNavigation() {

        binding.bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
                    findNavController().navigate(R.id.homeFragment)
                    true
                }

                R.id.nav_pesquisar -> {
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
    private fun configurarListeners() {

        // Botão TODOS
        binding.btnTodos.setOnClickListener {
            posicaoSelecionada = null
            buscarAtletas()
        }

        // Botão ATLETAS
        binding.btnAtletas.setOnClickListener {
            posicaoSelecionada = null
            buscarAtletas()
        }

        // Filtro DEFENSORES
        binding.cardDefensores.setOnClickListener {
            posicaoSelecionada = "Defensor"
            buscarAtletas()
        }

        // Filtro MEIO-CAMPO
        binding.cardMeioCampo.setOnClickListener {
            posicaoSelecionada = "Meio-campo"
            buscarAtletas()
        }

        // Filtro ATACANTES
        binding.cardAtacantes.setOnClickListener {
            posicaoSelecionada = "Atacante"
            buscarAtletas()
        }

        // Filtro GOLEIROS
        binding.cardGoleiros.setOnClickListener {
            posicaoSelecionada = "Goleiro"
            buscarAtletas()
        }

        // Pesquisa pelo nome
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
                buscarAtletas()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun buscarAtletas() {

        // Pega o texto digitado na pesquisa
        val textoBusca = binding.edtBusca
            .text
            .toString()
            .trim()
            .lowercase()

        // Busca somente usuários que são atletas
        var consulta = db.collection("usuarios")
            .whereEqualTo("tipoUsuario", "Atleta")

        // Se uma posição foi selecionada,
        // adiciona o filtro de posição
        if (posicaoSelecionada != null) {

            consulta = consulta.whereEqualTo(
                "posicao",
                posicaoSelecionada
            )
        }

        consulta
            .get()
            .addOnSuccessListener { resultado ->

                // Converte os documentos do Firestore
                // para objetos Usuario
                val atletas = resultado.documents
                    .mapNotNull { documento ->

                        documento.toObject(
                            Usuario::class.java
                        )
                    }
                    .filter { atleta ->

                        // Se a busca estiver vazia,
                        // mostra todos os atletas.

                        // Caso tenha texto,
                        // procura pelo nome.
                        textoBusca.isEmpty() ||
                                atleta.nome
                                    .lowercase()
                                    .contains(textoBusca)
                    }

                mostrarAtletas(atletas)
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao buscar atletas: ${erro.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarAtletas(
        atletas: List<Usuario>
    ) {

        // Limpa os resultados anteriores
        binding.containerAtletas.removeAllViews()

        // Se não encontrou ninguém
        if (atletas.isEmpty()) {

            val mensagem = android.widget.TextView(
                requireContext()
            )

            mensagem.text = "Nenhum atleta encontrado."
            mensagem.textSize = 16f
            mensagem.setTextColor(
                resources.getColor(
                    R.color.txtCinza,
                    requireContext().theme
                )
            )

            mensagem.setPadding(
                16,
                20,
                16,
                20
            )

            binding.containerAtletas.addView(
                mensagem
            )

            return
        }

        // Cria um card para cada atleta
        atletas.forEach { atleta ->

            val item = layoutInflater.inflate(
                R.layout.item_busca_atleta,
                binding.containerAtletas,
                false
            )

            val txtNome = item.findViewById<android.widget.TextView>(
                R.id.txtNomeAtleta
            )

            val txtPosicao = item.findViewById<android.widget.TextView>(
                R.id.txtPosicaoAtleta
            )

            val btnVerPerfil = item.findViewById<android.widget.Button>(
                R.id.btnVerPerfil
            )

            txtNome.text = atleta.nome
            txtPosicao.text = atleta.posicao

            btnVerPerfil.setOnClickListener {

                // Aqui depois podemos colocar
                // a navegação para o perfil do atleta.

                Toast.makeText(
                    requireContext(),
                    "Perfil de ${atleta.nome}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            binding.containerAtletas.addView(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
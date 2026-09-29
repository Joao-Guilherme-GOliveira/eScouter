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
import com.example.escouter.databinding.FragmentBuscaTodosBinding
import com.example.escouter.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore

class FragmentBuscaTodos : Fragment() {

    private var _binding: FragmentBuscaTodosBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    // Guarda a posição que está sendo filtrada.
    // null = todas as posições
    private var posicaoSelecionada: String? = null

    // Define o tipo de usuário que está sendo exibido.
    // "Todos" = atletas + clubes
    // "Atletas" = somente atletas
    private var filtroSelecionado = "Todos"

    // Lista com os cards de categoria
    // usada para controlar a borda azul
    private var cardsCategoria: List<View> = emptyList()

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

        // Carrega atletas e clubes assim que abrir a tela
        buscarUsuarios()

        configurarBottomNavigation()
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

    // Marca um card com borda azul e desmarca os outros.
    // Se o card clicado já estava selecionado,
    // desmarca e mostra todas as posições.
    private fun selecionarCategoria(
        cardClicado: View,
        posicao: String
    ) {

        val jaSelecionado = cardClicado.isSelected

        // Limpa a seleção de todos os cards
        cardsCategoria.forEach {
            it.isSelected = false
        }

        if (jaSelecionado) {

            posicaoSelecionada = null

        } else {

            cardClicado.isSelected = true
            posicaoSelecionada = posicao
        }

        // Ao selecionar uma posição,
        // automaticamente trabalhamos somente com atletas
        filtroSelecionado = "Atletas"

        buscarUsuarios()
    }

    // Remove a borda azul de todos os cards
    private fun limparSelecaoCategorias() {

        cardsCategoria.forEach {
            it.isSelected = false
        }
    }

    private fun configurarListeners() {

        cardsCategoria = listOf(
            binding.cardDefensores,
            binding.cardMeioCampo,
            binding.cardAtacantes,
            binding.cardGoleiros
        )

        // BOTÃO TODOS
        // Mostra atletas + clubes
        binding.btnTodos.setOnClickListener {

            limparSelecaoCategorias()

            posicaoSelecionada = null

            filtroSelecionado = "Todos"

            buscarUsuarios()
        }

        // BOTÃO ATLETAS
        // Mostra somente atletas
        binding.btnAtletas.setOnClickListener {

            limparSelecaoCategorias()

            posicaoSelecionada = null

            filtroSelecionado = "Atletas"

            buscarUsuarios()
        }

        // BOTÃO CLUBES
        // Abre a tela específica de clubes
        binding.btnClubes.setOnClickListener {

            findNavController().navigate(
                R.id.action_fragmentBuscaTodos_to_fragmentBuscaClubes
            )
        }

        // FILTRO DEFENSORES
        binding.cardDefensores.setOnClickListener {

            selecionarCategoria(
                binding.cardDefensores,
                "Defensor"
            )
        }

        // FILTRO MEIO-CAMPO
        binding.cardMeioCampo.setOnClickListener {

            selecionarCategoria(
                binding.cardMeioCampo,
                "Meio-campo"
            )
        }

        // FILTRO ATACANTES
        binding.cardAtacantes.setOnClickListener {

            selecionarCategoria(
                binding.cardAtacantes,
                "Atacante"
            )
        }

        // FILTRO GOLEIROS
        binding.cardGoleiros.setOnClickListener {

            selecionarCategoria(
                binding.cardGoleiros,
                "Goleiro"
            )
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
                buscarUsuarios()
            }

            override fun afterTextChanged(
                s: Editable?
            ) {
            }
        })
    }

    private fun buscarUsuarios() {

        // Pega o texto digitado na pesquisa
        val textoBusca = binding.edtBusca
            .text
            .toString()
            .trim()
            .lowercase()

        // Busca todos os usuários
        db.collection("usuarios")
            .get()
            .addOnSuccessListener { resultado ->

                // Converte os documentos do Firestore
                // para objetos Usuario
                val usuarios = resultado.documents
                    .mapNotNull { documento ->

                        documento.toObject(
                            Usuario::class.java
                        )
                    }

                    // Filtra pelo tipo de usuário
                    .filter { usuario ->

                        when (filtroSelecionado) {

                            // TODOS:
                            // atletas + clubes
                            "Todos" -> {

                                usuario.tipoUsuario == "Atleta" ||
                                        usuario.tipoUsuario == "Clube/Olheiro"
                            }

                            // ATLETAS:
                            // somente atletas
                            "Atletas" -> {

                                usuario.tipoUsuario == "Atleta"
                            }

                            else -> {
                                false
                            }
                        }
                    }

                    // Filtra pela posição
                    .filter { usuario ->

                        if (
                            posicaoSelecionada != null
                        ) {

                            // Quando uma posição foi selecionada,
                            // somente atletas podem aparecer.

                            usuario.tipoUsuario == "Atleta" &&
                                    usuario.posicao == posicaoSelecionada

                        } else {

                            true
                        }
                    }

                    // Pesquisa pelo nome
                    .filter { usuario ->

                        textoBusca.isEmpty() ||
                                usuario.nome
                                    .lowercase()
                                    .contains(textoBusca)
                    }

                mostrarUsuarios(usuarios)
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao buscar usuários: ${erro.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarUsuarios(
        usuarios: List<Usuario>
    ) {

        // Limpa os resultados anteriores
        binding.containerAtletas.removeAllViews()

        // Se não encontrou ninguém
        if (usuarios.isEmpty()) {

            val mensagem = TextView(
                requireContext()
            )

            mensagem.text = "Nenhum resultado encontrado."
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

        // Cria um card para cada usuário
        usuarios.forEach { usuario ->

            // Usa o layout que você já possui
            // tanto para atleta quanto para clube
            val item = layoutInflater.inflate(
                R.layout.item_busca_atleta,
                binding.containerAtletas,
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

            // Nome do usuário
            txtNome.text = usuario.nome

            // Se for atleta:
            // mostra a posição
            //
            // Se for clube:
            // mostra cidade e estado
            if (usuario.tipoUsuario == "Atleta") {

                txtPosicao.text = usuario.posicao

            } else {

                txtPosicao.text =
                    "${usuario.cidade} - ${usuario.estado}"
            }

            btnVerPerfil.setOnClickListener {

                Toast.makeText(
                    requireContext(),
                    "Perfil de ${usuario.nome}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Adiciona o card na tela
            binding.containerAtletas.addView(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
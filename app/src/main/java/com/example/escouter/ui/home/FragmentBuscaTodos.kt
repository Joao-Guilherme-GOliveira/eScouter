
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

    private var posicaoSelecionada: String? = null

    private var filtroSelecionado = "Todos"

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

                R.id.nav_partidas -> {
                    findNavController().navigate(
                        R.id.partidasFragment
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

    private fun normalizar(valor: String): String {
        return valor.trim()
            .lowercase()
            .replace("-", "")
            .replace(" ", "")
    }

    private fun selecionarCategoria(
        cardClicado: View,
        posicao: String
    ) {

        val jaSelecionado = cardClicado.isSelected

        cardsCategoria.forEach {
            it.isSelected = false
        }

        if (jaSelecionado) {
            posicaoSelecionada = null
        } else {
            cardClicado.isSelected = true
            posicaoSelecionada = posicao
        }

        filtroSelecionado = "Atletas"

        buscarUsuarios()
    }

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

        binding.btnTodos.setOnClickListener {

            limparSelecaoCategorias()
            posicaoSelecionada = null
            filtroSelecionado = "Todos"

            buscarUsuarios()
        }

        binding.btnAtletas.setOnClickListener {

            limparSelecaoCategorias()
            posicaoSelecionada = null
            filtroSelecionado = "Atletas"

            buscarUsuarios()
        }

        binding.btnClubes.setOnClickListener {

            findNavController().navigate(
                R.id.action_fragmentBuscaTodos_to_fragmentBuscaClubes
            )
        }

        binding.cardDefensores.setOnClickListener {
            selecionarCategoria(
                binding.cardDefensores,
                "Defensor"
            )
        }

        binding.cardMeioCampo.setOnClickListener {
            selecionarCategoria(
                binding.cardMeioCampo,
                "Meio-campo"
            )
        }

        binding.cardAtacantes.setOnClickListener {
            selecionarCategoria(
                binding.cardAtacantes,
                "Atacante"
            )
        }

        binding.cardGoleiros.setOnClickListener {
            selecionarCategoria(
                binding.cardGoleiros,
                "Goleiro"
            )
        }

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

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun buscarUsuarios() {

        val textoBusca = normalizar(
            binding.edtBusca.text.toString()
        )

        db.collection("usuarios")
            .get()
            .addOnSuccessListener { resultado ->

                if (_binding == null) {
                    return@addOnSuccessListener
                }

                val usuarios = resultado.documents
                    .mapNotNull { documento ->

                        val usuario = documento.toObject(
                            Usuario::class.java
                        )

                        if (usuario == null) {
                            null
                        } else {
                            documento.id to usuario
                        }
                    }
                    .filter { (_, usuario) ->

                        val tipo = normalizar(
                            usuario.tipoUsuario
                        )

                        when (filtroSelecionado) {

                            "Todos" -> {
                                tipo == normalizar("Atleta") ||
                                        tipo == normalizar("Clube/Olheiro")
                            }

                            "Atletas" -> {
                                tipo == normalizar("Atleta")
                            }

                            else -> false
                        }
                    }
                    .filter { (_, usuario) ->

                        posicaoSelecionada == null ||
                                (
                                        normalizar(usuario.tipoUsuario) ==
                                                normalizar("Atleta") &&
                                                normalizar(usuario.posicao) ==
                                                normalizar(posicaoSelecionada!!)
                                        )
                    }
                    .filter { (_, usuario) ->

                        textoBusca.isEmpty() ||
                                normalizar(usuario.nome).contains(
                                    textoBusca
                                )
                    }

                mostrarUsuarios(usuarios)
            }
            .addOnFailureListener { erro ->

                if (_binding == null) {
                    return@addOnFailureListener
                }

                Toast.makeText(
                    requireContext(),
                    "Erro ao buscar usuários: ${erro.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarUsuarios(
        usuarios: List<Pair<String, Usuario>>
    ) {

        binding.containerAtletas.removeAllViews()

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

        usuarios.forEach { (uid, usuario) ->

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

            txtNome.text = usuario.nome

            if (
                normalizar(usuario.tipoUsuario) ==
                normalizar("Atleta")
            ) {
                txtPosicao.text = usuario.posicao
            } else {
                txtPosicao.text =
                    "${usuario.cidade} - ${usuario.estado}"
            }

            btnVerPerfil.setOnClickListener {

                val args = Bundle().apply {
                    putString("usuarioId", uid)
                }

                findNavController().navigate(
                    R.id.action_fragmentBuscaTodos_to_perfilFragment,
                    args
                )
            }

            binding.containerAtletas.addView(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

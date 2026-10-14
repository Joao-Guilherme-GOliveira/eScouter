package com.example.escouter.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.adapter.MediaAdapter
import com.example.escouter.databinding.FragmentPerfilBinding
import com.example.escouter.model.Midia
import com.example.escouter.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var usuarioAtual: Usuario? = null
    // uid do perfil que está sendo exibido
    // (o meu, ou o de outro usuário vindo da busca)
    private lateinit var perfilUid: String

    // true  = estou vendo o perfil de outra pessoa (somente leitura)
    // false = é o meu próprio perfil (pode editar)
    private var somenteLeitura = false
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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        return binding.root
    }
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val meuUid = auth.currentUser?.uid
        if (meuUid == null) {
            Toast.makeText(
                requireContext(),
                "Nenhum usuário está logado.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        // Se veio "usuarioId" nos argumentos, exibe esse perfil.
        // Se não veio, exibe o perfil do usuário logado.
        perfilUid = arguments?.getString("usuarioId") ?: meuUid

        somenteLeitura = perfilUid != meuUid
        configurarModoVisualizacao()
        carregarUsuario()
        configurarBottomNavigation()
        configurarRecyclerMidias()
        // O clique de editar só é configurado no meu próprio perfil
        if (!somenteLeitura) {
            configurarBotaoEditar()
        }
    }
    // MODO VISUALIZAÇÃO / EDIÇÃO
    private fun configurarModoVisualizacao() {

        // Perfil de terceiros: o botão de editar nem aparece
        binding.btnEditarPerfil.visibility =
            if (somenteLeitura) View.GONE else View.VISIBLE
    }

    private fun configurarBotaoEditar() {

        binding.btnEditarPerfil.setOnClickListener {

            val usuario = usuarioAtual ?: return@setOnClickListener

            if (usuario.tipoUsuario.equals("Atleta", ignoreCase = true)) {

                findNavController().navigate(
                    R.id.action_perfilFragment_to_editarPerfilAtletaFragment
                )

            } else if (
                usuario.tipoUsuario.equals("Clube/Olheiro", ignoreCase = true)
            ) {

                findNavController().navigate(
                    R.id.action_perfilFragment_to_editarPerfilClubeFragment
                )
            }
        }
    }
    private fun configurarRecyclerMidias() {

        mediaAdapter = MediaAdapter(emptyList())

        binding.recyclerMedia.adapter = mediaAdapter

        carregarMidias()
    }

    private fun carregarUsuario() {

        db.collection("usuarios")
            .document(perfilUid)
            .get()
            .addOnSuccessListener { document ->
                // Evita crash se a tela foi fechada antes da resposta
                if (_binding == null) return@addOnSuccessListener

                if (!document.exists()) {

                    Toast.makeText(
                        requireContext(),
                        "Perfil não encontrado.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }
                try {
                    val usuario = document.toObject(Usuario::class.java)
                    if (usuario == null) {
                        Toast.makeText(
                            requireContext(),
                            "Não foi possível converter os dados do usuário.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnSuccessListener
                    }
                    usuarioAtual = usuario

                    mostrarUsuario(usuario)

                } catch (e: Exception) {

                    Toast.makeText(
                        requireContext(),
                        "Erro ao converter usuário: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { erro ->

                if (_binding == null) return@addOnFailureListener

                Toast.makeText(
                    requireContext(),
                    "Erro ao acessar Firestore: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun mostrarUsuario(usuario: Usuario) {

        val ehClube =
            usuario.tipoUsuario.equals("Clube/Olheiro", ignoreCase = true)

        if (ehClube) {

            binding.txtPositionAge.visibility = View.GONE
            binding.cardInfo.visibility = View.GONE

        } else {

            binding.txtPositionAge.visibility = View.VISIBLE
            binding.cardInfo.visibility = View.VISIBLE
        }
        binding.txtName.text = usuario.nome

        if (!ehClube) {

            val idade = calcularIdade(usuario.dataNascimento)

            binding.txtPositionAge.text =
                "${usuario.posicao} • $idade anos"

            // Informações

            binding.statIdade.txtStatLabel.text = "Idade"
            binding.statIdade.txtStatValue.text = "$idade anos"

            binding.statPeso.txtStatLabel.text = "Peso"
            binding.statPeso.txtStatValue.text = usuario.peso

            binding.statAltura.txtStatLabel.text = "Altura"
            binding.statAltura.txtStatValue.text = usuario.altura

            binding.statExperiencia.txtStatLabel.text = "Experiência"
            binding.statExperiencia.txtStatValue.text = usuario.experiencia
        }
        val anoCadastro =
            if (usuario.dataCadastro.isNotEmpty()) {
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
        if (usuario.descricao.isEmpty()) {
            binding.txtBio.visibility = View.GONE
        } else {
            binding.txtBio.visibility = View.VISIBLE
            binding.txtBio.text = usuario.descricao
        }
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

                val formato = SimpleDateFormat(padrao, Locale.getDefault())
                formato.isLenient = false
                val data = formato.parse(dataNascimento) ?: continue
                val nascimento = Calendar.getInstance()
                nascimento.time = data
                val hoje = Calendar.getInstance()
                var idade =
                    hoje.get(Calendar.YEAR) - nascimento.get(Calendar.YEAR)
                if (hoje.get(Calendar.MONTH) < nascimento.get(Calendar.MONTH)) {
                    idade--
                } else if (
                    hoje.get(Calendar.MONTH) == nascimento.get(Calendar.MONTH) &&
                    hoje.get(Calendar.DAY_OF_MONTH) <
                    nascimento.get(Calendar.DAY_OF_MONTH)
                ) {
                    idade--
                }
                return idade
            } catch (_: Exception) {
                // Tenta o próximo formato
            }
        }
        return 0
    }
    private fun carregarMidias() {
        db.collection("midias")
            .whereEqualTo("usuarioId", perfilUid)
            .get()
            .addOnSuccessListener { documentos ->
                if (_binding == null) return@addOnSuccessListener

                val listaMidias = documentos.mapNotNull { documento ->
                    try {
                        documento.toObject(Midia::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (listaMidias.isEmpty()) {

                    binding.recyclerMedia.visibility = View.GONE
                } else {
                    binding.recyclerMedia.visibility = View.VISIBLE
                    mediaAdapter.atualizarMidias(listaMidias)
                }
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener

                binding.recyclerMedia.visibility = View.GONE

                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar mídias.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    private fun configurarBottomNavigation() {
        val navController = findNavController()
        // Define o item selecionado ANTES de registrar o listener,
        // para não disparar navegação sem querer.
        // Meu perfil -> "Perfil" marcado
        // Perfil de outra pessoa -> "Pesquisar" marcado (veio da busca)
        binding.bottomNavigation.selectedItemId =
            if (somenteLeitura) R.id.nav_pesquisar else R.id.nav_perfil
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.nav_perfil -> {
                    // Se estou vendo outro perfil, volta para o MEU perfil
                    // (abre sem argumento). Se já é o meu, não faz nada.
                    if (somenteLeitura) {
                        navController.navigate(R.id.perfilFragment)
                    }
                    true
                }
                R.id.nav_pesquisar -> {
                    if (somenteLeitura) {
                        // Volta para a tela de busca de onde veio
                        navController.popBackStack()

                    } else {

                        navController.navigate(
                            R.id.action_perfilFragment_to_fragmentBuscaTodos
                        )
                    }
                    true
                }
                else -> false
            }
        }
    }
    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
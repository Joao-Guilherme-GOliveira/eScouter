package com.example.escouter.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.adapter.MediaAdapter
import com.example.escouter.data.SeguidorRepository
import com.example.escouter.databinding.FragmentPerfilBinding
import com.example.escouter.model.Midia
import com.example.escouter.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PerfilFragment : Fragment() {

    // Estado do botão seguir / contador de seguidores
    private var seguindo = false
    private var totalSeguidores = 0L
    private val uidLogado get() = auth.currentUser?.uid.orEmpty()

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var usuarioAtual: Usuario? = null

    // Listener em tempo real do documento do usuário
    private var perfilListener: ListenerRegistration? = null

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
        carregarContadores()

        // Perfil de terceiros: botão seguir. Meu perfil: botão editar.
        if (somenteLeitura) {
            configurarBotaoSeguir()
        } else {
            configurarBotaoEditar()
        }
    }

    // =========================================================
    // MODO VISUALIZAÇÃO / EDIÇÃO
    // =========================================================

    private fun configurarModoVisualizacao() {

        // Perfil de terceiros: o botão de editar nem aparece
        binding.btnEditarPerfil.visibility =
            if (somenteLeitura) View.GONE else View.VISIBLE

        // Perfil de terceiros: aparece o botão de seguir
        binding.btnSeguir.visibility =
            if (somenteLeitura) View.VISIBLE else View.GONE
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

    // =========================================================
    // SEGUIR / SEGUIDORES
    // =========================================================

    private fun carregarContadores() {
        SeguidorRepository.qtdSeguidores(perfilUid) { total ->
            if (_binding != null) {
                totalSeguidores = total
                atualizarTextoSeguidores()
            }
        }
        SeguidorRepository.qtdSeguindo(perfilUid) { total ->
            if (_binding != null) {
                binding.txtSeguindo.text = "$total seguindo"
            }
        }
    }

    private fun atualizarTextoSeguidores() {
        binding.txtSeguidores.text =
            if (totalSeguidores == 1L) "1 seguidor"
            else "$totalSeguidores seguidores"
    }

    private fun configurarBotaoSeguir() {
        // Fica desabilitado até sabermos se eu já sigo essa pessoa
        binding.btnSeguir.isEnabled = false

        SeguidorRepository.estaSeguindo(uidLogado, perfilUid) { resultado ->
            if (_binding != null) {
                seguindo = resultado
                atualizarBotaoSeguir()
                binding.btnSeguir.isEnabled = true
            }
        }

        binding.btnSeguir.setOnClickListener {
            alternarSeguir()
        }
    }

    private fun alternarSeguir() {
        // Evita toques repetidos enquanto a operação está em andamento
        binding.btnSeguir.isEnabled = false

        val aoTerminar = { sucesso: Boolean ->
            if (_binding != null) {
                if (sucesso) {
                    seguindo = !seguindo
                    totalSeguidores =
                        if (seguindo) totalSeguidores + 1
                        else (totalSeguidores - 1).coerceAtLeast(0)
                    atualizarTextoSeguidores()
                    atualizarBotaoSeguir()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Não foi possível concluir. Tente novamente.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                binding.btnSeguir.isEnabled = true
            }
        }

        if (seguindo) {
            SeguidorRepository.deixaSeguir(
                uidLogado, perfilUid,
                onSucesso = { aoTerminar(true) },
                onErro = { aoTerminar(false) }
            )
        } else {
            SeguidorRepository.seguir(
                uidLogado, perfilUid,
                onSucesso = { aoTerminar(true) },
                onErro = { aoTerminar(false) }
            )
        }
    }

    private fun atualizarBotaoSeguir() {
        if (seguindo) {
            binding.btnSeguir.text = "Seguindo"
            binding.btnSeguir.setBackgroundResource(R.drawable.bg_button_outline)
            binding.btnSeguir.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.cardPeneira)
            )
        } else {
            binding.btnSeguir.text = "Seguir"
            binding.btnSeguir.setBackgroundResource(R.drawable.bg_button_primary)
            binding.btnSeguir.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.card_white)
            )
        }
    }

    private fun configurarRecyclerMidias() {

        mediaAdapter = MediaAdapter(emptyList())

        binding.recyclerMedia.adapter = mediaAdapter

        carregarMidias()
    }

    // =========================================================
    // CARREGAR USUÁRIO (em tempo real)
    // =========================================================

    private fun carregarUsuario() {

        // Garante que nunca existam dois listeners ao mesmo tempo
        perfilListener?.remove()

        perfilListener = db.collection("usuarios")
            .document(perfilUid)
            .addSnapshotListener { document, erro ->

                // Evita crash se a tela foi fechada antes da resposta
                if (_binding == null) return@addSnapshotListener

                if (erro != null) {

                    Toast.makeText(
                        requireContext(),
                        "Erro ao acessar Firestore: ${erro.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (document == null || !document.exists()) {

                    Toast.makeText(
                        requireContext(),
                        "Perfil não encontrado.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                try {
                    val usuario = document.toObject(Usuario::class.java)

                    if (usuario == null) {
                        Toast.makeText(
                            requireContext(),
                            "Não foi possível converter os dados do usuário.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addSnapshotListener
                    }

                    usuarioAtual = usuario

                    // Campos extras do clube, lidos direto do documento
                    val telefone = document.getString("telefone") ?: ""
                    val cnpj = document.getString("cnpj") ?: ""

                    mostrarUsuario(usuario, telefone, cnpj)

                } catch (e: Exception) {

                    Toast.makeText(
                        requireContext(),
                        "Erro ao converter usuário: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun mostrarUsuario(
        usuario: Usuario,
        telefone: String,
        cnpj: String
    ) {

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

        // Contato do clube (telefone e CNPJ)
        val linhasContato = mutableListOf<String>()

        if (telefone.isNotBlank()) linhasContato.add("📞 $telefone")
        if (cnpj.isNotBlank()) linhasContato.add("CNPJ: $cnpj")

        if (ehClube && linhasContato.isNotEmpty()) {
            binding.txtContatoClube.visibility = View.VISIBLE
            binding.txtContatoClube.text = linhasContato.joinToString("\n")
        } else {
            binding.txtContatoClube.visibility = View.GONE
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

    // =========================================================
    // MÍDIAS
    // =========================================================

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

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

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

                R.id.nav_partidas -> {
                    navController.navigate(R.id.partidasFragment)
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

        // Para de escutar o Firestore quando a tela some
        perfilListener?.remove()
        perfilListener = null

        _binding = null
    }
}
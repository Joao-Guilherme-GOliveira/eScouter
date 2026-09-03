package com.example.escouter.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentPerfilBinding
import com.example.escouter.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.escouter.adapter.MediaAdapter
import com.example.escouter.model.Midia

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var usuarioAtual: Usuario? = null

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

        carregarUsuario()
        configurarBottomNavigation()
        configurarRecyclerMidias()

        binding.btnEditarPerfil.setOnClickListener {

            val usuario = usuarioAtual ?: return@setOnClickListener

            if (usuario.tipoUsuario.equals(
                    "Atleta",
                    ignoreCase = true
                )
            ) {

                findNavController().navigate(
                    R.id.action_perfilFragment_to_editarPerfilAtletaFragment
                )

            } else if (
                usuario.tipoUsuario.equals(
                    "Clube/Olheiro",
                    ignoreCase = true
                )
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

    // =========================================================
    // CARREGAR USUÁRIO DO FIRESTORE
    // =========================================================

    private fun carregarUsuario() {

        val usuarioFirebase = auth.currentUser

        if (usuarioFirebase == null) {

            Toast.makeText(
                requireContext(),
                "Nenhum usuário está logado.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val uid = usuarioFirebase.uid

        db.collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    Toast.makeText(
                        requireContext(),
                        "Documento não encontrado: usuarios/$uid",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                try {

                    val usuario = document.toObject(
                        Usuario::class.java
                    )

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

                Toast.makeText(
                    requireContext(),
                    "Erro ao acessar Firestore: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // MOSTRAR DADOS NA TELA
    // =========================================================

    private fun mostrarUsuario(usuario: Usuario) {

        val ehClube =
            usuario.tipoUsuario.equals(
                "Clube/Olheiro",
                ignoreCase = true
            )

        // =====================================================
        // TIPO DE PERFIL
        // =====================================================

        if (ehClube) {

            binding.txtPositionAge.visibility =
                View.GONE

            binding.cardInfo.visibility =
                View.GONE

        } else {

            binding.txtPositionAge.visibility =
                View.VISIBLE

            binding.cardInfo.visibility =
                View.VISIBLE
        }

        // =====================================================
        // NOME
        // =====================================================

        binding.txtName.text =
            usuario.nome

        // =====================================================
        // IDADE / POSIÇÃO
        // =====================================================

        if (!ehClube) {

            val idade =
                calcularIdade(
                    usuario.dataNascimento
                )

            binding.txtPositionAge.text =
                "${usuario.posicao} • $idade anos"

            // Informações

            binding.statIdade.txtStatLabel.text =
                "Idade"

            binding.statIdade.txtStatValue.text =
                "$idade anos"

            binding.statPeso.txtStatLabel.text =
                "Peso"

            binding.statPeso.txtStatValue.text =
                usuario.peso

            binding.statAltura.txtStatLabel.text =
                "Altura"

            binding.statAltura.txtStatValue.text =
                usuario.altura

            binding.statExperiencia.txtStatLabel.text =
                "Experiência"

            binding.statExperiencia.txtStatValue.text =
                usuario.experiencia
        }

        // =====================================================
        // LOCALIZAÇÃO / DATA
        // =====================================================

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

        // =====================================================
        // DESCRIÇÃO
        // =====================================================

        if (usuario.descricao.isEmpty()) {

            binding.txtBio.visibility =
                View.GONE

        } else {

            binding.txtBio.visibility =
                View.VISIBLE

            binding.txtBio.text =
                usuario.descricao
        }


    }

    // =========================================================
    // CALCULAR IDADE
    // =========================================================

    private fun calcularIdade(
        dataNascimento: String
    ): Int {

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

                val formato =
                    SimpleDateFormat(
                        padrao,
                        Locale.getDefault()
                    )

                formato.isLenient = false

                val data =
                    formato.parse(
                        dataNascimento
                    ) ?: continue

                val nascimento =
                    Calendar.getInstance()

                nascimento.time = data

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

        val usuarioFirebase = auth.currentUser

        if (usuarioFirebase == null) {
            binding.recyclerMedia.visibility = View.GONE
            return
        }

        val uid = usuarioFirebase.uid

        db.collection("midias")
            .whereEqualTo("usuarioId", uid)
            .get()
            .addOnSuccessListener { documentos ->

                val listaMidias = documentos.mapNotNull { documento ->

                    try {
                        documento.toObject(Midia::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (listaMidias.isEmpty()) {

                    binding.recyclerMedia.visibility =
                        View.GONE

                } else {

                    binding.recyclerMedia.visibility =
                        View.VISIBLE

                    mediaAdapter.atualizarMidias(listaMidias)
                }
            }
            .addOnFailureListener {

                binding.recyclerMedia.visibility =
                    View.GONE

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

        val navController =
            findNavController()

        binding.bottomNavigation
            .setOnItemSelectedListener { item ->

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

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
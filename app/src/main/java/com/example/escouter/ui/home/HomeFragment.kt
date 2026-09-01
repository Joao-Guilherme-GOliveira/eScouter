package com.example.escouter.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.model.InscricaoPeneira
import com.example.escouter.model.Peneira
import com.example.escouter.model.Usuario
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var cardPeneira: View
    private lateinit var txtTime: TextView
    private lateinit var txtData: TextView

    private lateinit var btnCriarPeneira: Button
    private lateinit var btnMinhasPeneiras: Button
    private lateinit var btnInscreverPeneira: Button

    private var peneiraExibida: Peneira? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var usuarioAtual: Usuario? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Views
        bottomNavigation =
            view.findViewById(R.id.bottomNavigation)

        cardPeneira =
            view.findViewById(R.id.cardPeneira)

        txtTime =
            view.findViewById(R.id.txtTime)

        txtData =
            view.findViewById(R.id.txtData)

        btnCriarPeneira =
            view.findViewById(R.id.btnCriarPeneira)

        btnMinhasPeneiras =
            view.findViewById(R.id.btnMinhasPeneiras)

        btnInscreverPeneira =
            view.findViewById(R.id.btnInscreverPeneira)

        configurarBottomNavigation()

        carregarUsuario()
    }

    // =========================================================
    // USUÁRIO
    // =========================================================

    private fun carregarUsuario() {

        val usuarioFirebase = auth.currentUser

        if (usuarioFirebase == null) {

            Toast.makeText(
                requireContext(),
                "Nenhum usuário está logado.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        db.collection("usuarios")
            .document(usuarioFirebase.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    Toast.makeText(
                        requireContext(),
                        "Usuário não encontrado no Firestore.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                val usuario =
                    document.toObject(Usuario::class.java)

                if (usuario == null) {

                    Toast.makeText(
                        requireContext(),
                        "Erro ao carregar usuário.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                usuarioAtual = usuario

                configurarTelaPorTipoDeUsuario()
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar usuário: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // TELA DE ACORDO COM O USUÁRIO
    // =========================================================

    private fun configurarTelaPorTipoDeUsuario() {

        val usuario = usuarioAtual ?: return

        val isClubeOuOlheiro =
            usuario.tipoUsuario.equals(
                "Clube/Olheiro",
                ignoreCase = true
            )

        val isAtleta =
            usuario.tipoUsuario.equals(
                "Atleta",
                ignoreCase = true
            )

        if (isClubeOuOlheiro) {

            btnCriarPeneira.visibility =
                View.VISIBLE

            btnMinhasPeneiras.visibility =
                View.VISIBLE

            btnInscreverPeneira.visibility =
                View.GONE

            btnCriarPeneira.setOnClickListener {
                abrirCriarPeneira()
            }

            btnMinhasPeneiras.setOnClickListener {

                findNavController().navigate(
                    R.id.minhasPeneirasFragment
                )
            }

        } else if (isAtleta) {

            btnCriarPeneira.visibility =
                View.GONE

            btnMinhasPeneiras.visibility =
                View.GONE

            btnInscreverPeneira.visibility =
                View.VISIBLE

            btnInscreverPeneira.setOnClickListener {
                inscreverAtletaNaPeneira()
            }

        } else {

            btnCriarPeneira.visibility =
                View.GONE

            btnMinhasPeneiras.visibility =
                View.GONE

            btnInscreverPeneira.visibility =
                View.GONE
        }

        exibirProximaPeneira()
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private fun configurarBottomNavigation() {

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
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

    // =========================================================
    // BUSCAR PENEIRAS NO FIRESTORE
    // =========================================================

    private fun exibirProximaPeneira() {

        db.collection("peneiras")
            .get()
            .addOnSuccessListener { resultado ->

                val peneiras = resultado.documents.mapNotNull {
                    it.toObject(Peneira::class.java)
                }

                if (peneiras.isEmpty()) {

                    cardPeneira.visibility =
                        View.GONE

                    btnInscreverPeneira.visibility =
                        View.GONE

                    peneiraExibida = null

                    return@addOnSuccessListener
                }

                encontrarProximaPeneira(
                    peneiras
                )
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar peneiras: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()

                cardPeneira.visibility =
                    View.GONE
            }
    }

    // =========================================================
    // ENCONTRAR PRÓXIMA PENEIRA
    // =========================================================

    private fun encontrarProximaPeneira(
        peneiras: List<Peneira>
    ) {

        val formato = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )

        val hoje = Calendar.getInstance().apply {

            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

        }.time

        val proximaPeneira =
            peneiras
                .mapNotNull { peneira ->

                    try {

                        val dataPeneira =
                            formato.parse(
                                peneira.data
                            )

                        if (
                            dataPeneira != null &&
                            !dataPeneira.before(hoje)
                        ) {

                            Pair(
                                peneira,
                                dataPeneira
                            )

                        } else {

                            null
                        }

                    } catch (_: Exception) {

                        null
                    }
                }
                .minByOrNull {
                    it.second
                }
                ?.first

        if (proximaPeneira == null) {

            cardPeneira.visibility =
                View.GONE

            btnInscreverPeneira.visibility =
                View.GONE

            peneiraExibida = null

            return
        }

        peneiraExibida =
            proximaPeneira

        cardPeneira.visibility =
            View.VISIBLE

        txtTime.text =
            proximaPeneira.nomeTime

        txtData.text =
            "${proximaPeneira.data} • " +
                    "${proximaPeneira.hora} • " +
                    proximaPeneira.local

        val usuario =
            usuarioAtual

        val isAtleta =
            usuario?.tipoUsuario.equals(
                "Atleta",
                ignoreCase = true
            )

        if (isAtleta) {

            configurarBotaoInscricao(
                usuario,
                proximaPeneira
            )

        } else {

            btnInscreverPeneira.visibility =
                View.GONE
        }
    }

    // =========================================================
    // VERIFICAR INSCRIÇÃO
    // =========================================================

    private fun configurarBotaoInscricao(
        usuario: Usuario?,
        peneira: Peneira
    ) {

        if (usuario == null) {

            btnInscreverPeneira.visibility =
                View.GONE

            return
        }

        db.collection("inscricoesPeneira")
            .whereEqualTo(
                "atletaEmail",
                usuario.email
            )
            .whereEqualTo(
                "nomeTime",
                peneira.nomeTime
            )
            .whereEqualTo(
                "data",
                peneira.data
            )
            .whereEqualTo(
                "hora",
                peneira.hora
            )
            .get()
            .addOnSuccessListener { resultado ->

                btnInscreverPeneira.visibility =
                    View.VISIBLE

                if (!resultado.isEmpty) {

                    btnInscreverPeneira.text =
                        "Já inscrito"

                    btnInscreverPeneira.isEnabled =
                        false

                } else {

                    btnInscreverPeneira.text =
                        "Inscrever-se"

                    btnInscreverPeneira.isEnabled =
                        true
                }
            }
    }

    // =========================================================
    // INSCRIÇÃO
    // =========================================================

    private fun inscreverAtletaNaPeneira() {

        val usuario =
            usuarioAtual

        val peneira =
            peneiraExibida

        if (
            usuario == null ||
            peneira == null
        ) {

            Toast.makeText(
                requireContext(),
                "Não foi possível realizar a inscrição.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Verifica novamente no Firestore
        db.collection("inscricoesPeneira")
            .whereEqualTo(
                "atletaEmail",
                usuario.email
            )
            .whereEqualTo(
                "nomeTime",
                peneira.nomeTime
            )
            .whereEqualTo(
                "data",
                peneira.data
            )
            .whereEqualTo(
                "hora",
                peneira.hora
            )
            .get()
            .addOnSuccessListener { resultado ->

                if (!resultado.isEmpty) {

                    Toast.makeText(
                        requireContext(),
                        "Você já está inscrito nesta peneira.",
                        Toast.LENGTH_SHORT
                    ).show()

                    configurarBotaoInscricao(
                        usuario,
                        peneira
                    )

                    return@addOnSuccessListener
                }

                val novaInscricao =
                    InscricaoPeneira(

                        atletaNome =
                            usuario.nome,

                        atletaEmail =
                            usuario.email,

                        nomeTime =
                            peneira.nomeTime,

                        data =
                            peneira.data,

                        hora =
                            peneira.hora,

                        local =
                            peneira.local
                    )

                db.collection("inscricoesPeneira")
                    .add(novaInscricao)
                    .addOnSuccessListener {

                        Toast.makeText(
                            requireContext(),
                            "Inscrição realizada com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()

                        configurarBotaoInscricao(
                            usuario,
                            peneira
                        )
                    }
                    .addOnFailureListener { erro ->

                        Toast.makeText(
                            requireContext(),
                            "Erro ao realizar inscrição: ${erro.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao verificar inscrição: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // CRIAR PENEIRA
    // =========================================================

    private fun abrirCriarPeneira() {

        val dialog =
            CriarPeneiraFragment()

        dialog.onPeneiraCriada = {

            exibirProximaPeneira()
        }

        dialog.show(
            parentFragmentManager,
            "CriarPeneiraFragment"
        )
    }
}
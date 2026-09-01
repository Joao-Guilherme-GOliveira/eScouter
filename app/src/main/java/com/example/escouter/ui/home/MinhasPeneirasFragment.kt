package com.example.escouter.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentMinhasPeneirasBinding
import com.example.escouter.model.Peneira
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MinhasPeneirasFragment : Fragment() {

    private var _binding: FragmentMinhasPeneirasBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMinhasPeneirasBinding.inflate(
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

        bottomNavigation =
            view.findViewById(R.id.bottomNavigation)

        configurarBottomNavigation()

        carregarMinhasPeneiras()
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private fun configurarBottomNavigation() {

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {

                    findNavController().navigate(
                        R.id.homeFragment
                    )

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
    // CARREGAR MINHAS PENEIRAS
    // =========================================================

    private fun carregarMinhasPeneiras() {

        val usuario = auth.currentUser

        if (usuario == null) {

            mostrarMensagem(
                "Nenhum usuário está logado."
            )

            return
        }

        val emailClube = usuario.email

        if (emailClube.isNullOrEmpty()) {

            mostrarMensagem(
                "Não foi possível identificar o clube."
            )

            return
        }

        db.collection("peneiras")
            .whereEqualTo(
                "emailClube",
                emailClube
            )
            .get()
            .addOnSuccessListener { resultado ->

                binding.containerPeneiras.removeAllViews()

                if (resultado.isEmpty) {

                    mostrarMensagem(
                        "Você ainda não criou nenhuma peneira."
                    )

                    return@addOnSuccessListener
                }

                binding.txtMensagem.visibility =
                    View.GONE

                // Percorre as peneiras encontradas
                for (documento in resultado) {

                    val peneira =
                        documento.toObject(
                            Peneira::class.java
                        )

                    adicionarPeneiraNaTela(
                        peneira
                    )
                }
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar peneiras: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()

                mostrarMensagem(
                    "Erro ao carregar suas peneiras."
                )
            }
    }

    // =========================================================
    // ADICIONAR PENEIRA NA TELA
    // =========================================================

    private fun adicionarPeneiraNaTela(
        peneira: Peneira
    ) {

        val item = layoutInflater.inflate(
            R.layout.item_minhas_peneiras,
            binding.containerPeneiras,
            false
        )

        val txtNomeTime =
            item.findViewById<TextView>(
                R.id.txtNomeTime
            )

        val txtData =
            item.findViewById<TextView>(
                R.id.txtData
            )

        val txtLocal =
            item.findViewById<TextView>(
                R.id.txtLocal
            )

        txtNomeTime.text =
            peneira.nomeTime

        txtData.text =
            "${peneira.data} • ${peneira.hora}"

        txtLocal.text =
            peneira.local

        binding.containerPeneiras.addView(
            item
        )
    }

    // =========================================================
    // MENSAGEM
    // =========================================================

    private fun mostrarMensagem(
        mensagem: String
    ) {

        binding.txtMensagem.visibility =
            View.VISIBLE

        binding.txtMensagem.text =
            mensagem
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
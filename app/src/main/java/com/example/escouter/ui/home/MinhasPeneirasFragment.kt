package com.example.escouter.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

                for (documento in resultado) {

                    val peneira =
                        documento.toObject(
                            Peneira::class.java
                        )

                    adicionarPeneiraNaTela(
                        documento.id,
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
        idPeneira: String,
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

        val btnEditar =
            item.findViewById<View>(
                R.id.btnEditar
            )

        val btnExcluir =
            item.findViewById<View>(
                R.id.btnExcluir
            )

        txtNomeTime.text =
            peneira.nomeTime

        txtData.text = getString(
            R.string.data_hora_peneira,
            peneira.data,
            peneira.hora
        )

        txtLocal.text =
            peneira.local

        btnEditar.setOnClickListener {

            editarPeneira(
                idPeneira,
                peneira
            )
        }

        btnExcluir.setOnClickListener {

            confirmarExclusao(
                idPeneira
            )
        }

        binding.containerPeneiras.addView(
            item
        )
    }

    // =========================================================
    // EDITAR PENEIRA
    // =========================================================

    private fun editarPeneira(
        idPeneira: String,
        peneira: Peneira
    ) {

        val layout = LinearLayout(requireContext())

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            48,
            0,
            48,
            0
        )

        val editNomeTime =
            EditText(requireContext())

        editNomeTime.hint =
            "Nome do time"

        editNomeTime.setText(
            peneira.nomeTime
        )

        val editData =
            EditText(requireContext())

        editData.hint =
            "Data"

        editData.setText(
            peneira.data
        )

        val editHora =
            EditText(requireContext())

        editHora.hint =
            "Hora"

        editHora.setText(
            peneira.hora
        )

        val editLocal =
            EditText(requireContext())

        editLocal.hint =
            "Local"

        editLocal.setText(
            peneira.local
        )

        layout.addView(
            editNomeTime
        )

        layout.addView(
            editData
        )

        layout.addView(
            editHora
        )

        layout.addView(
            editLocal
        )

        val dialog =
            AlertDialog.Builder(requireContext())
                .setTitle("Editar peneira")
                .setView(layout)
                .setNegativeButton(
                    "Cancelar",
                    null
                )
                .setPositiveButton(
                    "Salvar",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val nomeTime =
                    editNomeTime.text
                        .toString()
                        .trim()

                val data =
                    editData.text
                        .toString()
                        .trim()

                val hora =
                    editHora.text
                        .toString()
                        .trim()

                val local =
                    editLocal.text
                        .toString()
                        .trim()

                if (
                    nomeTime.isEmpty() ||
                    data.isEmpty() ||
                    hora.isEmpty() ||
                    local.isEmpty()
                ) {

                    Toast.makeText(
                        requireContext(),
                        "Preencha todos os campos.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val dadosAtualizados: Map<String, Any> =
                    mapOf(
                        "nomeTime" to nomeTime,
                        "data" to data,
                        "hora" to hora,
                        "local" to local,
                        "emailClube" to peneira.emailClube
                    )

                db.collection("peneiras")
                    .document(idPeneira)
                    .update(dadosAtualizados)
                    .addOnSuccessListener {

                        Toast.makeText(
                            requireContext(),
                            "Peneira atualizada com sucesso.",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()

                        carregarMinhasPeneiras()
                    }
                    .addOnFailureListener { erro ->

                        Toast.makeText(
                            requireContext(),
                            "Erro ao editar peneira: ${erro.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        dialog.show()
    }

    // =========================================================
    // EXCLUIR PENEIRA
    // =========================================================

    private fun confirmarExclusao(
        idPeneira: String
    ) {

        AlertDialog.Builder(requireContext())
            .setTitle("Excluir peneira")
            .setMessage(
                "Tem certeza que deseja excluir esta peneira?"
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Excluir"
            ) { _, _ ->

                excluirPeneira(
                    idPeneira
                )
            }
            .show()
    }

    private fun excluirPeneira(
        idPeneira: String
    ) {

        db.collection("peneiras")
            .document(idPeneira)
            .delete()
            .addOnSuccessListener {

                Toast.makeText(
                    requireContext(),
                    "Peneira excluída com sucesso.",
                    Toast.LENGTH_SHORT
                ).show()

                carregarMinhasPeneiras()
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao excluir peneira: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
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
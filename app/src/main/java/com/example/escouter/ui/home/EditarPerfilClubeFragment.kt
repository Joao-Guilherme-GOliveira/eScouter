package com.example.escouter.ui.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.databinding.FragmentEditarPerfilClubeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilClubeFragment : Fragment() {

    private var _binding: FragmentEditarPerfilClubeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var imagemSelecionada: Uri? = null

    private val selecionarImagem =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri?.let {

                imagemSelecionada = it

                binding.imgLogoClube.setImageURI(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEditarPerfilClubeBinding.inflate(
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

        carregarDados()

        binding.btnAlterarLogo.setOnClickListener {
            selecionarImagem.launch("image/*")
        }

        binding.btnSalvar.setOnClickListener {
            salvarAlteracoes()
        }
    }

    // =========================================================
    // CARREGAR DADOS
    // =========================================================

    private fun carregarDados() {

        val usuarioFirebase = auth.currentUser

        if (usuarioFirebase == null) {

            Toast.makeText(
                requireContext(),
                "Nenhum usuário está logado.",
                Toast.LENGTH_SHORT
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
                        "Perfil não encontrado.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                val nome =
                    document.getString("nome") ?: ""

                val cnpj =
                    document.getString("cnpj") ?: ""

                val cidade =
                    document.getString("cidade") ?: ""

                val estado =
                    document.getString("estado") ?: ""

                val descricao =
                    document.getString("descricao") ?: ""

                val telefone =
                    document.getString("telefone") ?: ""

                binding.edtNomeClube.setText(nome)
                binding.edtCnpj.setText(cnpj)
                binding.edtCidade.setText(cidade)
                binding.edtEstado.setText(estado)
                binding.edtDescricao.setText(descricao)
                binding.edtTelefone.setText(telefone)
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // SALVAR
    // =========================================================

    private fun salvarAlteracoes() {

        val usuarioFirebase = auth.currentUser

        if (usuarioFirebase == null) {

            Toast.makeText(
                requireContext(),
                "Nenhum usuário está logado.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val nome = binding.edtNomeClube
            .text
            .toString()
            .trim()

        val cnpj = binding.edtCnpj
            .text
            .toString()
            .trim()

        val cidade = binding.edtCidade
            .text
            .toString()
            .trim()

        val estado = binding.edtEstado
            .text
            .toString()
            .trim()

        val descricao = binding.edtDescricao
            .text
            .toString()
            .trim()

        val telefone = binding.edtTelefone
            .text
            .toString()
            .trim()

        // =====================================================
        // VALIDAÇÕES
        // =====================================================

        if (nome.isEmpty()) {

            binding.tilNomeClube.error =
                "Informe o nome do clube"

            binding.edtNomeClube.requestFocus()

            return

        } else {
            binding.tilNomeClube.error = null
        }

        if (cidade.isEmpty()) {

            binding.tilCidade.error =
                "Informe a cidade"

            binding.edtCidade.requestFocus()

            return

        } else {
            binding.tilCidade.error = null
        }

        if (estado.isEmpty()) {

            binding.tilEstado.error =
                "Informe o estado"

            binding.edtEstado.requestFocus()

            return

        } else {
            binding.tilEstado.error = null
        }

        val uid = usuarioFirebase.uid

        // =====================================================
        // DADOS PARA O FIRESTORE
        // =====================================================

        val dadosAtualizados = mapOf(
            "nome" to nome,
            "cnpj" to cnpj,
            "cidade" to cidade,
            "estado" to estado,
            "descricao" to descricao,
            "telefone" to telefone
        )

        db.collection("usuarios")
            .document(uid)
            .update(dadosAtualizados)
            .addOnSuccessListener {

                Toast.makeText(
                    requireContext(),
                    "Perfil atualizado com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()

                findNavController().navigateUp()
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao salvar: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
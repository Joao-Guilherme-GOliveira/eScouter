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
import com.example.escouter.model.Usuario
import com.google.gson.Gson

class EditarPerfilClubeFragment : Fragment() {

    private var _binding: FragmentEditarPerfilClubeBinding? = null
    private val binding get() = _binding!!

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

    private fun carregarDados() {

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            android.content.Context.MODE_PRIVATE
        )

        val json = preferences.getString(
            "usuario",
            null
        )

        if (json == null) {

            Toast.makeText(
                requireContext(),
                "Usuário não encontrado.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            val usuario = Gson().fromJson(
                json,
                Usuario::class.java
            )

            // =========================
            // DADOS DO CLUBE
            // =========================

            binding.edtNomeClube.setText(
                usuario.nome
            )

            binding.edtCnpj.setText(
                usuario.cnpj
            )

            binding.edtCidade.setText(
                usuario.cidade
            )

            binding.edtEstado.setText(
                usuario.estado
            )

            binding.edtDescricao.setText(
                usuario.descricao
            )

            binding.edtTelefone.setText(
                usuario.telefone
            )

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "Erro ao carregar os dados do perfil.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun salvarAlteracoes() {

        // =========================
        // PEGAR DADOS DOS CAMPOS
        // =========================

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

        // =========================
        // VALIDAÇÕES
        // =========================

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

        // =========================
        // CARREGAR USUÁRIO ATUAL
        // =========================

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            android.content.Context.MODE_PRIVATE
        )

        val jsonAtual = preferences.getString(
            "usuario",
            null
        )

        if (jsonAtual == null) {

            Toast.makeText(
                requireContext(),
                "Usuário não encontrado.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            val usuarioAtual = Gson().fromJson(
                jsonAtual,
                Usuario::class.java
            )

            // =========================
            // CRIAR USUÁRIO ATUALIZADO
            // =========================

            val usuarioAtualizado = usuarioAtual.copy(
                nome = nome,
                cnpj = cnpj,
                cidade = cidade,
                estado = estado,
                descricao = descricao,
                telefone = telefone
            )

            // =========================
            // CONVERTER PARA JSON
            // =========================

            val novoJson = Gson().toJson(
                usuarioAtualizado
            )

            // =========================
            // SALVAR
            // =========================

            preferences.edit()
                .putString(
                    "usuario",
                    novoJson
                )
                .apply()

            // =========================
            // MENSAGEM
            // =========================

            Toast.makeText(
                requireContext(),
                "Perfil atualizado com sucesso!",
                Toast.LENGTH_SHORT
            ).show()

            // =========================
            // VOLTAR PARA O PERFIL
            // =========================

            findNavController().navigateUp()

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "Erro ao salvar as alterações.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
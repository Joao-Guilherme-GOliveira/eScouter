package com.example.escouter.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentEditarPerfilAtletaBinding
import com.example.escouter.model.Midia
import com.example.escouter.model.Usuario
import com.google.gson.Gson

class EditarPerfilAtletaFragment : Fragment() {

    private var _binding: FragmentEditarPerfilAtletaBinding? = null
    private val binding get() = _binding!!

    private var usuarioAtual: Usuario? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEditarPerfilAtletaBinding.inflate(
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

        carregarUsuario()
        configurarListeners()
    }

    private fun carregarUsuario() {

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = preferences.getString("usuario", null)

        if (json == null) {
            return
        }

        val usuario = Gson().fromJson(
            json,
            Usuario::class.java
        )

        usuarioAtual = usuario

        // Dados que vieram do cadastro
        binding.txtNome.text = usuario.nome
        binding.txtEmail.text = usuario.email

        binding.txtLocalizacao.text =
            "${usuario.cidade} - ${usuario.estado}"

        binding.txtDataNascimento.text =
            "Nascimento: ${usuario.dataNascimento}"


        // Dados que podem ser editados

        binding.edtPosicao.setText(usuario.posicao)

        binding.edtPeso.setText(usuario.peso)

        binding.edtAltura.setText(usuario.altura)

        binding.edtExperiencia.setText(usuario.experiencia)

        binding.edtDescricao.setText(usuario.descricao)

        carregarMidias(usuario.midias)
    }

    private fun configurarListeners() {

        binding.btnSalvar.setOnClickListener {

            salvarAlteracoes()
        }

        binding.txtCancelar.setOnClickListener {

            findNavController().navigateUp()
        }

        binding.btnAdicionarMidia.setOnClickListener {

            // Vamos implementar a seleção da mídia depois.
        }
    }

    private fun salvarAlteracoes() {

        val usuario = usuarioAtual ?: return

        val novoUsuario = usuario.copy(

            posicao = binding.edtPosicao.text
                .toString()
                .trim(),

            peso = binding.edtPeso.text
                .toString()
                .trim(),

            altura = binding.edtAltura.text
                .toString()
                .trim(),

            experiencia = binding.edtExperiencia.text
                .toString()
                .trim(),

            descricao = binding.edtDescricao.text
                .toString()
                .trim()

        )

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = Gson().toJson(novoUsuario)

        preferences.edit()
            .putString("usuario", json)
            .apply()

        usuarioAtual = novoUsuario

        findNavController().navigateUp()
    }

    private fun carregarMidias(midias: List<Midia>) {

        binding.gridMidias.removeAllViews()

        for (midia in midias) {

            val item = layoutInflater.inflate(
                R.layout.item_midia,
                binding.gridMidias,
                false
            )

            val txtNome = item.findViewById<android.widget.TextView>(
                R.id.txtNomeMidia
            )

            val txtDuracao = item.findViewById<android.widget.TextView>(
                R.id.txtDuracaoMidia
            )

            txtNome.text = midia.nome
            txtDuracao.text = midia.duracao

            val params =
                android.widget.GridLayout.LayoutParams()

            params.width = 0
            params.height =
                android.widget.GridLayout.LayoutParams.WRAP_CONTENT

            params.columnSpec =
                android.widget.GridLayout.spec(
                    android.widget.GridLayout.UNDEFINED,
                    1f
                )

            item.layoutParams = params

            binding.gridMidias.addView(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
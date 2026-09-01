package com.example.escouter.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentEditarPerfilAtletaBinding
import com.example.escouter.model.Midia
import com.example.escouter.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilAtletaFragment : Fragment() {

    private var _binding: FragmentEditarPerfilAtletaBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
        configurarListeners()
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

                val usuario =
                    document.toObject(Usuario::class.java)

                if (usuario == null) {

                    Toast.makeText(
                        requireContext(),
                        "Erro ao carregar perfil.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                usuarioAtual = usuario

                preencherCampos(usuario)
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar perfil: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // PREENCHER CAMPOS
    // =========================================================

    private fun preencherCampos(usuario: Usuario) {

        // Dados que não podem ser alterados aqui

        binding.txtNome.text =
            usuario.nome

        binding.txtEmail.text =
            usuario.email

        binding.txtLocalizacao.text =
            "${usuario.cidade} - ${usuario.estado}"

        binding.txtDataNascimento.text =
            "Nascimento: ${usuario.dataNascimento}"


        // Dados editáveis

        binding.edtPosicao.setText(
            usuario.posicao
        )

        binding.edtPeso.setText(
            usuario.peso
        )

        binding.edtAltura.setText(
            usuario.altura
        )

        binding.edtExperiencia.setText(
            usuario.experiencia
        )

        binding.edtDescricao.setText(
            usuario.descricao
        )

        carregarMidias(
            usuario.midias
        )
    }

    // =========================================================
    // LISTENERS
    // =========================================================

    private fun configurarListeners() {

        binding.btnSalvar.setOnClickListener {

            salvarAlteracoes()
        }

        binding.txtCancelar.setOnClickListener {

            findNavController().navigateUp()
        }

        binding.btnAdicionarMidia.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Adição de mídia será implementada depois.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // SALVAR ALTERAÇÕES
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

        val uid = usuarioFirebase.uid

        val dadosAtualizados = hashMapOf<String, Any>(

            "posicao" to binding.edtPosicao.text
                .toString()
                .trim(),

            "peso" to binding.edtPeso.text
                .toString()
                .trim(),

            "altura" to binding.edtAltura.text
                .toString()
                .trim(),

            "experiencia" to binding.edtExperiencia.text
                .toString()
                .trim(),

            "descricao" to binding.edtDescricao.text
                .toString()
                .trim()
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

    // =========================================================
    // MÍDIAS
    // =========================================================

    private fun carregarMidias(
        midias: List<Midia>
    ) {

        binding.gridMidias.removeAllViews()

        for (midia in midias) {

            val item = layoutInflater.inflate(
                R.layout.item_midia,
                binding.gridMidias,
                false
            )

            val txtNome =
                item.findViewById<android.widget.TextView>(
                    R.id.txtNomeMidia
                )

            val txtDuracao =
                item.findViewById<android.widget.TextView>(
                    R.id.txtDuracaoMidia
                )

            txtNome.text =
                midia.nome

            txtDuracao.text =
                midia.duracao

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

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
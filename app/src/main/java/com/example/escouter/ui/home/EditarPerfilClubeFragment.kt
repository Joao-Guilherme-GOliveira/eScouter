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
import coil.load // NOVO
import com.cloudinary.android.MediaManager // NOVO
import com.cloudinary.android.callback.ErrorInfo // NOVO
import com.cloudinary.android.callback.UploadCallback // NOVO
import com.example.escouter.R // NOVO
import com.example.escouter.databinding.FragmentEditarPerfilClubeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilClubeFragment : Fragment() {

    private var _binding: FragmentEditarPerfilClubeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    /*
     * Abre a galeria só com imagens.
     * Ao escolher, mostra a prévia e já envia a logo.
     */
    private val selecionarImagem =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {
                fazerUploadLogo(uri)
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

        // NOVO: clicar na própria logo também abre a galeria
        binding.imgLogoClube.setOnClickListener {
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

                if (_binding == null) return@addOnSuccessListener

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

                // NOVO: carrega a logo salva
                exibirLogo(document.getString("fotoUrl"))
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
    // LOGO DO CLUBE (NOVO)
    // =========================================================

    /*
     * Mostra a logo no círculo.
     * Se não houver logo salva, mantém o ícone padrão (ic_perfil).
     */
    private fun exibirLogo(url: String?) {

        if (_binding == null) return

        if (url.isNullOrEmpty()) {
            binding.imgLogoClube.setImageResource(R.drawable.ic_perfil)
            return
        }

        binding.imgLogoClube.load(url) {
            crossfade(true)
            placeholder(R.drawable.ic_perfil)
            error(R.drawable.ic_perfil)
        }
    }

    /*
     * Envia a imagem escolhida para o Cloudinary e,
     * quando terminar, salva a URL no Firestore.
     */
    private fun fazerUploadLogo(uri: Uri) {

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

        // Garante que o arquivo escolhido é uma imagem
        val tipoMime = requireContext()
            .contentResolver
            .getType(uri)

        if (tipoMime == null || !tipoMime.startsWith("image")) {

            Toast.makeText(
                requireContext(),
                "Selecione uma imagem válida.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Mostra a prévia na hora e bloqueia os botões durante o envio
        binding.imgLogoClube.load(uri) {
            crossfade(true)
        }

        binding.btnAlterarLogo.isEnabled = false
        binding.imgLogoClube.isEnabled = false
        binding.btnSalvar.isEnabled = false

        MediaManager.get()
            .upload(uri)
            .unsigned("escouter_midias")
            .callback(object : UploadCallback {

                override fun onStart(requestId: String) {
                }

                override fun onProgress(
                    requestId: String,
                    bytes: Long,
                    totalBytes: Long
                ) {

                    if (_binding == null) return

                    if (totalBytes > 0) {

                        val progresso =
                            ((bytes * 100) / totalBytes).toInt()

                        binding.btnAlterarLogo.text =
                            "Enviando... $progresso%"
                    }
                }

                override fun onSuccess(
                    requestId: String,
                    resultData: MutableMap<Any?, Any?>
                ) {

                    if (_binding == null) return

                    val url =
                        resultData["secure_url"]?.toString()

                    if (url.isNullOrEmpty()) {

                        restaurarBotoesLogo()

                        Toast.makeText(
                            requireContext(),
                            "Não foi possível obter a URL da logo.",
                            Toast.LENGTH_LONG
                        ).show()

                        // volta para a logo que estava salva
                        carregarDados()

                        return
                    }

                    salvarLogoNoFirestore(uid, url)
                }

                override fun onError(
                    requestId: String,
                    error: ErrorInfo
                ) {

                    if (_binding == null) return

                    restaurarBotoesLogo()

                    Toast.makeText(
                        requireContext(),
                        "Erro no upload da logo: ${error.description}",
                        Toast.LENGTH_LONG
                    ).show()

                    // volta para a logo que estava salva
                    carregarDados()
                }

                override fun onReschedule(
                    requestId: String,
                    error: ErrorInfo
                ) {
                }
            })
            .dispatch()
    }

    private fun salvarLogoNoFirestore(
        uid: String,
        url: String
    ) {

        db.collection("usuarios")
            .document(uid)
            .update("fotoUrl", url)
            .addOnSuccessListener {

                if (_binding == null) return@addOnSuccessListener

                restaurarBotoesLogo()

                exibirLogo(url)

                Toast.makeText(
                    requireContext(),
                    "Logo atualizada!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { erro ->

                if (_binding == null) return@addOnFailureListener

                restaurarBotoesLogo()

                Toast.makeText(
                    requireContext(),
                    "Erro ao salvar logo: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()

                // volta para a logo que estava salva
                carregarDados()
            }
    }

    private fun restaurarBotoesLogo() {

        binding.btnAlterarLogo.text = "Alterar logo"
        binding.btnAlterarLogo.isEnabled = true
        binding.imgLogoClube.isEnabled = true
        binding.btnSalvar.isEnabled = true
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
        // (a logo já é salva automaticamente ao ser escolhida)
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
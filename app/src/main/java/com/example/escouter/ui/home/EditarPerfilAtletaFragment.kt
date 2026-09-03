package com.example.escouter.ui.home

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
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

    /*
     * Abre a galeria permitindo selecionar imagem ou vídeo.
     */
    private val selecionarMidia =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {
                fazerUpload(uri)
            }
        }

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
        carregarMidias()
        configurarListeners()
    }

    private fun configurarListeners() {

        binding.btnSalvar.setOnClickListener {
            salvarAlteracoes()
        }

        binding.txtCancelar.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAdicionarMidia.setOnClickListener {

            selecionarMidia.launch("*/*")
        }
    }

    // ============================================================
    // USUÁRIO
    // ============================================================

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

                val usuario = document.toObject(Usuario::class.java)

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

    private fun preencherCampos(usuario: Usuario) {

        binding.txtNome.text = usuario.nome
        binding.txtEmail.text = usuario.email
        binding.txtLocalizacao.text =
            "${usuario.cidade} - ${usuario.estado}"

        binding.txtDataNascimento.text =
            "Nascimento: ${usuario.dataNascimento}"

        binding.edtPosicao.setText(usuario.posicao)
        binding.edtPeso.setText(usuario.peso)
        binding.edtAltura.setText(usuario.altura)
        binding.edtExperiencia.setText(usuario.experiencia)
        binding.edtDescricao.setText(usuario.descricao)
    }

    // ============================================================
    // SALVAR ALTERAÇÕES DO PERFIL
    // ============================================================

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

            "posicao" to binding.edtPosicao.text.toString().trim(),

            "peso" to binding.edtPeso.text.toString().trim(),

            "altura" to binding.edtAltura.text.toString().trim(),

            "experiencia" to binding.edtExperiencia.text.toString().trim(),

            "descricao" to binding.edtDescricao.text.toString().trim()
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

    // ============================================================
    // SELEÇÃO E UPLOAD DA MÍDIA
    // ============================================================

    private fun fazerUpload(uri: Uri) {

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

        Toast.makeText(
            requireContext(),
            "Enviando mídia...",
            Toast.LENGTH_SHORT
        ).show()

        val nomeArquivo = obterNomeArquivo(uri)

        val requestId = MediaManager.get()
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

                    if (totalBytes > 0) {

                        val progresso =
                            ((bytes * 100) / totalBytes).toInt()

                        binding.btnAdicionarMidia.text =
                            "Enviando... $progresso%"
                    }
                }

                override fun onSuccess(
                    requestId: String,
                    resultData: MutableMap<Any?, Any?>
                ) {

                    binding.btnAdicionarMidia.text =
                        "+  Adicionar mídia"

                    val url =
                        resultData["secure_url"]?.toString()

                    if (url.isNullOrEmpty()) {

                        Toast.makeText(
                            requireContext(),
                            "Não foi possível obter a URL da mídia.",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    val duracao =
                        obterDuracao(uri)

                    salvarMidiaNoFirestore(
                        uid = uid,
                        nome = nomeArquivo,
                        url = url,
                        duracao = duracao
                    )
                }

                override fun onError(
                    requestId: String,
                    error: ErrorInfo
                ) {

                    binding.btnAdicionarMidia.text =
                        "+  Adicionar mídia"

                    Toast.makeText(
                        requireContext(),
                        "Erro no upload: ${error.description}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onReschedule(
                    requestId: String,
                    error: ErrorInfo
                ) {

                }
            })
            .dispatch()
    }

    // ============================================================
    // SALVAR URL NO FIRESTORE
    // ============================================================

    private fun salvarMidiaNoFirestore(
        uid: String,
        nome: String,
        url: String,
        duracao: String
    ) {

        val midia = Midia(
            nome = nome,
            uri = url,
            duracao = duracao,
            usuarioId = uid
        )

        db.collection("midias")
            .add(midia)
            .addOnSuccessListener {

                Toast.makeText(
                    requireContext(),
                    "Mídia adicionada com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()

                carregarMidias()
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao salvar mídia: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ============================================================
    // CARREGAR MÍDIAS DO ATLETA
    // ============================================================

    private fun carregarMidias() {

        val usuarioFirebase = auth.currentUser ?: return

        val uid = usuarioFirebase.uid

        db.collection("midias")
            .whereEqualTo("usuarioId", uid)
            .get()
            .addOnSuccessListener { resultado ->

                val midias = resultado.documents.mapNotNull {
                    it.toObject(Midia::class.java)
                }

                mostrarMidias(midias)
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar mídias: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun mostrarMidias(midias: List<Midia>) {

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

            /*
             * Quando clicar na mídia,
             * abre a URL no navegador/aplicativo compatível.
             */
            item.setOnClickListener {

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(midia.uri)
                )

                startActivity(intent)
            }

            binding.gridMidias.addView(item)
        }
    }

    // ============================================================
    // NOME DO ARQUIVO
    // ============================================================

    private fun obterNomeArquivo(uri: Uri): String {

        var nome = "midia"

        val cursor = requireContext()
            .contentResolver
            .query(
                uri,
                null,
                null,
                null,
                null
            )

        cursor?.use {

            val coluna =
                it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (coluna >= 0 && it.moveToFirst()) {

                nome = it.getString(coluna)
            }
        }

        return nome
    }

    // ============================================================
    // DURAÇÃO DO VÍDEO
    // ============================================================

    private fun obterDuracao(uri: Uri): String {

        try {

            val retriever =
                MediaMetadataRetriever()

            retriever.setDataSource(
                requireContext(),
                uri
            )

            val duracaoString =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )

            retriever.release()

            if (duracaoString == null) {
                return ""
            }

            val duracaoMs =
                duracaoString.toLong()

            val segundosTotal =
                duracaoMs / 1000

            val minutos =
                segundosTotal / 60

            val segundos =
                segundosTotal % 60

            return String.format(
                "%02d:%02d",
                minutos,
                segundos
            )

        } catch (e: Exception) {

            return ""
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
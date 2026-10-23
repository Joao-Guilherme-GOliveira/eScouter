package com.example.escouter.ui.home

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.content.Context
import android.net.Uri
import java.io.File
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.escouter.R
import com.example.escouter.databinding.FragmentEditarPerfilClubeBinding
import com.example.escouter.model.Midia
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilClubeFragment : Fragment() {

    private var _binding: FragmentEditarPerfilClubeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var imagemSelecionada: Uri? = null
    private val midiasSelecionadas = mutableSetOf<String>()
    private var uploadsPendentes = 0

    // Seleciona a logo do clube
    private val selecionarImagem =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri?.let {

                imagemSelecionada = it

                binding.imgLogoClube.setImageURI(it)
            }
        }

    // Seleciona imagem ou vídeo para a galeria de mídias
    private val selecionarMidia =
        registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris: List<Uri> ->

            if (uris.isNotEmpty()) {
                uploadsPendentes += uris.size
                uris.forEach { fazerUpload(it) }
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
        carregarMidias()

        binding.btnAlterarLogo.setOnClickListener {
            selecionarImagem.launch("image/*")
        }

        binding.btnSalvar.setOnClickListener {
            salvarAlteracoes()
        }

        binding.btnAdicionarMidia.setOnClickListener {
            selecionarMidia.launch(arrayOf("image/*", "video/*"))
        }

        binding.btnExcluirSelecionadas.setOnClickListener {
            confirmarExclusaoMidias()
        }

        binding.txtCancelar.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.txtSairConta.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_home_to_autentication)
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

    // ============================================================
    // SELEÇÃO E UPLOAD DA MÍDIA
    // ============================================================

    // Copia o arquivo escolhido para o cache do app antes de enviar.
    // Assim o Cloudinary recebe um arquivo local completo, e não um content:// URI.
    private fun fazerUpload(uri: Uri) {

        val appContext = requireContext().applicationContext
        val nome = obterNomeArquivo(uri)

        Toast.makeText(
            requireContext(),
            "Preparando mídia...",
            Toast.LENGTH_SHORT
        ).show()

        Thread {

            val arquivo = copiarParaCache(appContext, uri, nome)

            activity?.runOnUiThread {

                if (_binding == null) {
                    arquivo?.delete()
                    return@runOnUiThread
                }

                if (arquivo == null) {

                    Toast.makeText(
                        requireContext(),
                        "Não foi possível ler o arquivo selecionado.",
                        Toast.LENGTH_LONG
                    ).show()

                    finalizarUpload()

                } else {

                    enviarArquivo(uri, arquivo)
                }
            }
        }.start()
    }

    private fun copiarParaCache(
        contexto: Context,
        uri: Uri,
        nome: String
    ): File? {

        return try {

            val extensao = nome
                .substringAfterLast('.', "")
                .filter { it.isLetterOrDigit() }

            val sufixo =
                if (extensao.isNotEmpty()) ".$extensao" else ""

            val arquivo = File.createTempFile(
                "upload_",
                sufixo,
                contexto.cacheDir
            )

            val copiou = contexto.contentResolver
                .openInputStream(uri)
                ?.use { entrada ->
                    arquivo.outputStream().use { saida ->
                        entrada.copyTo(saida)
                    }
                    true
                } ?: false

            if (!copiou || arquivo.length() == 0L) {
                arquivo.delete()
                null
            } else {
                arquivo
            }

        } catch (e: Exception) {
            null
        }
    }

    private fun enviarArquivo(uri: Uri, arquivo: File) {

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

        // Descobre se o arquivo é imagem ou vídeo
        val tipoMime = requireContext()
            .contentResolver
            .getType(uri)

        val tipo = if (tipoMime?.startsWith("video") == true) {
            "video"
        } else {
            "imagem"
        }

        Toast.makeText(
            requireContext(),
            "Enviando mídia...",
            Toast.LENGTH_SHORT
        ).show()

        val nomeArquivo = obterNomeArquivo(uri)

        MediaManager.get()
            .upload(arquivo.absolutePath)
            .unsigned("escouter_midias")
            .option("resource_type", "auto")
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

                        _binding?.btnAdicionarMidia?.text =
                            if (uploadsPendentes > 1) "Enviando $uploadsPendentes mídias..." else "Enviando... $progresso%"
                    }
                }

                override fun onSuccess(
                    requestId: String,
                    resultData: MutableMap<Any?, Any?>
                ) {

                    finalizarUpload()
                    arquivo.delete()

                    val url =
                        resultData["secure_url"]?.toString()

                    if (url.isNullOrEmpty()) {

                        context?.let {
                            Toast.makeText(
                                it,
                                "Não foi possível obter a URL da mídia.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        return
                    }

                    // Só calcula duração se for vídeo
                    val duracao = if (tipo == "video") {
                        obterDuracao(uri)
                    } else {
                        ""
                    }

                    // Gera a capa automaticamente para vídeos
                    val thumbnailUri = if (tipo == "video") {
                        gerarThumbnailVideo(url)
                    } else {
                        url
                    }

                    salvarMidiaNoFirestore(
                        uid = uid,
                        nome = nomeArquivo,
                        url = url,
                        duracao = duracao,
                        tipo = tipo,
                        thumbnailUri = thumbnailUri
                    )
                }

                override fun onError(
                    requestId: String,
                    error: ErrorInfo
                ) {

                    finalizarUpload()
                    arquivo.delete()

                    context?.let {
                        Toast.makeText(
                            it,
                            "Erro no upload: ${error.description}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onReschedule(
                    requestId: String,
                    error: ErrorInfo
                ) {
                }
            })
            .dispatch()
    }

    private fun finalizarUpload() {

        if (uploadsPendentes > 0) {
            uploadsPendentes--
        }

        if (uploadsPendentes == 0) {
            _binding?.btnAdicionarMidia?.text =
                "+  Adicionar mídia"
        }
    }

    private fun gerarThumbnailVideo(videoUrl: String): String {

        return videoUrl
            .replace("/upload/", "/upload/so_0/")
            .replace(
                Regex("\\.[^./?]+$"),
                ".jpg"
            )
    }

    // ============================================================
    // SALVAR URL NO FIRESTORE
    // ============================================================

    private fun salvarMidiaNoFirestore(
        uid: String,
        nome: String,
        url: String,
        duracao: String,
        tipo: String,
        thumbnailUri: String
    ) {

        val midia = Midia(
            nome = nome,
            uri = url,
            duracao = duracao,
            usuarioId = uid,
            tipo = tipo,
            thumbnailUri = thumbnailUri
        )

        db.collection("midias")
            .add(midia)
            .addOnSuccessListener {

                context?.let {
                    Toast.makeText(
                        it,
                        "Mídia adicionada com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                if (_binding != null) {
                    carregarMidias()
                }
            }
            .addOnFailureListener { erro ->

                context?.let {
                    Toast.makeText(
                        it,
                        "Erro ao salvar mídia: ${erro.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // ============================================================
    // CARREGAR MÍDIAS DO CLUBE
    // ============================================================

    private fun carregarMidias() {

        val usuarioFirebase = auth.currentUser ?: return

        val uid = usuarioFirebase.uid

        db.collection("midias")
            .whereEqualTo("usuarioId", uid)
            .get()
            .addOnSuccessListener { resultado ->

                if (_binding == null) return@addOnSuccessListener

                val midias = resultado.documents.mapNotNull { documento ->

                    val midia = documento.toObject(Midia::class.java)

                    if (midia != null) {
                        Pair(documento.id, midia)
                    } else {
                        null
                    }
                }

                mostrarMidias(midias)
            }
            .addOnFailureListener { erro ->

                context?.let {
                    Toast.makeText(
                        it,
                        "Erro ao carregar mídias: ${erro.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun atualizarBotaoExcluir() {

        if (midiasSelecionadas.isEmpty()) {

            binding.btnExcluirSelecionadas.visibility =
                View.GONE

        } else {

            binding.btnExcluirSelecionadas.visibility =
                View.VISIBLE

            binding.btnExcluirSelecionadas.text =
                "Excluir ${midiasSelecionadas.size} selecionada(s)"
        }
    }

    private fun confirmarExclusaoMidias() {

        if (midiasSelecionadas.isEmpty()) {
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Excluir mídias")
            .setMessage(
                "Deseja realmente excluir ${midiasSelecionadas.size} mídia(s)?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->

                excluirMidiasSelecionadas()
            }
            .show()
    }

    private fun excluirMidiasSelecionadas() {

        val batch = db.batch()

        for (documentoId in midiasSelecionadas) {

            val referencia = db.collection("midias")
                .document(documentoId)

            batch.delete(referencia)
        }

        batch.commit()
            .addOnSuccessListener {

                context?.let {
                    Toast.makeText(
                        it,
                        "Mídia(s) excluída(s) com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                midiasSelecionadas.clear()

                if (_binding != null) {
                    carregarMidias()
                }
            }
            .addOnFailureListener { erro ->

                context?.let {
                    Toast.makeText(
                        it,
                        "Erro ao excluir: ${erro.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun mostrarDialogEditarNome(
        documentoId: String,
        nomeAtual: String
    ) {

        val input = EditText(requireContext())

        input.setText(nomeAtual)

        input.setSelection(
            input.text.length
        )

        input.setPadding(
            50,
            20,
            50,
            20
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Alterar nome da mídia")
            .setMessage("Digite o novo nome que aparecerá no perfil.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->

                val novoNome =
                    input.text.toString().trim()

                if (novoNome.isEmpty()) {

                    Toast.makeText(
                        requireContext(),
                        "O nome não pode ficar vazio.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                alterarNomeMidia(
                    documentoId,
                    novoNome
                )
            }
            .show()
    }

    private fun alterarNomeMidia(
        documentoId: String,
        novoNome: String
    ) {

        db.collection("midias")
            .document(documentoId)
            .update("nome", novoNome)
            .addOnSuccessListener {

                context?.let {
                    Toast.makeText(
                        it,
                        "Nome alterado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                if (_binding != null) {
                    carregarMidias()
                }
            }
            .addOnFailureListener { erro ->

                context?.let {
                    Toast.makeText(
                        it,
                        "Erro ao alterar nome: ${erro.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun mostrarMidias(
        midias: List<Pair<String, Midia>>
    ) {

        binding.gridMidias.removeAllViews()

        midiasSelecionadas.clear()

        atualizarBotaoExcluir()

        for ((documentoId, midia) in midias) {

            val item = layoutInflater.inflate(
                R.layout.item_midia,
                binding.gridMidias,
                false
            )

            val imgMidia =
                item.findViewById<android.widget.ImageView>(
                    R.id.imgMidia
                )

            val txtNome =
                item.findViewById<android.widget.TextView>(
                    R.id.txtNomeMidia
                )

            val txtDuracao =
                item.findViewById<android.widget.TextView>(
                    R.id.txtDuracaoMidia
                )

            val checkSelecionar =
                item.findViewById<CheckBox>(
                    R.id.checkSelecionar
                )

            val btnEditarNome =
                item.findViewById<android.widget.ImageButton>(
                    R.id.btnEditarNomeMidia
                )

            // Nome
            txtNome.text = midia.nome

            // Duração
            if (midia.tipo == "video") {
                txtDuracao.text = midia.duracao
                txtDuracao.visibility = View.VISIBLE
            } else {
                txtDuracao.visibility = View.GONE
            }

            // Imagem ou thumbnail do vídeo
            val imagemParaMostrar =
                if (midia.thumbnailUri.isNotEmpty()) {
                    midia.thumbnailUri
                } else {
                    midia.uri
                }

            imgMidia.load(imagemParaMostrar) {
                crossfade(true)
                error(R.drawable.ic_video)
            }

            // Selecionar mídia
            checkSelecionar.setOnCheckedChangeListener { _, marcado ->

                if (marcado) {
                    midiasSelecionadas.add(documentoId)
                } else {
                    midiasSelecionadas.remove(documentoId)
                }

                atualizarBotaoExcluir()
            }

            // Editar nome
            btnEditarNome.setOnClickListener {
                mostrarDialogEditarNome(
                    documentoId,
                    midia.nome
                )
            }

            // Abrir mídia
            imgMidia.setOnClickListener {

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(midia.uri)
                )

                startActivity(intent)
            }

            // Configuração do GridLayout
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
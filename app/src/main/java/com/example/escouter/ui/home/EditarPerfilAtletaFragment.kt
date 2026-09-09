package com.example.escouter.ui.home

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import coil.load

class EditarPerfilAtletaFragment : Fragment() {

    private var _binding: FragmentEditarPerfilAtletaBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val midiasSelecionadas = mutableSetOf<String>()
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
        configuraSpinner()
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

        binding.btnExcluirSelecionadas.setOnClickListener {
            confirmarExclusaoMidias()
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

        val posicaoSelecionada =
            (binding.spinnerPosicao.adapter as ArrayAdapter<String>)
                .getPosition(usuario.posicao)

        if (posicaoSelecionada >= 0) {
            binding.spinnerPosicao.setSelection(posicaoSelecionada)
        }

        binding.edtPeso.setText(usuario.peso)

        // ==========================================
        // ALTURA
        // Firestore: 1.86 metros
        // Campo de edição: 186 centímetros
        // ==========================================

        val alturaMetros = usuario.altura.toDoubleOrNull()

        if (alturaMetros != null) {

            val alturaCm = alturaMetros * 100

            binding.edtAltura.setText(
                alturaCm.toInt().toString()
            )

        } else {

            binding.edtAltura.setText("")
        }

        binding.edtDescricao.setText(usuario.descricao)

        val experienciaSelecionada =
            (binding.spinnerExperiencia.adapter as ArrayAdapter<String>)
                .getPosition(usuario.experiencia)

        if (experienciaSelecionada >= 0) {
            binding.spinnerExperiencia.setSelection(experienciaSelecionada)
        }
    }

    //CONFIGURAÇÃO DOS SPINNERS DO PERFIL DE ATLETA
    private fun configuraSpinner() {

        // SPINNER DE POSIÇÃO
        val posicoes = arrayOf(
            "Selecione sua posição",
            "Goleiro",
            "Defensor",
            "Meio-Campo",
            "Atacante"
        )

        val adapterPosicao = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_selecionado,
            posicoes
        )

        adapterPosicao.setDropDownViewResource(
            R.layout.item_spinner_dropdown
        )

        binding.spinnerPosicao.adapter = adapterPosicao


        // SPINNER DE EXPERIÊNCIA
        val experiencia = arrayOf(
            "Selecione sua experiência",
            "Menos de 1 ano",
            "1 a 2 anos",
            "2 a 3 anos",
            "3 a 4 anos",
            "5 a 10 anos",
            "Mais de 10 anos"
        )

        val adapterExperiencia = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_selecionado,
            experiencia
        )

        adapterExperiencia.setDropDownViewResource(
            R.layout.item_spinner_dropdown
        )

        binding.spinnerExperiencia.adapter = adapterExperiencia
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

        // ==============================
        // VALIDAÇÃO DE PESO E ALTURA
        // ==============================

        val peso = binding.edtPeso.text.toString().trim()
        val pesoValor = peso.toDoubleOrNull()

        val altura = binding.edtAltura.text.toString().trim()
        val alturaValor = altura.toDoubleOrNull()

        // Validação do peso
        if (pesoValor == null || pesoValor <= 0) {
            binding.edtPeso.error = "Informe um peso válido"
            binding.edtPeso.requestFocus()
            return
        }

        // Validação da altura
        if (alturaValor == null || alturaValor <= 0) {
            binding.edtAltura.error = "Informe uma altura válida em cm"
            binding.edtAltura.requestFocus()
            return
        }
        //transforma altura para metros

        val alturaMetros = alturaValor/100


        // ==============================
        // VALIDAÇÃO DOS SPINNERS
        // ==============================

        if (binding.spinnerPosicao.selectedItemPosition == 0) {
            Toast.makeText(
                requireContext(),
                "Selecione sua posição",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (binding.spinnerExperiencia.selectedItemPosition == 0) {
            Toast.makeText(
                requireContext(),
                "Selecione sua experiência",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Pega os valores selecionados nos Spinners
        val posicao = binding.spinnerPosicao.selectedItem.toString()
        val experiencia = binding.spinnerExperiencia.selectedItem.toString()

        // ==============================
        // DADOS ATUALIZADOS
        // ==============================

        val dadosAtualizados = hashMapOf<String, Any>(
            "posicao" to posicao,
            "peso" to peso,
            "altura" to alturaMetros.toString(),
            "experiencia" to experiencia,
            "descricao" to binding.edtDescricao.text.toString().trim()
        )

        // ==============================
        // ATUALIZAÇÃO NO FIRESTORE
        // ==============================

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

                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar mídias: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
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

                Toast.makeText(
                    requireContext(),
                    "Mídia(s) excluída(s) com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()

                midiasSelecionadas.clear()

                carregarMidias()
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao excluir: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
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

                Toast.makeText(
                    requireContext(),
                    "Nome alterado com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()

                carregarMidias()
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao alterar nome: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
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

            // =========================
            // SELECIONAR MÍDIA
            // =========================

            checkSelecionar.setOnCheckedChangeListener { _, marcado ->

                if (marcado) {
                    midiasSelecionadas.add(documentoId)
                } else {
                    midiasSelecionadas.remove(documentoId)
                }

                atualizarBotaoExcluir()
            }

            // =========================
            // EDITAR NOME
            // =========================

            btnEditarNome.setOnClickListener {
                mostrarDialogEditarNome(
                    documentoId,
                    midia.nome
                )
            }

            // =========================
            // ABRIR MÍDIA
            // =========================

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
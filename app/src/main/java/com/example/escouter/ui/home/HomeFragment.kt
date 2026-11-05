package com.example.escouter.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.escouter.R
import com.example.escouter.model.InscricaoPeneira
import com.example.escouter.model.Partida
import com.example.escouter.model.Peneira
import com.example.escouter.model.Seguidor
import com.example.escouter.model.Usuario
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var cardPeneira: View
    private lateinit var txtTime: TextView
    private lateinit var txtData: TextView

    private lateinit var btnCriarPeneira: Button
    private lateinit var btnMinhasPeneiras: Button
    private lateinit var btnInscreverPeneira: Button

    private var peneiraExibida: Peneira? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var usuarioAtual: Usuario? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Views
        bottomNavigation =
            view.findViewById(R.id.bottomNavigation)

        cardPeneira =
            view.findViewById(R.id.cardPeneira)

        txtTime =
            view.findViewById(R.id.txtTime)

        txtData =
            view.findViewById(R.id.txtData)

        btnCriarPeneira =
            view.findViewById(R.id.btnCriarPeneira)

        btnMinhasPeneiras =
            view.findViewById(R.id.btnMinhasPeneiras)

        btnInscreverPeneira =
            view.findViewById(R.id.btnInscreverPeneira)

        configurarBottomNavigation()

        carregarUsuario()

        carregarPartidas()
        carregarDestaques()
    }

    // =========================================================
    // USUÁRIO
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

        db.collection("usuarios")
            .document(usuarioFirebase.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    Toast.makeText(
                        requireContext(),
                        "Usuário não encontrado no Firestore.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                val usuario =
                    document.toObject(Usuario::class.java)

                if (usuario == null) {

                    Toast.makeText(
                        requireContext(),
                        "Erro ao carregar usuário.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                usuarioAtual = usuario

                configurarTelaPorTipoDeUsuario()
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar usuário: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // TELA DE ACORDO COM O USUÁRIO
    // =========================================================

    private fun configurarTelaPorTipoDeUsuario() {

        val usuario = usuarioAtual ?: return

        val isClubeOuOlheiro =
            usuario.tipoUsuario.equals(
                "Clube/Olheiro",
                ignoreCase = true
            )

        val isAtleta =
            usuario.tipoUsuario.equals(
                "Atleta",
                ignoreCase = true
            )

        if (isClubeOuOlheiro) {

            btnCriarPeneira.visibility =
                View.VISIBLE

            btnMinhasPeneiras.visibility =
                View.VISIBLE

            btnInscreverPeneira.visibility =
                View.GONE

            btnCriarPeneira.setOnClickListener {
                abrirCriarPeneira()
            }

            btnMinhasPeneiras.setOnClickListener {

                findNavController().navigate(
                    R.id.minhasPeneirasFragment
                )
            }

        } else if (isAtleta) {

            btnCriarPeneira.visibility =
                View.GONE

            btnMinhasPeneiras.visibility =
                View.GONE

            btnInscreverPeneira.visibility =
                View.VISIBLE

            btnInscreverPeneira.setOnClickListener {
                inscreverAtletaNaPeneira()
            }

        } else {

            btnCriarPeneira.visibility =
                View.GONE

            btnMinhasPeneiras.visibility =
                View.GONE

            btnInscreverPeneira.visibility =
                View.GONE
        }

        exibirProximaPeneira()
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private fun configurarBottomNavigation() {

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
                    true
                }

                R.id.nav_perfil -> {

                    findNavController().navigate(
                        R.id.perfilFragment
                    )
                    true
                }

                R.id.nav_partidas -> {
                    findNavController().navigate(R.id.partidasFragment)
                    true
                }

                R.id.nav_pesquisar->{
                    findNavController().navigate(
                        R.id.fragmentBuscaTodos
                    )
                    true
                }

                else -> false
            }
        }
    }

    // =========================================================
    // BUSCAR PENEIRAS NO FIRESTORE
    // =========================================================

    private fun exibirProximaPeneira() {

        db.collection("peneiras")
            .get()
            .addOnSuccessListener { resultado ->

                val peneiras = resultado.documents.mapNotNull {
                    it.toObject(Peneira::class.java)
                }

                if (peneiras.isEmpty()) {

                    cardPeneira.visibility =
                        View.GONE

                    btnInscreverPeneira.visibility =
                        View.GONE

                    peneiraExibida = null

                    return@addOnSuccessListener
                }

                encontrarProximaPeneira(
                    peneiras
                )
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar peneiras: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()

                cardPeneira.visibility =
                    View.GONE
            }
    }

    // =========================================================
    // ENCONTRAR PRÓXIMA PENEIRA
    // =========================================================

    private fun encontrarProximaPeneira(
        peneiras: List<Peneira>
    ) {

        val formato = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )

        val hoje = Calendar.getInstance().apply {

            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

        }.time

        val proximaPeneira =
            peneiras
                .mapNotNull { peneira ->

                    try {

                        val dataPeneira =
                            formato.parse(
                                peneira.data
                            )

                        if (
                            dataPeneira != null &&
                            !dataPeneira.before(hoje)
                        ) {

                            Pair(
                                peneira,
                                dataPeneira
                            )

                        } else {

                            null
                        }

                    } catch (_: Exception) {

                        null
                    }
                }
                .minByOrNull {
                    it.second
                }
                ?.first

        if (proximaPeneira == null) {

            cardPeneira.visibility =
                View.GONE

            btnInscreverPeneira.visibility =
                View.GONE

            peneiraExibida = null

            return
        }

        peneiraExibida =
            proximaPeneira

        cardPeneira.visibility =
            View.VISIBLE

        txtTime.text =
            proximaPeneira.nomeTime

        txtData.text =
            "${proximaPeneira.data} • " +
                    "${proximaPeneira.hora} • " +
                    proximaPeneira.local

        val usuario =
            usuarioAtual

        val isAtleta =
            usuario?.tipoUsuario.equals(
                "Atleta",
                ignoreCase = true
            )

        if (isAtleta) {

            configurarBotaoInscricao(
                usuario,
                proximaPeneira
            )

        } else {

            btnInscreverPeneira.visibility =
                View.GONE
        }
    }

    // =========================================================
    // VERIFICAR INSCRIÇÃO
    // =========================================================

    private fun configurarBotaoInscricao(
        usuario: Usuario?,
        peneira: Peneira
    ) {

        if (usuario == null) {

            btnInscreverPeneira.visibility =
                View.GONE

            return
        }

        db.collection("inscricoesPeneira")
            .whereEqualTo(
                "atletaEmail",
                usuario.email
            )
            .whereEqualTo(
                "nomeTime",
                peneira.nomeTime
            )
            .whereEqualTo(
                "data",
                peneira.data
            )
            .whereEqualTo(
                "hora",
                peneira.hora
            )
            .get()
            .addOnSuccessListener { resultado ->

                btnInscreverPeneira.visibility =
                    View.VISIBLE

                if (!resultado.isEmpty) {

                    btnInscreverPeneira.text =
                        "Já inscrito"

                    btnInscreverPeneira.isEnabled =
                        false

                } else {

                    btnInscreverPeneira.text =
                        "Inscrever-se"

                    btnInscreverPeneira.isEnabled =
                        true
                }
            }
    }

    // =========================================================
    // INSCRIÇÃO
    // =========================================================

    private fun inscreverAtletaNaPeneira() {

        val usuario =
            usuarioAtual

        val peneira =
            peneiraExibida

        if (
            usuario == null ||
            peneira == null
        ) {

            Toast.makeText(
                requireContext(),
                "Não foi possível realizar a inscrição.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Verifica novamente no Firestore
        db.collection("inscricoesPeneira")
            .whereEqualTo(
                "atletaEmail",
                usuario.email
            )
            .whereEqualTo(
                "nomeTime",
                peneira.nomeTime
            )
            .whereEqualTo(
                "data",
                peneira.data
            )
            .whereEqualTo(
                "hora",
                peneira.hora
            )
            .get()
            .addOnSuccessListener { resultado ->

                if (!resultado.isEmpty) {

                    Toast.makeText(
                        requireContext(),
                        "Você já está inscrito nesta peneira.",
                        Toast.LENGTH_SHORT
                    ).show()

                    configurarBotaoInscricao(
                        usuario,
                        peneira
                    )

                    return@addOnSuccessListener
                }

                val novaInscricao =
                    InscricaoPeneira(

                        atletaNome =
                            usuario.nome,

                        atletaEmail =
                            usuario.email,

                        nomeTime =
                            peneira.nomeTime,

                        data =
                            peneira.data,

                        hora =
                            peneira.hora,

                        local =
                            peneira.local
                    )

                db.collection("inscricoesPeneira")
                    .add(novaInscricao)
                    .addOnSuccessListener {

                        Toast.makeText(
                            requireContext(),
                            "Inscrição realizada com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()

                        configurarBotaoInscricao(
                            usuario,
                            peneira
                        )
                    }
                    .addOnFailureListener { erro ->

                        Toast.makeText(
                            requireContext(),
                            "Erro ao realizar inscrição: ${erro.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao verificar inscrição: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // CRIAR PENEIRA
    // =========================================================

    private fun abrirCriarPeneira() {

        val dialog =
            CriarPeneiraFragment()

        dialog.onPeneiraCriada = {

            exibirProximaPeneira()
        }

        dialog.show(
            parentFragmentManager,
            "CriarPeneiraFragment"
        )
    }

    // =========================================================
    // PARTIDAS (PRÓXIMAS E RECENTES)
    // =========================================================

    private fun carregarPartidas() {

        val agora = System.currentTimeMillis()

        // Próximas: de agora em diante, da mais próxima para a mais distante
        db.collection("partidas")
            .whereGreaterThanOrEqualTo("dataHoraMillis", agora)
            .orderBy("dataHoraMillis")
            .limit(3)
            .get()
            .addOnSuccessListener { resultado ->

                val raiz = view ?: return@addOnSuccessListener

                preencherPartidas(
                    container = raiz.findViewById(R.id.containerProximas),
                    mensagem = raiz.findViewById(R.id.txtSemProximas),
                    partidas = resultado.documents.mapNotNull {
                        it.toObject(Partida::class.java)
                    },
                    textoVazio = "Nenhuma partida marcada."
                )
            }
            .addOnFailureListener { erro ->
                avisarErroPartidas(erro)
            }

        // Recentes: antes de agora, da mais recente para a mais antiga
        db.collection("partidas")
            .whereLessThan("dataHoraMillis", agora)
            .orderBy("dataHoraMillis", Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { resultado ->

                val raiz = view ?: return@addOnSuccessListener

                preencherPartidas(
                    container = raiz.findViewById(R.id.containerRecentes),
                    mensagem = raiz.findViewById(R.id.txtSemRecentes),
                    partidas = resultado.documents.mapNotNull {
                        it.toObject(Partida::class.java)
                    },
                    textoVazio = "Nenhuma partida recente."
                )
            }
            .addOnFailureListener { erro ->
                avisarErroPartidas(erro)
            }
    }

    private fun avisarErroPartidas(erro: Exception) {

        if (!isAdded) return

        // se o Logcat mostrar "requires an index", clique no link do erro
        Toast.makeText(
            requireContext(),
            "Erro ao carregar partidas: ${erro.message}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun preencherPartidas(
        container: LinearLayout,
        mensagem: TextView,
        partidas: List<Partida>,
        textoVazio: String
    ) {

        container.removeAllViews()

        if (partidas.isEmpty()) {
            mensagem.text = textoVazio
            mensagem.visibility = View.VISIBLE
            return
        }

        mensagem.visibility = View.GONE

        partidas.forEach { partida ->

            val item = layoutInflater.inflate(
                R.layout.item_partida,
                container,
                false
            )

            item.findViewById<TextView>(R.id.txtTimes).text =
                montarConfronto(partida)

            item.findViewById<TextView>(R.id.txtLocal).text =
                partida.local

            item.findViewById<TextView>(R.id.txtDataHora).text =
                getString(
                    R.string.data_hora_peneira,
                    rotuloDoDia(partida),
                    partida.hora
                )

            container.addView(item)
        }
    }

    // "Casa 2 x 1 Visitante" quando existe placar; senão "Casa x Visitante"
    private fun montarConfronto(partida: Partida): String {

        val golsCasa = partida.golsCasa
        val golsVisitante = partida.golsVisitante

        return if (golsCasa != null && golsVisitante != null) {
            "${partida.timeCasa} $golsCasa x $golsVisitante ${partida.timeVisitante}"
        } else {
            "${partida.timeCasa} x ${partida.timeVisitante}"
        }
    }

    // Hoje / Amanhã / Ontem; nos demais dias mostra a data gravada
    private fun rotuloDoDia(partida: Partida): String {

        if (partida.dataHoraMillis == 0L) return partida.data

        val hoje = Calendar.getInstance().apply { zerarHorario() }
        val dia = Calendar.getInstance().apply {
            timeInMillis = partida.dataHoraMillis
            zerarHorario()
        }

        val diferencaEmDias = Math.round(
            (dia.timeInMillis - hoje.timeInMillis) / 86_400_000.0
        ).toInt()

        return when (diferencaEmDias) {
            0 -> "Hoje"
            1 -> "Amanhã"
            -1 -> "Ontem"
            else -> partida.data
        }
    }

    private fun Calendar.zerarHorario() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // =========================================================
    // DESTAQUES DA SEMANA
    // =========================================================
    // Critério: atletas que mais ganharam seguidores nos últimos
    // 7 dias. Em caso de empate (ou se ninguém ganhou seguidores
    // na semana), vale o total de seguidores.
    // =========================================================

    private data class Destaque(
        val uid: String,
        val atleta: Usuario,
        val naSemana: Int,
        val total: Int
    )

    private fun carregarDestaques() {

        val limiteDaSemana =
            System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

        db.collection("seguidores")
            .get()
            .addOnSuccessListener { resultado ->

                val totais = mutableMapOf<String, Int>()
                val naSemana = mutableMapOf<String, Int>()

                resultado.documents
                    .mapNotNull { it.toObject(Seguidor::class.java) }
                    .filter { it.seguidoId.isNotBlank() }
                    .forEach { relacao ->

                        totais[relacao.seguidoId] =
                            (totais[relacao.seguidoId] ?: 0) + 1

                        if (relacao.criadoEm >= limiteDaSemana) {
                            naSemana[relacao.seguidoId] =
                                (naSemana[relacao.seguidoId] ?: 0) + 1
                        }
                    }

                // mais seguidos primeiro; só os 10 primeiros são consultados
                val ranking = totais.keys
                    .sortedWith(
                        compareByDescending<String> { naSemana[it] ?: 0 }
                            .thenByDescending { totais[it] ?: 0 }
                    )
                    .take(10)

                buscarAtletasDoRanking(ranking, naSemana, totais)
            }
            .addOnFailureListener {
                mostrarDestaques(emptyList())
            }
    }

    private fun buscarAtletasDoRanking(
        ranking: List<String>,
        naSemana: Map<String, Int>,
        totais: Map<String, Int>
    ) {

        if (ranking.isEmpty()) {
            mostrarDestaques(emptyList())
            return
        }

        db.collection("usuarios")
            .whereIn(FieldPath.documentId(), ranking)
            .get()
            .addOnSuccessListener { resultado ->

                val porId = resultado.documents.associate {
                    it.id to it.toObject(Usuario::class.java)
                }

                val destaques = ranking
                    .mapNotNull { uid ->

                        val usuario = porId[uid] ?: return@mapNotNull null

                        if (!usuario.tipoUsuario.equals("Atleta", ignoreCase = true)) {
                            return@mapNotNull null
                        }

                        Destaque(
                            uid = uid,
                            atleta = usuario,
                            naSemana = naSemana[uid] ?: 0,
                            total = totais[uid] ?: 0
                        )
                    }
                    .take(2)

                mostrarDestaques(destaques)
            }
            .addOnFailureListener {
                mostrarDestaques(emptyList())
            }
    }

    private fun mostrarDestaques(destaques: List<Destaque>) {

        val raiz = view ?: return

        raiz.findViewById<TextView>(R.id.txtSemDestaques).visibility =
            if (destaques.isEmpty()) View.VISIBLE else View.GONE

        val cards = listOf(
            R.id.cardJogador1, R.id.cardJogador2
        )
        val fotos = listOf(
            R.id.fotoJogador1, R.id.fotoJogador2
        )
        val nomes = listOf(
            R.id.nomeJogador1, R.id.nomeJogador2
        )
        val infos = listOf(
            R.id.infoJogador1, R.id.infoJogador2
        )
        val chipsSemana = listOf(
            R.id.chipSemanaJogador1, R.id.chipSemanaJogador2
        )
        val chipsTotal = listOf(
            R.id.chipTotalJogador1, R.id.chipTotalJogador2
        )

        cards.indices.forEach { i ->

            val card = raiz.findViewById<View>(cards[i])
            val destaque = destaques.getOrNull(i)

            if (destaque == null) {
                card.visibility = View.GONE
                return@forEach
            }

            card.visibility = View.VISIBLE

            val atleta = destaque.atleta

            raiz.findViewById<TextView>(nomes[i]).text = atleta.nome

            raiz.findViewById<TextView>(infos[i]).text =
                listOfNotNull(
                    atleta.posicao.ifBlank { "Atleta" },
                    calcularIdade(atleta.dataNascimento)?.let { "$it anos" }
                ).joinToString(" • ")

            val chipSemana = raiz.findViewById<TextView>(chipsSemana[i])
            chipSemana.visibility =
                if (destaque.naSemana > 0) View.VISIBLE else View.GONE
            chipSemana.text = "+${destaque.naSemana} na semana"

            raiz.findViewById<TextView>(chipsTotal[i]).text =
                if (destaque.total == 1) "1 seguidor"
                else "${destaque.total} seguidores"

            carregarFotoDoAtleta(
                raiz.findViewById(fotos[i]),
                atleta
            )

            card.setOnClickListener {
                findNavController().navigate(
                    R.id.perfilFragment,
                    Bundle().apply { putString("usuarioId", destaque.uid) }
                )
            }
        }
    }

    // O Usuario não tem foto de perfil própria: usa a primeira
    // imagem das mídias; sem imagem, mostra o ícone padrão.
    private fun carregarFotoDoAtleta(
        foto: ImageView,
        atleta: Usuario
    ) {

        val imagem = atleta.midias.firstOrNull {
            it.tipo == "imagem" && it.uri.isNotBlank()
        }

        if (imagem != null) {

            foto.setPadding(0, 0, 0, 0)

            foto.load(imagem.uri) {
                crossfade(true)
                error(R.drawable.ic_perfil)
            }

        } else {

            val margem = (10 * resources.displayMetrics.density).toInt()

            foto.setPadding(margem, margem, margem, margem)
            foto.setImageResource(R.drawable.ic_perfil)
        }
    }

    // Aceita "dd/MM/yyyy" (app) e "yyyy-MM-dd" (painel Web)
    private fun calcularIdade(dataNascimento: String): Int? {

        val formatos = listOf("dd/MM/yyyy", "yyyy-MM-dd")

        val nascimento = formatos.firstNotNullOfOrNull { formato ->
            try {
                SimpleDateFormat(formato, Locale.getDefault())
                    .apply { isLenient = false }
                    .parse(dataNascimento.trim())
            } catch (e: Exception) {
                null
            }
        } ?: return null

        val dataNasc = Calendar.getInstance().apply { time = nascimento }
        val hoje = Calendar.getInstance()

        var idade = hoje.get(Calendar.YEAR) - dataNasc.get(Calendar.YEAR)

        if (hoje.get(Calendar.DAY_OF_YEAR) < dataNasc.get(Calendar.DAY_OF_YEAR)) {
            idade--
        }

        return if (idade in 0..120) idade else null
    }

}
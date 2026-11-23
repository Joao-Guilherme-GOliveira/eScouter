package com.example.escouter.ui.notificacao

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentNotificacoesBinding
import com.example.escouter.databinding.ItemNotificacaoBinding
import com.example.escouter.model.Partida
import com.example.escouter.model.Peneira
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class NotificacoesFragment : Fragment() {

    private var _binding: FragmentNotificacoesBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    // Uma notificação = um aviso montado a partir de uma peneira ou partida
    private data class Notificacao(
        val id: String,       // identifica o aviso (usado para marcar como lido)
        val tipo: String,     // "peneira" ou "partida"
        val titulo: String,
        val texto: String,
        val quando: Long      // só para ordenar
    )

    private var todas = listOf<Notificacao>()
    private var filtro = "todos"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificacoesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarBottomNavigation()

        binding.btnTodos.setOnClickListener { selecionarFiltro("todos") }
        binding.btnPeneiras.setOnClickListener { selecionarFiltro("peneira") }
        binding.btnPartidas.setOnClickListener { selecionarFiltro("partida") }

        binding.tvMarcarLidas.setOnClickListener {
            prefs().edit()
                .putStringSet(chaveLidas(), todas.map { it.id }.toSet())
                .apply()
            mostrar()
        }

        carregar()
    }

    // =========================================================
    // BUSCAR: partidas e peneiras dos próximos 7 dias
    // =========================================================

    private fun carregar() {

        val agora = System.currentTimeMillis()
        val limite = agora + 7L * 24 * 60 * 60 * 1000

        val lista = mutableListOf<Notificacao>()
        var faltam = 2   // são duas consultas; quando as duas terminam, mostra

        fun terminou() {
            faltam--
            if (faltam == 0) {
                todas = lista.sortedBy { it.quando }
                mostrar()
            }
        }

        // PARTIDAS (mesma consulta que a tela de partidas já usa)
        db.collection("partidas")
            .whereGreaterThanOrEqualTo("dataHoraMillis", agora)
            .whereLessThanOrEqualTo("dataHoraMillis", limite)
            .orderBy("dataHoraMillis")
            .get()
            .addOnSuccessListener { resultado ->
                resultado.documents
                    .mapNotNull { it.toObject(Partida::class.java) }
                    .forEach { p ->
                        lista.add(
                            Notificacao(
                                id = "partida_${p.id}",
                                tipo = "partida",
                                titulo = "Próxima partida em breve",
                                texto = "${p.timeCasa} x ${p.timeVisitante} dia ${p.data} às ${p.hora}",
                                quando = p.dataHoraMillis
                            )
                        )
                    }
                terminou()
            }
            .addOnFailureListener { terminou() }

        // PENEIRAS (a data é texto "dd/MM/yyyy", então filtra aqui no app)
        db.collection("peneiras")
            .get()
            .addOnSuccessListener { resultado ->
                val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                resultado.documents
                    .mapNotNull { it.toObject(Peneira::class.java) }
                    .forEach { p ->
                        val quando = try {
                            formato.parse("${p.data} ${p.hora}")?.time
                        } catch (e: Exception) {
                            null
                        }

                        if (quando != null && quando in agora..limite) {
                            lista.add(
                                Notificacao(
                                    id = "peneira_${p.nomeTime}_${p.data}_${p.hora}",
                                    tipo = "peneira",
                                    titulo = "Peneira em breve",
                                    texto = "${p.nomeTime} fará uma peneira dia ${p.data} às ${p.hora}",
                                    quando = quando
                                )
                            )
                        }
                    }
                terminou()
            }
            .addOnFailureListener { terminou() }
    }

    // =========================================================
    // MOSTRAR A LISTA (respeitando o filtro)
    // =========================================================

    private fun mostrar() {

        val b = _binding ?: return

        b.containerNotificacoes.removeAllViews()

        val lidas = prefs().getStringSet(chaveLidas(), emptySet()) ?: emptySet()
        val lista = todas.filter { filtro == "todos" || it.tipo == filtro }

        b.txtMensagem.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE

        lista.forEach { n ->

            val item = ItemNotificacaoBinding.inflate(
                layoutInflater,
                b.containerNotificacoes,
                false
            )

            item.txtTitulo.text = n.titulo
            item.txtTexto.text = n.texto
            item.imgIcone.setImageResource(
                if (n.tipo == "partida") R.drawable.ic_partidas else R.drawable.ic_flag
            )
            item.viewNaoLida.visibility =
                if (n.id in lidas) View.INVISIBLE else View.VISIBLE

            b.containerNotificacoes.addView(item.root)
        }
    }

    private fun selecionarFiltro(novo: String) {

        val b = _binding ?: return

        filtro = novo

        val azul = ColorStateList.valueOf(Color.parseColor("#2F3FA8"))
        val preto = ColorStateList.valueOf(Color.parseColor("#1F1F1F"))

        b.btnTodos.backgroundTintList = if (novo == "todos") azul else preto
        b.btnPeneiras.backgroundTintList = if (novo == "peneira") azul else preto
        b.btnPartidas.backgroundTintList = if (novo == "partida") azul else preto

        mostrar()
    }

    // =========================================================
    // "LIDAS": guardadas no próprio celular (SharedPreferences)
    // =========================================================

    private fun prefs() =
        requireContext().getSharedPreferences("notificacoes", Context.MODE_PRIVATE)

    // uma lista por usuário, para duas contas no mesmo aparelho não se misturarem
    private fun chaveLidas() =
        "lidas_" + (FirebaseAuth.getInstance().currentUser?.uid ?: "")

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private fun configurarBottomNavigation() {

        // selectedItemId ANTES do listener, senão ele dispara a navegação
        binding.bottomNavigation.selectedItemId = R.id.nav_notif

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_inicio -> {
                    findNavController().navigate(R.id.homeFragment)
                    true
                }

                R.id.nav_pesquisar -> {
                    findNavController().navigate(R.id.fragmentBuscaTodos)
                    true
                }

                R.id.nav_partidas -> {
                    findNavController().navigate(R.id.partidasFragment)
                    true
                }

                R.id.nav_perfil -> {
                    findNavController().navigate(R.id.perfilFragment)
                    true
                }

                R.id.nav_notif -> true

                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
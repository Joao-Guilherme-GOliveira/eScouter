package com.example.escouter.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentPartidasBinding
import com.example.escouter.model.Partida
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PartidasFragment : Fragment() {

    private var _binding: FragmentPartidasBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPartidasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarBottomNavigation()
        configurarBotaoCadastrar()
        carregarPartidas()
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private fun configurarBottomNavigation() {

        // selectedItemId ANTES do listener, senão ele dispara a navegação
        binding.bottomNavigation.selectedItemId = R.id.nav_partidas

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
                R.id.nav_perfil -> {
                    findNavController().navigate(R.id.perfilFragment)
                    true
                }
                R.id.nav_partidas -> true
                else -> false
            }
        }
    }

    // =========================================================
    // BOTÃO CADASTRAR (só para Clube/Olheiro)
    // =========================================================

    private fun configurarBotaoCadastrar() {

        val uid = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->

                val b = _binding ?: return@addOnSuccessListener

                val isClube = doc.getString("tipoUsuario")
                    .equals("Clube/Olheiro", ignoreCase = true)

                if (isClube) {
                    b.btnCadastrarPartida.visibility = View.VISIBLE
                    b.btnCadastrarPartida.setOnClickListener {
                        CriarPartidaFragment()
                            .apply { onPartidaCriada = { carregarPartidas() } }
                            .show(childFragmentManager, "CriarPartida")
                    }
                }
            }
    }

    // =========================================================
    // LISTAR PARTIDAS FUTURAS
    // =========================================================

    private fun carregarPartidas() {

        db.collection("partidas")
            .whereGreaterThanOrEqualTo("dataHoraMillis", System.currentTimeMillis())
            .orderBy("dataHoraMillis")
            .get()
            .addOnSuccessListener { resultado ->

                val b = _binding ?: return@addOnSuccessListener

                b.containerPartidas.removeAllViews()

                val partidas = resultado.documents.mapNotNull {
                    it.toObject(Partida::class.java)
                }

                if (partidas.isEmpty()) {
                    b.cardProximaPartida.visibility = View.GONE
                    b.txtTituloLista.visibility = View.GONE
                    b.txtMensagem.visibility = View.VISIBLE
                    b.txtMensagem.text = "Nenhuma partida marcada."
                    return@addOnSuccessListener
                }

                b.txtMensagem.visibility = View.GONE

                // a mais próxima vai para o card azul de destaque
                exibirProximaPartida(partidas.first())

                // as demais vão para a lista
                val demais = partidas.drop(1)

                b.txtTituloLista.visibility =
                    if (demais.isEmpty()) View.GONE else View.VISIBLE

                demais.forEach { adicionarPartidaNaTela(it) }
            }
            .addOnFailureListener { erro ->

                _binding?.txtMensagem?.apply {
                    visibility = View.VISIBLE
                    text = "Erro ao carregar partidas."
                }

                // se o Logcat mostrar "requires an index", clique no link do erro
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar partidas: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun exibirProximaPartida(partida: Partida) {

        val b = _binding ?: return

        b.cardProximaPartida.visibility = View.VISIBLE

        b.txtProximaTimes.text =
            "${partida.timeCasa} x ${partida.timeVisitante}"

        b.txtProximaData.text = getString(
            R.string.data_hora_peneira,
            partida.data,
            partida.hora
        ) + "\n" + partida.local
    }

    private fun adicionarPartidaNaTela(partida: Partida) {

        val b = _binding ?: return

        val item = layoutInflater.inflate(
            R.layout.item_partida,
            b.containerPartidas,
            false
        )

        item.findViewById<TextView>(R.id.txtTimes).text =
            "${partida.timeCasa} x ${partida.timeVisitante}"

        item.findViewById<TextView>(R.id.txtLocal).text = partida.local

        item.findViewById<TextView>(R.id.txtDataHora).text = getString(
            R.string.data_hora_peneira,
            partida.data,
            partida.hora
        )

        b.containerPartidas.addView(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
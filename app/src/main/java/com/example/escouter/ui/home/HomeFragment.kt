package com.example.escouter.ui.home

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.model.InscricaoPeneira
import com.example.escouter.model.Peneira
import com.example.escouter.model.Usuario
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNavigation = view.findViewById(R.id.bottomNavigation)

        cardPeneira = view.findViewById(R.id.cardPeneira)
        txtTime = view.findViewById(R.id.txtTime)
        txtData = view.findViewById(R.id.txtData)

        btnCriarPeneira = view.findViewById(R.id.btnCriarPeneira)
        btnMinhasPeneiras = view.findViewById(R.id.btnMinhasPeneiras)

        // CORRIGIDO: o nome da variável é btnInscreverPeneira
        btnInscreverPeneira = view.findViewById(R.id.btnInscreverPeneira)

        configurarBottomNavigation()
    }

    override fun onResume() {
        super.onResume()

        if (::bottomNavigation.isInitialized) {
            bottomNavigation.selectedItemId = R.id.nav_inicio
        }

        configurarTelaPorTipoDeUsuario()
    }

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

                else -> false
            }
        }
    }

    private fun configurarTelaPorTipoDeUsuario() {

        val usuario = carregarUsuario()

        if (usuario == null) {
            Toast.makeText(
                requireContext(),
                "Usuário não encontrado.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val isClubeOuOlheiro = usuario.tipoUsuario.equals(
            "Clube/Olheiro",
            ignoreCase = true
        )

        val isAtleta = usuario.tipoUsuario.equals(
            "Atleta",
            ignoreCase = true
        )

        if (isClubeOuOlheiro) {

            btnCriarPeneira.visibility = View.VISIBLE
            btnMinhasPeneiras.visibility = View.VISIBLE
            btnInscreverPeneira.visibility = View.GONE

            btnCriarPeneira.setOnClickListener {
                abrirCriarPeneira()
            }

            btnMinhasPeneiras.setOnClickListener {
                findNavController().navigate(
                    R.id.minhasPeneirasFragment
                )
            }

        } else if (isAtleta) {

            btnCriarPeneira.visibility = View.GONE
            btnMinhasPeneiras.visibility = View.GONE

            btnInscreverPeneira.setOnClickListener {
                inscreverAtletaNaPeneira()
            }

        } else {

            btnCriarPeneira.visibility = View.GONE
            btnMinhasPeneiras.visibility = View.GONE
            btnInscreverPeneira.visibility = View.GONE
        }

        exibirProximaPeneira()
    }

    private fun carregarUsuario(): Usuario? {

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = preferences.getString(
            "usuario",
            null
        ) ?: return null

        return try {
            Gson().fromJson(
                json,
                Usuario::class.java
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun carregarPeneiras(): List<Peneira> {

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = preferences.getString(
            "peneiras",
            null
        ) ?: return emptyList()

        return try {

            val tipoLista =
                object : TypeToken<List<Peneira>>() {}.type

            Gson().fromJson(
                json,
                tipoLista
            ) ?: emptyList()

        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun exibirProximaPeneira() {

        val usuario = carregarUsuario()
        val peneiras = carregarPeneiras()

        if (peneiras.isEmpty()) {
            cardPeneira.visibility = View.GONE
            btnInscreverPeneira.visibility = View.GONE
            peneiraExibida = null
            return
        }

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

        val proximaPeneira = peneiras
            .mapNotNull { peneira ->

                try {

                    val dataPeneira = formato.parse(peneira.data)

                    if (dataPeneira != null && !dataPeneira.before(hoje)) {
                        Pair(peneira, dataPeneira)
                    } else {
                        null
                    }

                } catch (e: Exception) {
                    null
                }
            }
            .minByOrNull { it.second }
            ?.first

        if (proximaPeneira == null) {
            cardPeneira.visibility = View.GONE
            btnInscreverPeneira.visibility = View.GONE
            peneiraExibida = null
            return
        }

        peneiraExibida = proximaPeneira

        cardPeneira.visibility = View.VISIBLE

        txtTime.text = proximaPeneira.nomeTime

        txtData.text =
            "${proximaPeneira.data} • " +
                    "${proximaPeneira.hora} • " +
                    proximaPeneira.local

        val isAtleta = usuario?.tipoUsuario.equals(
            "Atleta",
            ignoreCase = true
        )

        if (isAtleta) {
            configurarBotaoInscricao(
                usuario,
                proximaPeneira
            )
        } else {
            btnInscreverPeneira.visibility = View.GONE
        }
    }

    private fun configurarBotaoInscricao(
        usuario: Usuario?,
        peneira: Peneira
    ) {

        if (usuario == null) {
            btnInscreverPeneira.visibility = View.GONE
            return
        }

        val jaInscrito = carregarInscricoes().any { inscricao ->

            inscricao.atletaEmail == usuario.email &&
                    inscricao.nomeTime == peneira.nomeTime &&
                    inscricao.data == peneira.data &&
                    inscricao.hora == peneira.hora
        }

        btnInscreverPeneira.visibility = View.VISIBLE

        if (jaInscrito) {

            btnInscreverPeneira.text = "Já inscrito"
            btnInscreverPeneira.isEnabled = false

        } else {

            btnInscreverPeneira.text = "Inscrever-se"
            btnInscreverPeneira.isEnabled = true
        }
    }

    private fun inscreverAtletaNaPeneira() {

        val usuario = carregarUsuario()
        val peneira = peneiraExibida

        if (usuario == null || peneira == null) {

            Toast.makeText(
                requireContext(),
                "Não foi possível realizar a inscrição.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val inscricoes = carregarInscricoes()

        val jaInscrito = inscricoes.any { inscricao ->

            inscricao.atletaEmail == usuario.email &&
                    inscricao.nomeTime == peneira.nomeTime &&
                    inscricao.data == peneira.data &&
                    inscricao.hora == peneira.hora
        }

        if (jaInscrito) {

            Toast.makeText(
                requireContext(),
                "Você já está inscrito nesta peneira.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val novaInscricao = InscricaoPeneira(
            atletaNome = usuario.nome,
            atletaEmail = usuario.email,
            nomeTime = peneira.nomeTime,
            data = peneira.data,
            hora = peneira.hora,
            local = peneira.local
        )

        inscricoes.add(novaInscricao)

        salvarInscricoes(inscricoes)

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

    private fun carregarInscricoes(): MutableList<InscricaoPeneira> {

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = preferences.getString(
            "inscricoes",
            null
        ) ?: return mutableListOf()

        return try {

            val tipoLista =
                object : TypeToken<MutableList<InscricaoPeneira>>() {}.type

            Gson().fromJson(
                json,
                tipoLista
            ) ?: mutableListOf()

        } catch (e: Exception) {

            mutableListOf()
        }
    }

    private fun salvarInscricoes(
        inscricoes: MutableList<InscricaoPeneira>
    ) {

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = Gson().toJson(inscricoes)

        preferences.edit()
            .putString("inscricoes", json)
            .apply()
    }

    private fun abrirCriarPeneira() {

        val dialog = CriarPeneiraFragment()

        dialog.onPeneiraCriada = {

            exibirProximaPeneira()
        }

        dialog.show(
            parentFragmentManager,
            "CriarPeneiraFragment"
        )
    }
}
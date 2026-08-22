package com.example.escouter.ui.home


import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.model.Peneira
import com.example.escouter.model.Usuario
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var bottomNavigation: BottomNavigationView

    // card "Próxima Peneira"
    private var cardPeneira: View? = null
    private var txtTime: android.widget.TextView? = null
    private var txtData: android.widget.TextView? = null
    private var btnCriarPeneira: android.widget.Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNavigation = view.findViewById(R.id.bottomNavigation)
        cardPeneira = view.findViewById(R.id.cardPeneira)
        txtTime = view.findViewById(R.id.txtTime)
        txtData = view.findViewById(R.id.txtData)
        btnCriarPeneira = view.findViewById(R.id.btnCriarPeneira)

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
                    true
                }

                R.id.nav_perfil -> {
                    findNavController().navigate(R.id.perfilFragment)
                    true
                }

                else -> false
            }
        }

        bottomNavigation.selectedItemId = R.id.nav_inicio
    }

    override fun onResume() {
        super.onResume()

        // Sempre que voltar para o Home,
        // marca o ícone de início como selecionado.
        if (::bottomNavigation.isInitialized) {
            bottomNavigation.selectedItemId = R.id.nav_inicio
        }
        configuraTelaByTipodeUser()
    }
    private fun configuraTelaByTipodeUser() {

        val usuario = carregarUsuario() ?: return

        val ClubeOrScout = usuario.tipoUsuario.equals("Clube/Olheiro",
            ignoreCase = true)

        if (ClubeOrScout) {
            // Clube/Olheiro: mostra o botão de criar peneira
            btnCriarPeneira?.visibility = View.VISIBLE
            btnCriarPeneira?.setOnClickListener {
                abrirCriarPeneira()
            }
        } else {
            //não aparece para o atleta
            btnCriarPeneira?.visibility = View.GONE
        }

        exibirProximaPeneira()
    }

    private fun carregarUsuario(): Usuario? {

        val context = context ?: return null

        val preferences = context.getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = preferences.getString("usuario", null) ?: return null

        return Gson().fromJson(json, Usuario::class.java)
    }

    private fun carregarPeneiras(): List<Peneira> {

        val context = context ?: return emptyList()

        val preferences = context.getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        val json = preferences.getString("peneiras", null) ?:
        return emptyList()

        val tipoLista = object : TypeToken<List<Peneira>>() {}.type

        return Gson().fromJson(json, tipoLista) ?: emptyList()
    }

    private fun exibirProximaPeneira() {

        val peneiras = carregarPeneiras()

        if (peneiras.isEmpty()) {
            cardPeneira?.visibility = View.GONE
            return
        }

        // Exibe a peneira mais recentemente criada
        val proximaPeneira = peneiras.last()

        cardPeneira?.visibility = View.VISIBLE
        txtTime?.text = proximaPeneira.nomeTime
        txtData?.text = "${proximaPeneira.data} • " +
                "${proximaPeneira.hora} • ${proximaPeneira.local}"
    }

    private fun abrirCriarPeneira() {

        val dialog = CriarPeneiraFragment()

        dialog.onPeneiraCriada = {
            exibirProximaPeneira()
        }

        dialog.show(parentFragmentManager, "CriarPeneiraFragment")
    }
}

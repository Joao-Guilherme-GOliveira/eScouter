package com.example.escouter.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.escouter.R
import com.example.escouter.databinding.FragmentCadastroBinding

class CadastroFragment : Fragment() {

    private var _binding: FragmentCadastroBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCadastroBinding.inflate(inflater, container, false)

        configurarSpinners()

        return binding.root
    }

    private fun configurarSpinners() {

        // Spinner de Estado
        val estadosArray = resources.getStringArray(R.array.estados_brasil)

        val listaComHint = mutableListOf("Selecione um estado")
        listaComHint.addAll(estadosArray)

        val adapterEstado = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.item_spinner_selecionado,
            listaComHint
        ) {

            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {

                val view = super.getDropDownView(
                    position,
                    convertView,
                    parent
                ) as TextView

                view.setTextColor(
                    if (position == 0)
                        Color.GRAY
                    else
                        Color.BLACK
                )

                return view
            }
        }

        adapterEstado.setDropDownViewResource(
            R.layout.item_spinner_dropdown
        )

        binding.spinnerEstado.adapter = adapterEstado


        // Spinner de Tipo de Usuário
        val usuarioArray = resources.getStringArray(
            R.array.tipo_de_usuario
        )

        val listaComHint2 = mutableListOf(
            "Selecione um Tipo de Usuário"
        )

        listaComHint2.addAll(usuarioArray)

        val adapterUsuario = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.item_spinner_selecionado,
            listaComHint2
        ) {

            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {

                val view = super.getDropDownView(
                    position,
                    convertView,
                    parent
                ) as TextView

                view.setTextColor(
                    if (position == 0)
                        Color.GRAY
                    else
                        Color.BLACK
                )

                return view
            }
        }

        adapterUsuario.setDropDownViewResource(
            R.layout.item_spinner_dropdown
        )

        binding.spinnerTipoUsuario.adapter = adapterUsuario
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
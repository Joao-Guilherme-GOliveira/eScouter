package com.example.escouter.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.escouter.databinding.CriaPeneiraBinding
import com.example.escouter.model.Peneira
import com.example.escouter.model.Usuario
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class CriarPeneiraFragment : BottomSheetDialogFragment() {

    private var _binding: CriaPeneiraBinding? = null
    private val binding get() = _binding!!

    // Chamado quando a peneira é salva com sucesso, para o HomeFragment atualizar a tela
    var onPeneiraCriada: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CriaPeneiraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarDatePicker()
        configurarTimePicker()

        binding.btnSalvarPeneira.setOnClickListener {
            salvarPeneira()
        }
    }

    private fun configurarDatePicker() {
        binding.edtDataPeneira.setOnClickListener {

            val calendario = Calendar.getInstance()

            val ano = calendario.get(Calendar.YEAR)
            val mes = calendario.get(Calendar.MONTH)
            val dia = calendario.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                requireContext(), { _, anoSelecionado, mesSelecionado, diaSelecionado ->
                    val data = String.format(
                        "%02d/%02d/%04d",
                        diaSelecionado,
                        mesSelecionado + 1,
                        anoSelecionado
                    )
                    binding.edtDataPeneira.setText(data)
                },
                ano,
                mes,
                dia
            ).show()
        }
    }

    private fun configurarTimePicker() {
        binding.edtHoraPeneira.setOnClickListener {

            val calendario = Calendar.getInstance()

            val horaAtual = calendario.get(Calendar.HOUR_OF_DAY)
            val minutoAtual = calendario.get(Calendar.MINUTE)

            TimePickerDialog(
                requireContext(), { _, horaSelecionada, minutoSelecionado ->
                    val hora = String.format(
                        "%02d:%02d",
                        horaSelecionada,
                        minutoSelecionado
                    )
                    binding.edtHoraPeneira.setText(hora)
                },
                horaAtual,
                minutoAtual,
                true
            ).show()
        }
    }

    private fun salvarPeneira() {

        val nomeTime = binding.edtNomeTime.text.toString().trim()
        val data = binding.edtDataPeneira.text.toString().trim()
        val hora = binding.edtHoraPeneira.text.toString().trim()
        val local = binding.edtLocalPeneira.text.toString().trim()

        if (nomeTime.isEmpty()) {
            binding.edtNomeTime.error = "Preencha o nome do time"
            return
        }

        if (data.isEmpty()) {
            Toast.makeText(requireContext(),
                "Selecione a data da peneira",
                Toast.LENGTH_SHORT).show()
            return
        }

        if (hora.isEmpty()) {
            Toast.makeText(requireContext(),
                "Selecione o horário da peneira",
                Toast.LENGTH_SHORT).show()
            return
        }

        if (local.isEmpty()) {
            binding.edtLocalPeneira.error = "Preencha o local da peneira"
            return
        }

        val preferences = requireContext().getSharedPreferences(
            "eScouter",
            Context.MODE_PRIVATE
        )

        // Pega o e-mail do clube logado, para vincular a peneira a ele
        val jsonUsuario = preferences.getString("usuario", null)
        val emailClube = if (jsonUsuario != null) {
            Gson().fromJson(jsonUsuario, Usuario::class.java).email
        } else {
            ""
        }

        val novaPeneira = Peneira(
            nomeTime = nomeTime,
            hora = hora,
            data = data,
            local = local,
            emailClube = emailClube
        )

        // Carrega a lista já salva
        val tipoLista = object : TypeToken<MutableList<Peneira>>() {}.type
        val jsonPeneiras = preferences.getString("peneiras", null)

        val listaPeneiras: MutableList<Peneira> = if (jsonPeneiras != null) {
            Gson().fromJson(jsonPeneiras, tipoLista)
        } else {
            mutableListOf()
        }

        listaPeneiras.add(novaPeneira)

        // Salva a lista atualizada
        preferences.edit()
            .putString("peneiras", Gson().toJson(listaPeneiras))
            .apply()

        Toast.makeText(
            requireContext(),
            "Peneira criada com sucesso!",
            Toast.LENGTH_SHORT
        ).show()

        onPeneiraCriada?.invoke()

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
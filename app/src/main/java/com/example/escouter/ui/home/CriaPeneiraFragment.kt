package com.example.escouter.ui.home

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import android.widget.Toast
import com.example.escouter.R
import com.example.escouter.databinding.CriaPeneiraBinding
import com.example.escouter.model.Peneira
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar

class CriarPeneiraFragment : BottomSheetDialogFragment() {

    private var _binding: CriaPeneiraBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Chamado quando a peneira é salva com sucesso
    var onPeneiraCriada: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = CriaPeneiraBinding.inflate(
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

        configurarDatePicker()
        configurarTimePicker()
        configurarNomeClubeImutavel()

        binding.btnSalvarPeneira.setOnClickListener {
            salvarPeneira()
        }
    }

    // =========================================================
    // NOME DO CLUBE (imutável — vem do perfil do usuário logado)
    // =========================================================

    private fun configurarNomeClubeImutavel() {

        // Trava o campo: sem foco, sem clique, sem teclado, sem cursor.
        // O texto continua com a mesma cor/estilo do EditText normal.
        binding.edtNomeTime.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            isCursorVisible = false
            keyListener = null
            setText("Carregando...")
        }

        val uid = auth.currentUser?.uid

        if (uid == null) {
            binding.edtNomeTime.setText("")
            return
        }

        db.collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->

                val nomeClube =
                    documento.getString("nome") ?: ""

                binding.edtNomeTime.setText(nomeClube)
            }
            .addOnFailureListener {
                binding.edtNomeTime.setText("")
            }
    }

    // =========================================================
    // DATA
    // =========================================================

    private fun configurarDatePicker() {

        binding.edtDataPeneira.setOnClickListener {

            val calendario = Calendar.getInstance()

            val ano = calendario.get(Calendar.YEAR)
            val mes = calendario.get(Calendar.MONTH)
            val dia = calendario.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                requireContext(),
                { _, anoSelecionado, mesSelecionado, diaSelecionado ->

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

    // =========================================================
    // HORA
    // =========================================================

    private fun configurarTimePicker() {

        binding.edtHoraPeneira.setOnClickListener {

            val calendario = Calendar.getInstance()

            val horaAtual =
                calendario.get(Calendar.HOUR_OF_DAY)

            val minutoAtual =
                calendario.get(Calendar.MINUTE)

            val view = LayoutInflater
                .from(requireContext())
                .inflate(
                    R.layout.timer_peneira,
                    null
                )

            val timePicker =
                view.findViewById<TimePicker>(
                    R.id.timerPickerPeneira
                )

            timePicker.setIs24HourView(true)

            timePicker.hour = horaAtual
            timePicker.minute = minutoAtual

            AlertDialog.Builder(requireContext())
                .setTitle("Selecione o horário da peneira")
                .setView(view)
                .setPositiveButton("OK") { _, _ ->

                    val hora = String.format(
                        "%02d:%02d",
                        timePicker.hour,
                        timePicker.minute
                    )

                    binding.edtHoraPeneira.setText(hora)
                }
                .setNegativeButton(
                    "Cancelar",
                    null
                )
                .show()
        }
    }

    // =========================================================
    // SALVAR PENEIRA
    // =========================================================

    private fun salvarPeneira() {

        val nomeTime =
            binding.edtNomeTime.text
                .toString()
                .trim()

        val data =
            binding.edtDataPeneira.text
                .toString()
                .trim()

        val hora =
            binding.edtHoraPeneira.text
                .toString()
                .trim()

        val local =
            binding.edtLocalPeneira.text
                .toString()
                .trim()

        // =====================================================
        // VALIDAÇÕES
        // =====================================================

        if (nomeTime.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "Não foi possível carregar o nome do clube. Tente novamente.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (data.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "Selecione a data da peneira",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (hora.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "Selecione o horário da peneira",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (local.isEmpty()) {

            binding.edtLocalPeneira.error =
                "Preencha o local da peneira"

            binding.edtLocalPeneira.requestFocus()

            return
        }

        // =====================================================
        // VERIFICAR USUÁRIO LOGADO
        // =====================================================

        val usuario = auth.currentUser

        if (usuario == null) {

            Toast.makeText(
                requireContext(),
                "Nenhum usuário está logado.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // E-mail do clube logado
        val emailClube =
            usuario.email ?: ""

        // =====================================================
        // CRIAR OBJETO PENEIRA
        // =====================================================

        val novaPeneira = Peneira(

            nomeTime = nomeTime,

            hora = hora,

            data = data,

            local = local,

            emailClube = emailClube
        )

        // =====================================================
        // SALVAR NO FIRESTORE
        // =====================================================

        db.collection("peneiras")
            .add(novaPeneira)
            .addOnSuccessListener {

                Toast.makeText(
                    requireContext(),
                    "Peneira criada com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()

                // Atualiza o HomeFragment
                onPeneiraCriada?.invoke()

                // Fecha o BottomSheet
                dismiss()
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao criar peneira: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
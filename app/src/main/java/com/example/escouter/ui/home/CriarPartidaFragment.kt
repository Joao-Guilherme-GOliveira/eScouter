package com.example.escouter.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.escouter.databinding.FragmentCriarPartidaBinding
import com.example.escouter.model.Partida
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class CriarPartidaFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCriarPartidaBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // guarda a data + hora escolhidas
    private val calendario: Calendar = Calendar.getInstance().apply {
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // chamado quando a partida é salva com sucesso
    var onPartidaCriada: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCriarPartidaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preencherTimeCasa()

        binding.edtDataPartida.setOnClickListener { escolherData() }
        binding.edtHoraPartida.setOnClickListener { escolherHora() }
        binding.btnSalvarPartida.setOnClickListener { salvarPartida() }
    }

    // time da casa = nome do clube logado (campo travado, igual à peneira)
    private fun preencherTimeCasa() {
        binding.edtTimeCasa.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            isCursorVisible = false
            keyListener = null
            setText("Carregando...")
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            binding.edtTimeCasa.setText("")
            return
        }

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                _binding?.edtTimeCasa?.setText(doc.getString("nome") ?: "")
            }
            .addOnFailureListener {
                _binding?.edtTimeCasa?.setText("")
            }
    }

    private fun escolherData() {
        val hoje = Calendar.getInstance()

        val dialog = DatePickerDialog(
            requireContext(),
            { _, ano, mes, dia ->
                calendario.set(ano, mes, dia)
                binding.edtDataPartida.setText(
                    String.format("%02d/%02d/%04d", dia, mes + 1, ano)
                )
            },
            hoje.get(Calendar.YEAR),
            hoje.get(Calendar.MONTH),
            hoje.get(Calendar.DAY_OF_MONTH)
        )

        // não permite datas passadas
        dialog.datePicker.minDate = System.currentTimeMillis() - 1000
        dialog.show()
    }

    private fun escolherHora() {
        val agora = Calendar.getInstance()

        TimePickerDialog(
            requireContext(),
            { _, hora, minuto ->
                calendario.set(Calendar.HOUR_OF_DAY, hora)
                calendario.set(Calendar.MINUTE, minuto)
                binding.edtHoraPartida.setText(
                    String.format("%02d:%02d", hora, minuto)
                )
            },
            agora.get(Calendar.HOUR_OF_DAY),
            agora.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun aviso(mensagem: String) {
        Toast.makeText(requireContext(), mensagem, Toast.LENGTH_SHORT).show()
    }

    private fun salvarPartida() {
        val casa = binding.edtTimeCasa.text.toString().trim()
        val visitante = binding.edtTimeVisitante.text.toString().trim()
        val data = binding.edtDataPartida.text.toString().trim()
        val hora = binding.edtHoraPartida.text.toString().trim()
        val local = binding.edtLocalPartida.text.toString().trim()

        // ---------- validações ----------
        if (casa.isEmpty()) {
            aviso("Não foi possível carregar o nome do clube. Tente novamente.")
            return
        }

        if (visitante.isEmpty()) {
            binding.edtTimeVisitante.error = "Informe o time visitante"
            binding.edtTimeVisitante.requestFocus()
            return
        }

        if (visitante.equals(casa, ignoreCase = true)) {
            binding.edtTimeVisitante.error = "Os times precisam ser diferentes"
            binding.edtTimeVisitante.requestFocus()
            return
        }

        if (data.isEmpty()) {
            aviso("Selecione a data da partida")
            return
        }

        if (hora.isEmpty()) {
            aviso("Selecione o horário da partida")
            return
        }

        if (local.isEmpty()) {
            binding.edtLocalPartida.error = "Preencha o local da partida"
            binding.edtLocalPartida.requestFocus()
            return
        }

        if (calendario.timeInMillis <= System.currentTimeMillis()) {
            aviso("Escolha uma data e um horário futuros")
            return
        }

        val usuario = auth.currentUser
        if (usuario == null) {
            aviso("Nenhum usuário está logado.")
            return
        }

        // ---------- salvar ----------
        val partida = Partida(
            timeCasa = casa,
            timeVisitante = visitante,
            data = data,
            hora = hora,
            local = local,
            dataHoraMillis = calendario.timeInMillis,
            emailClube = usuario.email ?: "",
            clubeUid = usuario.uid
        )

        binding.btnSalvarPartida.isEnabled = false // evita clique duplo

        db.collection("partidas")
            .add(partida)
            .addOnSuccessListener {
                aviso("Partida cadastrada com sucesso!")
                onPartidaCriada?.invoke()
                dismiss()
            }
            .addOnFailureListener { erro ->
                _binding?.btnSalvarPartida?.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Erro ao cadastrar partida: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
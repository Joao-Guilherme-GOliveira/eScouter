package com.example.escouter.ui.partidas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.example.escouter.R
import com.example.escouter.data.LocalRepository
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

    // locais já registrados (nomes oficiais) e estado do carregamento
    private var locais: List<String> = emptyList()
    private var locaisCarregados = false

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
        carregarLocais()

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

    // =========================================================
    // LOCAL (spinner com locais já registrados)
    // =========================================================

    private fun carregarLocais() {
        binding.spinnerLocal.isEnabled = false

        LocalRepository.listar(
            onSucesso = { lista ->
                if (_binding == null) return@listar
                locais = lista
                locaisCarregados = true
                configurarSpinnerLocal()
            },
            onErro = {
                if (_binding == null) return@listar
                // sem conexão/permissão: deixa o clube digitar o local
                locais = emptyList()
                locaisCarregados = true
                configurarSpinnerLocal()
            }
        )
    }

    private fun configurarSpinnerLocal() {

        // Nenhum local registrado ainda: só o campo de texto
        if (locais.isEmpty()) {
            binding.spinnerLocal.visibility = View.GONE
            binding.edtLocalNovo.visibility = View.VISIBLE
            binding.txtLocalInfo.text =
                "Nenhum local cadastrado ainda. O que você digitar ficará salvo para as próximas partidas"
            return
        }

        val itens = mutableListOf("Selecione o local")
        itens.addAll(locais)
        itens.add(ITEM_NOVO_LOCAL)

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.item_spinner_selecionado,
            itens
        ) {
            override fun isEnabled(position: Int): Boolean = position != 0

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(if (position == 0) Color.GRAY else Color.BLACK)
                return view
            }
        }
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)

        binding.spinnerLocal.adapter = adapter
        binding.spinnerLocal.isEnabled = true
        binding.spinnerLocal.visibility = View.VISIBLE

        binding.spinnerLocal.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val novo = position == itens.lastIndex
                    binding.edtLocalNovo.visibility = if (novo) View.VISIBLE else View.GONE
                    if (novo) binding.edtLocalNovo.requestFocus()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    // true quando o clube vai digitar um local (lista vazia ou "+ Novo local")
    private fun digitandoLocalNovo(): Boolean =
        locais.isEmpty() || binding.spinnerLocal.selectedItemPosition == locais.size + 1

    // =========================================================
    // DATA / HORA
    // =========================================================

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
        if (!isAdded) return
        Toast.makeText(requireContext(), mensagem, Toast.LENGTH_SHORT).show()
    }

    // =========================================================
    // SALVAR
    // =========================================================

    private fun salvarPartida() {
        val casa = binding.edtTimeCasa.text.toString().trim()
        val visitante = binding.edtTimeVisitante.text.toString().trim()
        val data = binding.edtDataPartida.text.toString().trim()
        val hora = binding.edtHoraPartida.text.toString().trim()

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

        if (!locaisCarregados) {
            aviso("Carregando os locais, aguarde um instante")
            return
        }

        // local escolhido na lista (já padronizado) ou digitado agora
        val novoLocal = digitandoLocalNovo()
        val local: String

        if (novoLocal) {
            local = binding.edtLocalNovo.text.toString().trim()

            if (local.isEmpty()) {
                binding.edtLocalNovo.error = "Preencha o local da partida"
                binding.edtLocalNovo.requestFocus()
                return
            }
        } else {
            val posicao = binding.spinnerLocal.selectedItemPosition

            if (posicao <= 0) {
                aviso("Selecione o local da partida")
                return
            }

            local = locais[posicao - 1]
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

        if (!novoLocal) {
            gravarPartida(partida)
            return
        }

        // Local digitado: registra (ou reaproveita o que já existe com o mesmo
        // nome, ignorando maiúsculas, acentos e espaços) e salva a partida
        // com o nome oficial.
        LocalRepository.registrar(
            nomeDigitado = local,
            onSucesso = { nomeOficial ->
                if (_binding == null) return@registrar
                gravarPartida(partida.copy(local = nomeOficial))
            },
            onErro = { erro ->
                if (_binding == null) return@registrar
                binding.btnSalvarPartida.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Erro ao registrar o local: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun gravarPartida(partida: Partida) {
        db.collection("partidas")
            .add(partida)
            .addOnSuccessListener {
                aviso("Partida cadastrada com sucesso!")
                onPartidaCriada?.invoke()
                dismiss()
            }
            .addOnFailureListener { erro ->
                _binding?.btnSalvarPartida?.isEnabled = true
                if (!isAdded) return@addOnFailureListener
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

    private companion object {
        const val ITEM_NOVO_LOCAL = "+ Novo local"
    }
}
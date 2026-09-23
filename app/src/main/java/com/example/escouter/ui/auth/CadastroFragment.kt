package com.example.escouter.ui.auth

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.R
import com.example.escouter.databinding.FragmentCadastroBinding
import com.example.escouter.model.Usuario
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import com.example.escouter.data.IbgeCliente
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CadastroFragment : Fragment() {

    private var _binding: FragmentCadastroBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Idade mínima permitida para o tipo "Atleta"
    private val IDADE_MINIMA = 13

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCadastroBinding.inflate(
            inflater,
            container,
            false
        )

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()


        configurarSpinners()
        configurarCidade()
        configurarDataNascimento()
        configurarBotoes()

        mostrarPasso(1)

        return binding.root
    }

    // =========================================================
    // HELPERS DE TIPO DE USUÁRIO
    // =========================================================

    private fun isAtleta(): Boolean {
        val tipo = binding.spinnerTipoUsuario.selectedItem?.toString() ?: ""
        return tipo.equals("Atleta", ignoreCase = true)
    }

    private fun isClube(): Boolean {
        val tipo = binding.spinnerTipoUsuario.selectedItem?.toString() ?: ""
        return tipo.equals("Clube/Olheiro", ignoreCase = true)
    }

    private fun cpfValido(cpf: String): Boolean {
        val cpfLimpo = cpf.replace(Regex("[^0-9]"), "")
        if (cpfLimpo.length != 11) return false
        if (cpfLimpo.all { it == cpfLimpo[0] }) return false

        val numeros = cpfLimpo.map { it.toString().toInt() }

        // Primeiro dígito verificador
        var soma = 0
        for (i in 0..8) soma += numeros[i] * (10 - i)
        var resto = soma % 11
        val digito1 = if (resto < 2) 0 else 11 - resto
        if (numeros[9] != digito1) return false

        // Segundo dígito verificador
        soma = 0
        for (i in 0..9) soma += numeros[i] * (11 - i)
        resto = soma % 11
        val digito2 = if (resto < 2) 0 else 11 - resto
        if (numeros[10] != digito2) return false

        return true
    }

    // =========================================================
    // CÁLCULO DE IDADE
    // =========================================================

    private fun calcularIdade(dataNascimento: Date): Int {

        val hoje = Calendar.getInstance()
        val nascimento = Calendar.getInstance()
        nascimento.time = dataNascimento

        var idade = hoje.get(Calendar.YEAR) - nascimento.get(Calendar.YEAR)

        if (hoje.get(Calendar.DAY_OF_YEAR) < nascimento.get(Calendar.DAY_OF_YEAR)) {
            idade--
        }

        return idade
    }

    // =========================================================
    // DATA DE NASCIMENTO / FUNDAÇÃO
    // =========================================================

    private fun configurarDataNascimento() {

        binding.edtDataNascimento.setOnClickListener {

            val calendario = Calendar.getInstance()

            val ano = calendario.get(Calendar.YEAR)
            val mes = calendario.get(Calendar.MONTH)
            val dia = calendario.get(Calendar.DAY_OF_MONTH)

            val dialog = DatePickerDialog(
                requireContext(),
                { _, anoSelecionado, mesSelecionado, diaSelecionado ->

                    val calendarioSelecionado = Calendar.getInstance()
                    calendarioSelecionado.set(
                        anoSelecionado,
                        mesSelecionado,
                        diaSelecionado
                    )

                    // Restrição de idade mínima somente para Atleta
                    if (isAtleta()) {

                        val idade = calcularIdade(calendarioSelecionado.time)

                        if (idade < IDADE_MINIMA) {

                            binding.edtDataNascimento.setText("")

                            Toast.makeText(
                                requireContext(),
                                "Cadastro não permitido para menores de $IDADE_MINIMA anos",
                                Toast.LENGTH_LONG
                            ).show()

                            return@DatePickerDialog
                        }
                    }

                    val data = String.format(
                        "%02d/%02d/%04d",
                        diaSelecionado,
                        mesSelecionado + 1,
                        anoSelecionado
                    )

                    binding.edtDataNascimento.setText(data)
                },
                ano,
                mes,
                dia
            )

            dialog.show()
        }
    }

    //CONFIGURAR CIDADE
    private var mapaSigla: Map<String, String> = emptyMap()

    private fun configurarCidade(){
        viewLifecycleOwner.lifecycleScope.launch {
            try{
                val estados = IbgeCliente.service.getEstados()
                mapaSigla = estados.associate { it.nome to it.sigla }
            } catch (e: Exception) {

            }
        }
        binding.spinnerEstado.onItemSelectedListener= object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 0) {
                    binding.edtCidade.setText("")
                    binding.edtCidade.isEnabled = false
                    binding.edtCidade.hint = "Selecione um estado primeiro"
                    return
                }
                val nomeEstado =
                    binding.spinnerEstado.selectedItem?.toString() ?: return
                carregarCidades(nomeEstado)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

    }
    private fun carregarCidades(nomeEstado: String){
        binding.edtCidade.setText("")
        binding.edtCidade.isEnabled = false
        binding.edtCidade.hint = "Carregando cidades..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {

                // Garante que o mapa de siglas já foi carregado
                var sigla = mapaSigla[nomeEstado]

                if (sigla == null) {
                    val estados = IbgeCliente.service.getEstados()
                    mapaSigla = estados.associate { it.nome to it.sigla }
                    sigla = mapaSigla[nomeEstado]
                }
                if (sigla == null) {
                    Toast.makeText(
                        requireContext(),
                        "Não foi possível identificar o estado selecionado",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.edtCidade.hint = "Selecione um estado primeiro"
                    return@launch
                }
                val cidades = IbgeCliente.service.getCidades(sigla)
                val nomesCidades = cidades.map { it.nome }
                val adapterCidade = ArrayAdapter(
                    requireContext(),
                    R.layout.item_spinner_dropdown,
                    nomesCidades
                )
                binding.edtCidade.setAdapter(adapterCidade)
                binding.edtCidade.isEnabled = true
                binding.edtCidade.hint = "Ex: ${nomesCidades.firstOrNull() ?: "Digite a cidade"}"
                binding.edtCidade.setOnClickListener {
                    binding.edtCidade.showDropDown()
                }
            } catch (e: Exception) {
                binding.edtCidade.hint = "Erro ao carregar cidades"
                Toast.makeText(
                    requireContext(),
                    "Não foi possível carregar as cidades. Verifique sua conexão.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    // =========================================================
    // BOTÕES
    // =========================================================

    private fun configurarBotoes() {

        // PASSO 1 → PASSO 2
        binding.btnProximo1.setOnClickListener {

            if (validarPasso1()) {
                mostrarPasso(2)
            }
        }

        // PASSO 2 → PASSO 1
        binding.btnVoltar2.setOnClickListener {
            mostrarPasso(1)
        }

        // PASSO 2 → PASSO 3
        binding.btnProximo2.setOnClickListener {

            if (validarPasso2()) {
                mostrarPasso(3)
            }
        }

        // PASSO 3 → PASSO 2
        binding.btnVoltar3.setOnClickListener {
            mostrarPasso(2)
        }

        // CRIAR CONTA
        binding.btnCriarConta.setOnClickListener {

            if (validarPasso3()) {
                criarConta()
            }
        }

        // IR PARA LOGIN
        binding.tvJaPossuiConta.setOnClickListener {

            findNavController().navigate(
                R.id.action_cadastroFragment_to_loginFragment
            )
        }
    }

    // =========================================================
    // CONTROLE DOS PASSOS
    // =========================================================

    private fun mostrarPasso(numero: Int) {

        binding.layoutPasso1.visibility =
            if (numero == 1) View.VISIBLE else View.GONE

        binding.layoutPasso2.visibility =
            if (numero == 2) View.VISIBLE else View.GONE

        binding.layoutPasso3.visibility =
            if (numero == 3) View.VISIBLE else View.GONE

        binding.txtProgresso.text =
            "Passo $numero de 3"

        binding.progressCadastro.progress = numero
    }

    // =========================================================
    // VALIDAÇÃO PASSO 1
    // =========================================================

    private fun validarPasso1(): Boolean {

        val nome = binding.edtNome.text
            .toString()
            .trim()

        val email = binding.edtEmail.text
            .toString()
            .trim()

        // Tipo de usuário
        if (binding.spinnerTipoUsuario.selectedItemPosition == 0) {

            val textErro =
                binding.spinnerTipoUsuario.selectedView as? TextView

            textErro?.error = "Selecione um tipo de usuário"

            Toast.makeText(
                requireContext(),
                "Selecione um tipo de usuário",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        // Nome
        if (nome.isEmpty()) {

            binding.edtNome.error =
                "Preencha o campo de nome"

            binding.edtNome.requestFocus()

            return false
        }

        // Email vazio
        if (email.isEmpty()) {

            binding.edtEmail.error =
                "Preencha o campo de email"

            binding.edtEmail.requestFocus()

            return false
        }

        // Validação do email
        val regexEmail = Regex(
            "^[A-Za-z0-9._%+-]+@(gmail|hotmail|outlook|yahoo)\\.com$"
        )

        if (!regexEmail.matches(email)) {

            binding.edtEmail.error =
                "Use um email válido"

            binding.edtEmail.requestFocus()

            return false
        }

        return true
    }
    private fun validarPasso2(): Boolean {

        val dataNascimento =
            binding.edtDataNascimento.text
                .toString()
                .trim()

        val cidade =
            binding.edtCidade.text
                .toString()
                .trim()

        val documento =
            binding.edtDocumento.text
                .toString()
                .trim()
                .replace(Regex("[^0-9]"), "") // <- restaurado

        // Estado
        if (binding.spinnerEstado.selectedItemPosition == 0) {

            val textErro =
                binding.spinnerEstado.selectedView as? TextView

            textErro?.error = "Selecione um estado"

            Toast.makeText(
                requireContext(),
                "Selecione um estado",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        // Data
        if (dataNascimento.isEmpty()) {

            binding.edtDataNascimento.error =
                "Preencha a data"

            binding.edtDataNascimento.requestFocus()

            return false
        }

        // Segunda checagem de idade mínima (segurança extra além do DatePicker)
        if (isAtleta()) {

            try {

                val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val data = formato.parse(dataNascimento)

                if (data != null && calcularIdade(data) < IDADE_MINIMA) {

                    binding.edtDataNascimento.error =
                        "Cadastro não permitido para menores de $IDADE_MINIMA anos"

                    Toast.makeText(
                        requireContext(),
                        "Cadastro não permitido para menores de $IDADE_MINIMA anos",
                        Toast.LENGTH_LONG
                    ).show()

                    return false
                }

            } catch (e: Exception) {
                // Se não conseguir interpretar a data, deixa a validação de formato seguir normalmente
            }
        }

        // Documento vazio (checado ANTES do cpfValido, para mensagem correta)
        if (documento.isEmpty()) {

            binding.edtDocumento.error =
                if (isClube()) "Preencha o CNPJ" else "Preencha o CPF"

            binding.edtDocumento.requestFocus()

            return false
        }

        // CNPJ: validação só por tamanho (Clube/Olheiro)
        if (isClube() && documento.length != 14) {

            binding.edtDocumento.error = "CNPJ inválido"
            binding.edtDocumento.requestFocus()

            return false
        }

        // CPF: validação por dígito verificador (substitui a checagem antiga de length != 11)
        if (!isClube() && !cpfValido(documento)) {

            binding.edtDocumento.error = "CPF inválido"
            binding.edtDocumento.requestFocus()

            return false
        }

        // Posição (somente Atleta)
        if (isAtleta() && binding.spinnerPosicao.selectedItemPosition == 0) {

            val textErro =
                binding.spinnerPosicao.selectedView as? TextView

            textErro?.error = "Selecione uma posição"

            Toast.makeText(
                requireContext(),
                "Selecione uma posição",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        // Cidade
        if (cidade.isEmpty()) {

            binding.edtCidade.error =
                "Preencha o campo de cidade"

            binding.edtCidade.requestFocus()

            return false
        }

        return true
    }

    // =========================================================
    // VALIDAÇÃO PASSO 3
    // =========================================================

    private fun validarPasso3(): Boolean {

        val senha =
            binding.edtSenha.text
                .toString()
                .trim()

        val confirmaSenha =
            binding.edtConfirmarSenha.text
                .toString()
                .trim()

        // Senha vazia
        if (senha.isEmpty()) {

            binding.edtSenha.error =
                "Preencha o campo de senha"

            binding.edtSenha.requestFocus()

            return false
        }

        // Confirmação vazia
        if (confirmaSenha.isEmpty()) {

            binding.edtConfirmarSenha.error =
                "Confirme sua senha"

            binding.edtConfirmarSenha.requestFocus()

            return false
        }

        // Senhas diferentes
        if (senha != confirmaSenha) {

            binding.edtConfirmarSenha.error =
                "As senhas não coincidem"

            binding.edtConfirmarSenha.requestFocus()

            return false
        }

        return true
    }

    // =========================================================
    // CRIAÇÃO DO USUÁRIO
    // =========================================================

    private fun criarConta() {

        val nome =
            binding.edtNome.text
                .toString()
                .trim()

        val email =
            binding.edtEmail.text
                .toString()
                .trim()

        val senha =
            binding.edtSenha.text
                .toString()
                .trim()

        val dataNascimento =
            binding.edtDataNascimento.text
                .toString()

        val cidade =
            binding.edtCidade.text
                .toString()
                .trim()

        val estado =
            binding.spinnerEstado.selectedItem
                .toString()

        val tipoUsuario =
            binding.spinnerTipoUsuario.selectedItem
                .toString()

        val documento =
            binding.edtDocumento.text
                .toString()
                .trim()
                .replace(Regex("[^0-9]"), "")

        val posicao =
            if (isAtleta())
                binding.spinnerPosicao.selectedItem.toString()
            else
                ""

        val dataCadastro = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())


        // ==========================================
        // CRIA A CONTA NO FIREBASE AUTH
        // ==========================================

        auth.createUserWithEmailAndPassword(
            email,
            senha
        )
            .addOnSuccessListener { result ->

                val uid = result.user?.uid

                if (uid == null) {

                    Toast.makeText(
                        requireContext(),
                        "Erro ao obter ID do usuário",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }


                // ==========================================
                // CRIA O PERFIL DO USUÁRIO
                // ==========================================

                val usuario = Usuario(
                    nome = nome,
                    email = email,
                    dataNascimento = dataNascimento,
                    estado = estado,
                    cidade = cidade,
                    tipoUsuario = tipoUsuario,
                    dataCadastro = dataCadastro,
                    documento = documento,
                    posicao = posicao
                )


                // ==========================================
                // SALVA NO FIRESTORE
                // ==========================================

                db.collection("usuarios")
                    .document(uid)
                    .set(usuario)
                    .addOnSuccessListener {

                        Toast.makeText(
                            requireContext(),
                            "Cadastro realizado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController().navigate(
                            R.id.action_cadastroFragment_to_loginFragment
                        )
                    }
                    .addOnFailureListener { erro ->

                        Toast.makeText(
                            requireContext(),
                            "Erro ao salvar usuário: ${erro.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    requireContext(),
                    "Erro ao criar conta: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }



    // =========================================================
    // SPINNERS
    // =========================================================

    private fun configurarSpinners() {

        // -------------------------
        // ESTADOS
        // -------------------------

        val estadosArray =
            resources.getStringArray(
                R.array.estados_brasil
            )

        val listaComHint =
            mutableListOf("Selecione um estado")

        listaComHint.addAll(estadosArray)

        val adapterEstado =
            object : ArrayAdapter<String>(
                requireContext(),
                R.layout.item_spinner_selecionado,
                listaComHint
            ) {

                override fun isEnabled(
                    position: Int
                ): Boolean {
                    return position != 0
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {

                    val view =
                        super.getDropDownView(
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

        binding.spinnerEstado.adapter =
            adapterEstado

        // -------------------------
        // TIPO DE USUÁRIO
        // -------------------------

        val usuarioArray =
            resources.getStringArray(
                R.array.tipo_de_usuario
            )

        val listaComHint2 =
            mutableListOf(
                "Selecione um Tipo de Usuário"
            )

        listaComHint2.addAll(usuarioArray)

        val adapterUsuario =
            object : ArrayAdapter<String>(
                requireContext(),
                R.layout.item_spinner_selecionado,
                listaComHint2
            ) {

                override fun isEnabled(
                    position: Int
                ): Boolean {
                    return position != 0
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {

                    val view =
                        super.getDropDownView(
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

        binding.spinnerTipoUsuario.adapter =
            adapterUsuario

        // Detecta mudança do tipo de usuário
        binding.spinnerTipoUsuario.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    val tipoSelecionado =
                        binding.spinnerTipoUsuario
                            .selectedItem
                            ?.toString()
                            ?: ""

                    attCamposporTipoUsuario(
                        tipoSelecionado
                    )
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    // Mantém os campos padrão
                }
            }

        // -------------------------
        // POSIÇÃO (somente Atleta)
        // -------------------------

        val posicaoArray =
            resources.getStringArray(
                R.array.posicoes
            )

        val listaComHint3 =
            mutableListOf("Selecione uma posição")

        listaComHint3.addAll(posicaoArray)

        val adapterPosicao =
            object : ArrayAdapter<String>(
                requireContext(),
                R.layout.item_spinner_selecionado,
                listaComHint3
            ) {

                override fun isEnabled(
                    position: Int
                ): Boolean {
                    return position != 0
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {

                    val view =
                        super.getDropDownView(
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

        adapterPosicao.setDropDownViewResource(
            R.layout.item_spinner_dropdown
        )

        binding.spinnerPosicao.adapter =
            adapterPosicao
    }

    // =========================================================
    // ALTERA CAMPOS DE ACORDO COM O TIPO
    // =========================================================

    private fun attCamposporTipoUsuario(
        tipo: String
    ) {

        val serClube =
            tipo.equals(
                "Clube/Olheiro",
                ignoreCase = true
            )

        val serAtleta =
            tipo.equals(
                "Atleta",
                ignoreCase = true
            )

        if (serClube) {

            binding.lblNome.text =
                "Nome do Clube"

            binding.edtNome.hint =
                "Ex: União FC"

            binding.lblDataNascimento.text =
                "Data de Fundação"

            binding.edtDataNascimento.hint =
                "00/00/0000"

            binding.lblDocumento.text = "CNPJ"
            binding.edtDocumento.hint = "00.000.000/0000-00"
            binding.edtDocumento.setText("")

        } else {

            binding.lblNome.text =
                "Nome"

            binding.edtNome.hint =
                "Nome Completo"

            binding.lblDataNascimento.text =
                "Data de Nascimento"

            binding.edtDataNascimento.hint =
                "00/00/0000"

            binding.lblDocumento.text = "CPF"
            binding.edtDocumento.hint = "000.000.000-00"
            binding.edtDocumento.setText("")
        }

        // Posição só aparece para Atleta
        binding.lblPosicao.visibility =
            if (serAtleta) View.VISIBLE else View.GONE

        binding.spinnerPosicao.visibility =
            if (serAtleta) View.VISIBLE else View.GONE

        if (!serAtleta) {
            binding.spinnerPosicao.setSelection(0)
        }

        // Limpa a data ao trocar de tipo, já que a regra de idade muda
        binding.edtDataNascimento.setText("")
    }

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CadastroFragment : Fragment() {

    private var _binding: FragmentCadastroBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
        configurarDataNascimento()
        configurarBotoes()

        mostrarPasso(1)

        return binding.root
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

            DatePickerDialog(
                requireContext(),
                { _, anoSelecionado, mesSelecionado, diaSelecionado ->

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
            ).show()
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

    // =========================================================
    // VALIDAÇÃO PASSO 2
    // =========================================================

    private fun validarPasso2(): Boolean {

        val dataNascimento =
            binding.edtDataNascimento.text
                .toString()
                .trim()

        val cidade =
            binding.edtCidade.text
                .toString()
                .trim()

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
                    senha = "",
                    dataNascimento = dataNascimento,
                    estado = estado,
                    cidade = cidade,
                    tipoUsuario = tipoUsuario,
                    dataCadastro = dataCadastro
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

        if (serClube) {

            binding.lblNome.text =
                "Nome do Clube"

            binding.edtNome.hint =
                "Ex: União FC"

            binding.lblDataNascimento.text =
                "Data de Fundação"

            binding.edtDataNascimento.hint =
                "00/00/0000"

        } else {

            binding.lblNome.text =
                "Nome"

            binding.edtNome.hint =
                "Nome Completo"

            binding.lblDataNascimento.text =
                "Data de Nascimento"

            binding.edtDataNascimento.hint =
                "00/00/0000"
        }
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
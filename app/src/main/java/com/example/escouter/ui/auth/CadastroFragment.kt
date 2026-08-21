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
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import java.util.Calendar
import android.content.Context
import com.example.escouter.model.Usuario
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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


        binding.edtDataNascimento.setOnClickListener {
            val calendario = Calendar.getInstance()

            val ano=calendario.get(Calendar.YEAR)
            val mes=calendario.get(Calendar.MONTH)
            val dia=calendario.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                requireContext(),{_,anoSelecionado,mesSelecionado,diaSelecionado ->
                    val data = String.format(
                        "%02d/%02d/%04d",
                        diaSelecionado,
                        mesSelecionado+1,
                        anoSelecionado
                    )
                    binding.edtDataNascimento.setText(data)
                },
                ano,
                mes,
                dia
            ).show()
        }

        binding.btnCriarConta.setOnClickListener {
            val nome = binding.edtNome.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val senha = binding.edtSenha.text.toString().trim()
            val confirmsenha = binding.edtConfirmarSenha.text.toString()
            val dtnascimento = binding.edtDataNascimento.text.toString()
            val cidade = binding.edtCidade.text.toString().trim()

            //valida nome
            if (nome.isEmpty()){
                binding.edtNome.error = "Preencha o campo de nome"
                return@setOnClickListener
            }

            //valida email e confere se está nos padroes

            val regexEmail = Regex("^[A-Za-z0-9._%+-]+@(gmail|hotmail|outlook|yahoo)\\.com$")

            if (email.isEmpty()){
                binding.edtEmail.error = "Preencha o campo de email"
                return@setOnClickListener
            }

            if (!regexEmail.matches(email)){
                binding.edtEmail.error = "Use um email válido"
                return@setOnClickListener
            }

            //verifica se os spinners estao selecionado
            if (binding.spinnerTipoUsuario.selectedItemPosition == 0){
                val textErro = binding.spinnerTipoUsuario.selectedView as? TextView
                textErro?.error = "Selecione um tipo de usuário"
                Toast.makeText(requireContext(), "Selecione um tipo de Usuário", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }

            if (binding.spinnerEstado.selectedItemPosition == 0){
                val textErro = binding.spinnerEstado.selectedView as? TextView
                textErro?.error = "Selecione um estado"
                Toast.makeText(requireContext(), "Selecione um estado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }

            //verifica data de nascimento
            if (dtnascimento.isEmpty()){
                binding.edtDataNascimento.error = "Preencha o campo de data de nascimento"
                return@setOnClickListener
            }

            //verifica a cidade
            if (cidade.isEmpty()){
                binding.edtCidade.error = "Preencha o campo de cidade"
                return@setOnClickListener
            }

            // verifica senha
            //confere se o campo de senha esta igual ao confirmar senha
            if (senha.isEmpty() || confirmsenha.isEmpty()){
                binding.edtConfirmarSenha.error = "Preencha o campo de senha"
                return@setOnClickListener
            }
            if(senha != confirmsenha){
                binding.edtConfirmarSenha.error = "As senhas não coincidem"
                return@setOnClickListener
            }

            val dataCadastro = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(Date())

            // Cria o objeto Usuario
            val usuario = Usuario(
                nome = nome,
                email = email,
                senha = senha,
                dataNascimento = dtnascimento,
                estado = binding.spinnerEstado.selectedItem.toString(),
                cidade = cidade,
                tipoUsuario = binding.spinnerTipoUsuario.selectedItem.toString(),
                dataCadastro = dataCadastro
            )

            // Converte o Usuario para JSON
            val json = Gson().toJson(usuario)

            // Abre o SharedPreferences
            val preferences = requireContext().getSharedPreferences(
                "eScouter",
                Context.MODE_PRIVATE
            )

            // Salva o JSON
            preferences.edit()
                .putString("usuario", json)
                .apply()

            Toast.makeText(
                requireContext(),
                "Cadastro realizado com sucesso!",
                Toast.LENGTH_SHORT
            ).show()

            findNavController().navigate(R.id.action_cadastroFragment_to_loginFragment)

        }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }
    private fun initListener() {
        binding.tvJaPossuiConta.setOnClickListener {
            findNavController().navigate(R.id.action_cadastroFragment_to_loginFragment)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
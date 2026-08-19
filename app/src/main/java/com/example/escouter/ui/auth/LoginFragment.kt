package com.example.escouter.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.escouter.R
import com.example.escouter.databinding.FragmentLoginBinding
import androidx.navigation.fragment.findNavController
import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import com.example.escouter.model.Usuario

class LoginFragment : Fragment() {

    private var _binding : FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater,container,false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }
    private fun initListener(){

        binding.btnEntrar.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val senha = binding.etSenha.text.toString().trim()

            when {
                email.isEmpty() -> {
                    binding.etEmail.error = "Informe seu email"
                    binding.etEmail.requestFocus()
                }

                senha.isEmpty() -> {
                    binding.etSenha.error = "Informe sua senha"
                    binding.etSenha.requestFocus()
                }

                else -> {
                    val preferences = requireContext().getSharedPreferences(
                        "eScouter",
                        Context.MODE_PRIVATE
                    )

                    val json = preferences.getString("usuario", null)

                    if (json == null) {

                        Toast.makeText(
                            requireContext(),
                            "Nenhum usuário cadastrado.",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        val usuario = Gson().fromJson(json, Usuario::class.java)

                        if (email == usuario.email && senha == usuario.senha) {

                            Toast.makeText(
                                requireContext(),
                                "Login realizado com sucesso!",
                                Toast.LENGTH_SHORT
                            ).show()

                            findNavController().navigate(
                                R.id.action_loginFragment_to_home
                            )

                        } else {

                            Toast.makeText(
                                requireContext(),
                                "E-mail ou senha incorretos.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        binding.tvCriarConta.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_cadastroFragment)
        }
        binding.tvEsqueceuSenha.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_redefinirSenhaFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }

}
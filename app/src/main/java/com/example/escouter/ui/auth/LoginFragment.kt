package com.example.escouter.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.escouter.R
import com.example.escouter.databinding.FragmentLoginBinding
import androidx.navigation.fragment.findNavController

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
                    findNavController().navigate(
                        R.id.action_loginFragment_to_home
                    )
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
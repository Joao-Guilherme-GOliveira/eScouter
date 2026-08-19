package com.example.escouter.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.escouter.R
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.escouter.databinding.FragmentRedefinirSenhaBinding

class RedefinirSenhaFragment : Fragment() {

    private var _binding: FragmentRedefinirSenhaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRedefinirSenhaBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Botão Enviar
        binding.btnEnviar.setOnClickListener {

            val email = binding.edtEmail.text.toString().trim()

            if (email.isEmpty()) {

                binding.inputEmail.error = "Digite seu e-mail"

            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                binding.inputEmail.error = "Digite um e-mail válido"

            } else {

                binding.inputEmail.error = null

                Toast.makeText(
                    requireContext(),
                    "Código enviado para seu e-mail!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Botão/Texto Voltar
        binding.txtVoltar.setOnClickListener {
            findNavController().navigate(R.id.action_redefinirSenhaFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
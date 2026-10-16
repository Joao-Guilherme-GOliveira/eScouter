package com.example.escouter.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.escouter.R
import com.example.escouter.databinding.FragmentBuscaTodosBinding
import com.google.firebase.firestore.FirebaseFirestore

class NotificacoesFragment : Fragment() {

    private var _binding: FragmentBuscaTodosBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_notificacoes, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
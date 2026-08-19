package com.example.escouter.model

data class Usuario(
    val nome: String,
    val email: String,
    val senha: String,
    val dataNascimento: String,
    val estado: String,
    val cidade: String,
    val tipoUsuario: String
)

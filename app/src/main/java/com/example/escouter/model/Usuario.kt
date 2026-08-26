package com.example.escouter.model

data class Usuario(
    val nome: String,
    val email: String,
    val senha: String,
    val dataNascimento: String,
    val estado: String,
    val cidade: String,
    val tipoUsuario: String,

    val posicao: String = "",
    val peso: String = "",
    val altura: String = "",
    val experiencia: String = "",
    val descricao: String = "",
    val dataCadastro: String = "",

    val cnpj: String = "",
    val telefone: String = "",

    val midias: List<Midia> = emptyList()
)

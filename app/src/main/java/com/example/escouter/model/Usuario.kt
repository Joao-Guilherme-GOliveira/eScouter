package com.example.escouter.model

data class Usuario(
    val nome: String = "",
    val email: String = "",
    val dataNascimento: String = "",
    val estado: String = "",
    val cidade: String = "",
    val tipoUsuario: String = "",

    // Dados do atleta
    val posicao: String = "",
    val peso: String = "",
    val altura: String = "",
    val experiencia: String = "",
    val descricao: String = "",

    // Dados da conta
    val dataCadastro: String = "",

    // Dados do clube
    val cnpj: String = "",
    val telefone: String = "",

    // Mídias
    val midias: List<Midia> = emptyList()
)

package com.example.escouter.model

data class Partida(
    val timeCasa: String = "",
    val timeVisitante: String = "",
    val data: String = "",
    val hora: String = "",
    val local: String = "",
    val dataHoraMillis: Long = 0L, // usado para ordenar e filtrar "próximas"
    val emailClube: String = "",
    val clubeUid: String = ""
)
package com.example.escouter.data

import retrofit2.http.GET
import retrofit2.http.Path

interface Ibgeservice {
    @GET("estados?orderBy=nome")
    suspend fun getEstados(): List<EstadoIbge>
    @GET("estados/{uf}/municipios")
    suspend fun getCidades(@Path("uf") uf: String): List<CidadeIbge>
}
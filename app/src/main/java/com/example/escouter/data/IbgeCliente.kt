package com.example.escouter.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object IbgeCliente {
    val service: Ibgeservice by lazy {
        Retrofit.Builder()
            .baseUrl("https://servicodados.ibge.gov.br/api/v1/localidades")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Ibgeservice::class.java)
    }
}
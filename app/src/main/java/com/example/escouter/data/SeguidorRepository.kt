package com.example.escouter.data

import com.example.escouter.model.Seguidor
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore

object SeguidorRepository{
    private val db = FirebaseFirestore.getInstance()
    private val colecao get() = db.collection("seguidores")

    private fun idRelation(seguidorId: String, seguidoId: String) =
        "\${seguidorId}_\${seguidoId}"

    fun seguir(
        seguidorId: String,
        seguidoId: String,
        onSucesso: () -> Unit,
        onErro: (Exception) -> Unit){
        if (seguidorId == seguidoId){
            onErro(IllegalArgumentException("Você não pode seguir a si mesmo."))
            return
        }
        val relacao = Seguidor(seguidorId, seguidoId, System.currentTimeMillis())

        colecao.document(idRelation(seguidorId,seguidoId))
            .set(relacao)
            .addOnSuccessListener { onSucesso() }
            .addOnFailureListener { onErro(it) }
    }
    fun deixaSeguir(
        seguidorId: String,
        seguidoId: String,
        onResultado: (Boolean) -> Unit){
        colecao.document(idRelation(seguidorId,seguidoId))
            .get()
            .addOnSuccessListener { onResultado(it.exists()) }
            .addOnFailureListener { onResultado(false) }
    }

    fun qtdSeguidores(uid: String,onResultado: (Long) -> Unit){
        colecao.whereEqualTo("seguidoId",uid)
            .count()
            .get(AggregateSource.SERVER)
            .addOnSuccessListener { onResultado(it.count) }
            .addOnFailureListener { onResultado(0) }
    }
    fun qtdSeguindo(uid: String, onResultado: (Long) -> Unit) {
        colecao.whereEqualTo("seguidorId", uid)
            .count()
            .get(AggregateSource.SERVER)
            .addOnSuccessListener { onResultado(it.count) }
            .addOnFailureListener { onResultado(0) }
    }
}
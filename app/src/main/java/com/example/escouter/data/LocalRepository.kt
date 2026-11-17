package com.example.escouter.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import java.util.Locale

/**
 * Locais de partida padronizados.
 *
 * Cada local é um documento em "locais" cujo ID é a "chave" do nome
 * (sem acento, minúsculo, sem símbolos). Assim "IFES SERRA", "Ifes SerRa"
 * e "ifes  serra" geram o MESMO ID e nunca viram registros diferentes.
 */
object LocalRepository {

    private val ptBR = Locale("pt", "BR")
    private val db get() = FirebaseFirestore.getInstance()
    private val colecao get() = db.collection("locais")

    private val palavrasMinusculas = setOf("de", "da", "do", "das", "dos", "e")

    /** "  Ifes   SERRA " -> "ifes_serra" (usada como ID do documento). */
    fun chave(texto: String): String {
        val semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return semAcento
            .lowercase(ptBR)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    /**
     * Arruma espaços e, se o texto veio todo em MAIÚSCULAS ou todo em
     * minúsculas, converte para "Primeira Letra Maiúscula".
     * Textos com caixa mista (ex.: "EMEF Centro") são mantidos.
     */
    fun formatarNome(texto: String): String {
        val limpo = texto.trim().replace(Regex("\\s+"), " ")
        val letras = limpo.filter { it.isLetter() }

        val caixaUniforme = letras.isNotEmpty() &&
                (letras == letras.uppercase(ptBR) || letras == letras.lowercase(ptBR))

        if (!caixaUniforme) return limpo

        return limpo.lowercase(ptBR)
            .split(" ")
            .mapIndexed { i, palavra ->
                if (i > 0 && palavra in palavrasMinusculas) palavra
                else palavra.replaceFirstChar { it.titlecase(ptBR) }
            }
            .joinToString(" ")
    }

    /** Lista os nomes dos locais já registrados, em ordem alfabética. */
    fun listar(
        onSucesso: (List<String>) -> Unit,
        onErro: (Exception) -> Unit
    ) {
        colecao.orderBy("chave")
            .get()
            .addOnSuccessListener { resultado ->
                onSucesso(resultado.documents.mapNotNull { it.getString("nome") })
            }
            .addOnFailureListener { onErro(it) }
    }

    /**
     * Registra o local se ainda não existir. Se já existir (mesma chave),
     * devolve o nome JÁ cadastrado, que é o nome oficial.
     */
    fun registrar(
        nomeDigitado: String,
        onSucesso: (String) -> Unit,
        onErro: (Exception) -> Unit
    ) {
        val nome = formatarNome(nomeDigitado)
        val chave = chave(nome)

        if (chave.isEmpty()) {
            onErro(IllegalArgumentException("Informe um nome de local válido."))
            return
        }

        val referencia = colecao.document(chave)

        db.runTransaction { transacao ->
            val existente = transacao.get(referencia)

            if (existente.exists()) {
                existente.getString("nome") ?: nome
            } else {
                transacao.set(
                    referencia,
                    mapOf(
                        "nome" to nome,
                        "chave" to chave,
                        "criadoPor" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
                        "criadoEm" to System.currentTimeMillis()
                    )
                )
                nome
            }
        }
            .addOnSuccessListener { onSucesso(it) }
            .addOnFailureListener { onErro(it) }
    }
}
package com.example.escouter.data

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.io.File

object FotoPerfilUploader {

    fun enviar(
        context: Context,
        uri: Uri,
        onSucesso: (String) -> Unit,
        onErro: (String) -> Unit
    ) {
        val appContext = context.applicationContext
        val principal = Handler(Looper.getMainLooper())

        Thread {
            val arquivo = copiarParaCache(appContext, uri)

            principal.post {
                if (arquivo == null) {
                    onErro("Não foi possível ver a imagem.")
                    return@post
                }

                MediaManager.get()
                    .upload(arquivo.absolutePath)
                    .unsigned("escouter_midias")
                    .option("resource_type", "image")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {}
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                        override fun onReschedule(requestId: String, error: ErrorInfo) {}

                        override fun onSuccess(
                            requestId: String,
                            resultData: MutableMap<Any?, Any?>
                        ) {
                            arquivo.delete()
                            val url = resultData["secure_url"]?.toString()
                            if (url.isNullOrEmpty()) onErro("Não foi possível obter a URL da foto.")
                            else onSucesso(url)
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            arquivo.delete()
                            onErro("Erro no upload: ${error.description}")
                        }
                    })
                    .dispatch()
            }
        }.start()
    }

    private fun copiarParaCache(contexto: Context, uri: Uri): File? {
        return try {
            val arquivo = File.createTempFile("foto_", ".jpg", contexto.cacheDir)
            val copiou = contexto.contentResolver.openInputStream(uri)?.use { entrada ->
                arquivo.outputStream().use { saida -> entrada.copyTo(saida) }
                true
            } ?: false

            if (!copiou || arquivo.length() == 0L) {
                arquivo.delete()
                null
            } else arquivo
        } catch (e: Exception) {
            null
        }
    }
}
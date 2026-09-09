package com.example.escouter.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.escouter.ImageViewerActivity
import com.example.escouter.R
import com.example.escouter.VideoPlayerActivity
import com.example.escouter.model.Midia

class MediaAdapter(
    private var midias: List<Midia>
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    class MediaViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imgThumbnail: ImageView =
            itemView.findViewById(R.id.imgThumbnail)

        val txtDuration: TextView =
            itemView.findViewById(R.id.txtDuration)

        val txtMediaTitle: TextView =
            itemView.findViewById(R.id.txtMediaTitle)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MediaViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_media,
                parent,
                false
            )

        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MediaViewHolder,
        position: Int
    ) {

        val midia = midias[position]

        // Nome da mídia
        holder.txtMediaTitle.text = midia.nome

        // Duração
        if (midia.tipo == "video" && midia.duracao.isNotEmpty()) {
            holder.txtDuration.visibility = View.VISIBLE
            holder.txtDuration.text = midia.duracao
        } else {
            holder.txtDuration.visibility = View.GONE
        }

        // Carrega a imagem da mídia / capa do vídeo
        if (midia.thumbnailUri.isNotEmpty()) {

            holder.imgThumbnail.load(midia.thumbnailUri) {
                crossfade(true)
            }

        } else {

            // Para mídias antigas que ainda não possuem thumbnail
            holder.imgThumbnail.setImageResource(
                R.drawable.ic_video
            )
        }

        // Abrir mídia
        holder.itemView.setOnClickListener {

            if (midia.uri.isEmpty()) {
                return@setOnClickListener
            }

            if (midia.tipo == "video") {

                // Vídeo → abre no player do próprio app
                val intent = Intent(
                    holder.itemView.context,
                    VideoPlayerActivity::class.java
                )

                intent.putExtra(
                    "video_url",
                    midia.uri
                )

                holder.itemView.context.startActivity(intent)

            } else if (midia.tipo == "imagem") {

                // Imagem → abre no visualizador do próprio app
                val intent = Intent(
                    holder.itemView.context,
                    ImageViewerActivity::class.java
                )

                intent.putExtra(
                    "image_url",
                    midia.uri
                )

                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return midias.size
    }

    fun atualizarMidias(novasMidias: List<Midia>) {
        midias = novasMidias
        notifyDataSetChanged()
    }
}
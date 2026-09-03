package com.example.escouter.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.escouter.R
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
        holder.txtDuration.text = midia.duracao

        // Imagem padrão enquanto ainda não temos thumbnail
        holder.imgThumbnail.setImageResource(
            R.drawable.ic_video
        )

        // Abrir mídia do Cloudinary ao clicar
        holder.itemView.setOnClickListener {

            if (midia.uri.isNotEmpty()) {

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(midia.uri)
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
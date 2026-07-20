package com.primaloptima.scribe.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.primaloptima.scribe.R
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.util.MarkdownUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shows notes as cards with title, preview, and metadata. Used in BookActivity list mode. */
class FileCardAdapter(
    private var items: List<Note> = emptyList(),
    private val onClick: (Note) -> Unit,
    private val onLongClick: (Note) -> Unit,
) : RecyclerView.Adapter<FileCardAdapter.VH>() {

    private val dateFmt = SimpleDateFormat("MMM d", Locale.getDefault())

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView    = v.findViewById(R.id.tv_file_name)
        val preview: TextView = v.findViewById(R.id.tv_file_preview)
        val meta: TextView    = v.findViewById(R.id.tv_file_meta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_file_card, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val note = items[position]
        holder.name.text = note.name

        val stripped = MarkdownUtil.toPlainText(note.content)
        holder.preview.text = if (stripped.isBlank()) "" else stripped.take(200)
        holder.preview.visibility = if (stripped.isBlank()) View.GONE else View.VISIBLE

        val words = MarkdownUtil.countWords(note.content)
        val date  = dateFmt.format(Date(note.updatedAt))
        holder.meta.text = if (words > 0) "$words words · $date" else date

        holder.itemView.setOnClickListener { onClick(note) }
        holder.itemView.setOnLongClickListener { onLongClick(note); true }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<Note>) {
        items = newItems
        notifyDataSetChanged()
    }
}

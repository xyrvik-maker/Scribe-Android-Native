package com.primaloptima.scribe.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.primaloptima.scribe.R
import com.primaloptima.scribe.data.Folder
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.util.MarkdownUtil
import com.primaloptima.scribe.viewmodel.BookViewModel

/**
 * Adapter for the file tree inside the editor's left panel.
 * Shows a flat ordered tree: folders (expandable) → their notes, recursively.
 */
class FileTreeAdapter(
    private var items: List<BookViewModel.TreeItem> = emptyList(),
    private val onNoteClick: (Note) -> Unit,
    private val onFolderClick: (Folder) -> Unit,
    private val onNoteLongClick: ((Note) -> Unit)? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_NOTE = 1
        private const val INDENT_DP = 16
    }

    inner class FolderVH(v: View) : RecyclerView.ViewHolder(v) {
        val indent: View = v.findViewById(R.id.view_indent)
        val icon: ImageView = v.findViewById(R.id.img_tree_icon)
        val name: TextView = v.findViewById(R.id.tv_tree_name)
    }

    inner class NoteVH(v: View) : RecyclerView.ViewHolder(v) {
        val indent: View = v.findViewById(R.id.view_indent)
        val icon: ImageView = v.findViewById(R.id.img_tree_icon)
        val name: TextView = v.findViewById(R.id.tv_tree_name)
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is BookViewModel.TreeItem.FolderItem -> TYPE_FOLDER
        is BookViewModel.TreeItem.NoteItem   -> TYPE_NOTE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_tree, parent, false)
        return if (viewType == TYPE_FOLDER) FolderVH(v) else NoteVH(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val density = holder.itemView.context.resources.displayMetrics.density
        when (val item = items[position]) {
            is BookViewModel.TreeItem.FolderItem -> {
                val h = holder as FolderVH
                val params = h.indent.layoutParams
                params.width = (item.depth * INDENT_DP * density).toInt()
                h.indent.layoutParams = params
                h.icon.setImageResource(
                    if (item.expanded) R.drawable.ic_arrow_down else R.drawable.ic_arrow_right
                )
                val folderName = item.folder.path.substringAfterLast('/', item.folder.path)
                h.name.text = if (folderName.isEmpty() || folderName == "/") "Root" else folderName
                h.itemView.setOnClickListener { onFolderClick(item.folder) }
            }
            is BookViewModel.TreeItem.NoteItem -> {
                val h = holder as NoteVH
                val params = h.indent.layoutParams
                params.width = (item.depth * INDENT_DP * density).toInt()
                h.indent.layoutParams = params
                h.icon.setImageResource(R.drawable.ic_edit)
                h.name.text = item.note.name
                h.itemView.setOnClickListener { onNoteClick(item.note) }
                if (onNoteLongClick != null) {
                    h.itemView.setOnLongClickListener {
                        onNoteLongClick.invoke(item.note)
                        true
                    }
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<BookViewModel.TreeItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

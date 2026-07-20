package com.primaloptima.scribe.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.primaloptima.scribe.R
import com.primaloptima.scribe.data.Book
import com.primaloptima.scribe.util.BookCoverUtil

class BookAdapter(
    private var items: List<Book> = emptyList(),
    private var isGridMode: Boolean = true,
    private val onClick: (Book) -> Unit,
    private val onLongClick: (Book, View) -> Unit,
) : RecyclerView.Adapter<BookAdapter.BookVH>() {

    companion object {
        private const val VIEW_GRID = 0
        private const val VIEW_LIST = 1
    }

    inner class BookVH(val root: View) : RecyclerView.ViewHolder(root) {
        val cover: ImageView = root.findViewById(R.id.img_book_cover)
        val title: TextView = root.findViewById(R.id.tv_book_title)
        val subtitle: TextView? = root.findViewById(R.id.tv_book_subtitle)
        // Grid-only: initial letter overlay and scrim
        val initial: TextView? = root.findViewById(R.id.tv_book_initial)
        val scrim: View? = root.findViewById(R.id.cover_scrim)
    }

    override fun getItemViewType(position: Int): Int =
        if (isGridMode) VIEW_GRID else VIEW_LIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookVH {
        val layout = if (viewType == VIEW_GRID) R.layout.item_book_grid else R.layout.item_book_list
        val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return BookVH(v)
    }

    override fun onBindViewHolder(holder: BookVH, position: Int) {
        val book = items[position]
        holder.title.text = book.title
        holder.subtitle?.text = ""

        if (book.coverUri != null) {
            // User has set a photo cover — load it, hide initial
            holder.cover.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.cover.load(Uri.parse(book.coverUri)) {
                crossfade(true)
                placeholder(R.drawable.ic_folder)
            }
            holder.initial?.visibility = View.GONE
            holder.scrim?.visibility = View.GONE
        } else {
            // No photo: render a gradient + big initial letter
            val gradient = BookCoverUtil.makeGradient(book.title)
            holder.cover.scaleType = ImageView.ScaleType.FIT_XY
            holder.cover.setImageDrawable(gradient)
            holder.initial?.let { tv ->
                tv.text = BookCoverUtil.initial(book.title)
                tv.visibility = View.VISIBLE
            }
            holder.scrim?.visibility = View.GONE
        }

        holder.root.setOnClickListener { onClick(book) }
        holder.root.setOnLongClickListener { onLongClick(book, holder.root); true }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<Book>, gridMode: Boolean) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(o: Int, n: Int) = items[o].id == newItems[n].id
            override fun areContentsTheSame(o: Int, n: Int) = items[o] == newItems[n]
        })
        val modeChanged = isGridMode != gridMode
        items = newItems
        isGridMode = gridMode
        if (modeChanged) notifyDataSetChanged() else diff.dispatchUpdatesTo(this)
    }
}

package tk.zwander.oneuituner.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import kotlinx.android.synthetic.main.overlay_file_item.view.*
import tk.zwander.oneuituner.R
import java.io.File

class OverlayFileManagerAdapter : RecyclerView.Adapter<OverlayFileManagerAdapter.VH>() {
    private val list = SortedList(
        File::class.java,
        object : SortedList.Callback<File>() {
            override fun areItemsTheSame(item1: File?, item2: File?) = item1 == item2

            override fun compare(o1: File, o2: File) = o1.name.compareTo(o2.name)

            override fun areContentsTheSame(oldItem: File, newItem: File) = oldItem.name == newItem.name

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyItemMoved(fromPosition, toPosition)
            }

            override fun onChanged(position: Int, count: Int) {
                notifyItemRangeChanged(position, count)
            }

            override fun onInserted(position: Int, count: Int) {
                notifyItemRangeInserted(position, count)
            }

            override fun onRemoved(position: Int, count: Int) {
                notifyItemRangeRemoved(position, count)
            }
        }
    )

    override fun getItemCount() = list.size()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.overlay_file_item, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.setText(list[position].name)
    }

    fun addItem(item: File): Int {
        return list.add(item)
    }

    fun removeItem(item: File): Boolean {
        return list.remove(item)
    }

    fun removeItemAt(index: Int): File {
        return list.removeItemAt(index)
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        fun setText(text: CharSequence) {
            itemView.overlay_file_name.text = text
        }
    }
}
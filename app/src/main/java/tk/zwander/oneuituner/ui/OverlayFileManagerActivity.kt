package tk.zwander.oneuituner.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.samsungthemelib.util.SEARCH_PATH
import com.samsungthemelib.util.isAPK
import com.samsungthemelib.util.mainHandler
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_overlay_file_manager.*
import kotlinx.android.synthetic.main.activity_overlay_file_manager.bottom_bar
import kotlinx.android.synthetic.main.activity_overlay_file_manager.content
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tk.zwander.oneuituner.R
import tk.zwander.oneuituner.util.OverlayFileManagerAdapter
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption


class OverlayFileManagerActivity : AppCompatActivity() {
    companion object {
        const val PICKER_REQ = 1001
    }

    private val adapter by lazy { OverlayFileManagerAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overlay_file_manager)

        with(bottom_bar.background as MaterialShapeDrawable) {
            val color = ElevationOverlayProvider(this@OverlayFileManagerActivity)
                .compositeOverlayWithThemeSurfaceColorIfNeeded(elevation)

            window.navigationBarColor = color
        }

        overlay_file_list.adapter = adapter
        overlay_file_list.layoutManager = LinearLayoutManager(this)
        overlay_file_list.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        ItemTouchHelper(ItemTouchHelperImpl()).attachToRecyclerView(overlay_file_list)

        parseOverlays()

        add_overlay.setOnClickListener {
            val pickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            pickerIntent.type = "application/vnd.android.package-archive"
            startActivityForResult(pickerIntent, PICKER_REQ)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PICKER_REQ -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        val returnCursor = contentResolver.query(uri, null, null, null, null)
                        val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        returnCursor.moveToFirst()
                        val fileName = returnCursor.getString(nameIndex)
                        returnCursor.close()

                        val dst = File(SEARCH_PATH, fileName)

                        try {
                            contentResolver.openInputStream(uri).use { input ->
                                Files.copy(
                                    input,
                                    dst.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING
                                )
                            }

                            adapter.addItem(dst)
                        } catch (e: IOException) {}
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun parseOverlays() {
        GlobalScope.launch {
            val dir = SEARCH_PATH

            if (dir.exists()) {
                dir.listFiles(FileFilter { it.isAPK }).forEach {
                    mainHandler.post {
                        adapter.addItem(it)
                    }
                }
            }
        }
    }

    inner class ItemTouchHelperImpl : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
        private val icon = ContextCompat.getDrawable(this@OverlayFileManagerActivity, R.drawable.ic_delete_black_24dp)!!
        private val background = ColorDrawable(ContextCompat.getColor(this@OverlayFileManagerActivity, R.color.colorError))

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val item = adapter.removeItemAt(position)

            showUndoSnackbar(item)
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

            val itemView = viewHolder.itemView

            val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
            val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
            val iconBottom = iconTop + icon.intrinsicHeight


            when {
                dX > 0 -> { // Swiping to the right
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    background.setBounds(
                        itemView.left, itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                }
                dX < 0 -> { // Swiping to the left
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top, itemView.right, itemView.bottom
                    )
                }
                else -> // view is unSwiped
                    background.setBounds(0, 0, 0, 0)
            }
            background.draw(c)
            icon.draw(c)
        }

        private fun showUndoSnackbar(oldItem: File) {
            val snackbar = Snackbar.make(
                content, R.string.overlay_removed,
                Snackbar.LENGTH_LONG
            )
            snackbar.setAction(R.string.undo) { adapter.addItem(oldItem) }
            snackbar.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    oldItem.delete()
                }
            })
            snackbar.show()
        }
    }
}

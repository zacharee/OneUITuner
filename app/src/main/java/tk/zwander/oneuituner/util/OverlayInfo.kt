package tk.zwander.oneuituner.util

import tk.zwander.oneuituner.data.ResourceFileData

data class OverlayInfo(
    val targetPkg: String,
    val overlayPkg: String,
    val data: ArrayList<ResourceFileData>
)
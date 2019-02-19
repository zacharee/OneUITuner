package tk.zwander.oneuituner.util

data class OverlayInfo(
    val targetPkg: String,
    val overlayPkg: String,
    val data: ArrayList<ResourceFileData>
)
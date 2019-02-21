package tk.zwander.oneuituner.data

data class OverlayInfo(
    val targetPkg: String,
    val overlayPkg: String,
    val data: ArrayList<ResourceFileData>
)
package tk.zwander.oneuituner.data

data class LayoutData(
    val tag: String,
    val items: HashMap<String, String>,
    val children: ArrayList<LayoutData>? = null
)
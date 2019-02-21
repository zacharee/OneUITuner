package tk.zwander.oneuituner.data

data class ResourceData(
        val type: String,
        val name: String,
        val value: String,
        val otherData: String = ""
)
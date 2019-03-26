package tk.zwander.oneuituner.util

import android.content.Context
import android.content.res.AssetManager
import com.samsungthemelib.util.transferAndClose
import eu.chainfire.libsuperuser.Shell
import tk.zwander.oneuituner.data.ResourceData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

fun Context.getResourceXmlFromAsset(folder: String, file: String): String {
    return assets.open("$folder/$file")
        .use { stream ->
            StringBuilder()
                .apply {
                    stream.bufferedReader()
                        .forEachLine {
                            append("$it\n")
                        }
                }
                .toString()
        }
}

fun getManifest(base: File, packageName: String, overlayPkg: String): File {
    val builder = StringBuilder()
    builder.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n")
    builder.append(
        "<manifest " +
                "xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "package=\"$overlayPkg\"\n" +
                "android:versionCode=\"100\"\n" +
                "android:versionName=\"100\">\n"
    )
    builder.append("<uses-permission android:name=\"com.samsung.android.permission.SAMSUNG_OVERLAY_COMPONENT\" />\n")
    builder.append("<overlay android:priority=\"100\" android:targetPackage=\"$packageName\" android:category=\"samsung\" />\n")
    builder.append("</manifest>\n")

    val manifestFile = File(base, "AndroidManifest.xml")
    if (manifestFile.exists()) manifestFile.delete()

    OutputStreamWriter(manifestFile.outputStream()).use {
        it.write(builder.toString())
        it.write("\n")
    }

    return manifestFile
}

fun makeResourceXml(data: List<ResourceData>) =
    makeResourceXml(*data.toTypedArray())

fun makeResourceXml(vararg data: ResourceData): String {
    return StringBuilder()
        .append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        .append("<resources xmlns:android=\"http://schemas.android.com/apk/res/android\">\n")
        .apply {
            data.forEach {
                append("<item type=\"${it.type}\" ${it.otherData} name=\"${it.name}\">${it.value}</item>\n")
            }
        }
        .append("</resources>\n")
        .toString()
}

fun makeOverlayFile(base: File, suffix: String, type: OverlayType): File {
    return File(base, "${suffix}_$type.apk")
}

fun makeResDir(base: File): File {
    val dir = File(base, "res/")
    if (dir.exists()) dir.deleteRecursively()

    dir.mkdirs()
    dir.mkdir()

    return dir
}

fun Context.makeBaseDir(suffix: String): File {
    val dir = File(cacheDir, suffix)

    if (dir.exists()) dir.deleteRecursively()

    dir.mkdirs()
    dir.mkdir()

    Shell.SH.run("chmod 777 ${dir.absolutePath}")

    return dir
}

fun AssetManager.extractAsset(assetPath: String, devicePath: String): Boolean {
    try {
        val files = list(assetPath) ?: emptyArray()
        if (files.isEmpty()) {
            return handleExtractAsset(this, assetPath, devicePath)
        }
        val f = File(devicePath)
        if (!f.exists() && !f.mkdirs()) {
            throw RuntimeException("cannot create directory: $devicePath")
        }
        var res = true
        for (file in files) {
            val assetList = list("$assetPath/$file") ?: emptyArray()
            res = if (assetList.isEmpty()) {
                res and handleExtractAsset(this, "$assetPath/$file", "$devicePath/$file")
            } else {
                res and extractAsset("$assetPath/$file", "$devicePath/$file")
            }
        }
        return res
    } catch (e: IOException) {
        e.printStackTrace()
        return false
    }
}

private fun handleExtractAsset(
    am: AssetManager, assetPath: String, devicePath: String
): Boolean {
    val parent = File(devicePath).parentFile
    if (!parent.exists() && !parent.mkdirs()) {
        throw RuntimeException("cannot create directory: " + parent.absolutePath)
    }

    return try {
        transferAndClose(am.open(assetPath), FileOutputStream(File(devicePath)))
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
}
package tk.zwander.oneuituner.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import com.android.apksig.ApkSigner
import eu.chainfire.libsuperuser.Shell
import tk.zwander.oneuituner.data.OverlayInfo
import tk.zwander.oneuituner.data.ResourceData
import tk.zwander.oneuituner.data.ResourceFileData
import tk.zwander.oneuituner.data.ResourceImageData
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

fun Context.install(which: String, listener: ((apk: File) -> Unit)?) {
    val data = when (which) {
        Keys.clock -> {
            OverlayInfo(
                Keys.systemuiPkg,
                Keys.clockPkg,
                mutableListOf<ResourceFileData>().apply {
                    val format = prefs.clockFormat

                    if (format.isValidClockFormat && prefs.customClock) {
                        add(
                            ResourceFileData(
                                "qs_status_bar_clock.xml",
                                "layout",
                                getResourceXmlFromAsset(
                                    "clock/layout",
                                    "qs_status_bar_clock_custom.xml"
                                ).replace("h:mm a", format)
                            )
                        )
                    }
                }
            )
        }
        Keys.qs -> {
            OverlayInfo(
                Keys.systemuiPkg,
                Keys.qsPkg,
                arrayListOf(
                    ResourceFileData(
                        "integers.xml",
                        "values",
                        makeResourceXml(
                            ResourceData(
                                "integer",
                                "quick_qs_tile_num",
                                prefs.headerCountPortrait.toString()
                            )
                        )
                    ),
                    ResourceFileData(
                        "integers.xml",
                        "values-land",
                        makeResourceXml(
                            ResourceData(
                                "integer",
                                "quick_qs_tile_num",
                                prefs.headerCountLandscape.toString()
                            )
                        )
                    )
                )
            )
        }
        Keys.misc -> {
            OverlayInfo(
                Keys.androidPkg,
                Keys.miscPkg,
                arrayListOf(
                    ResourceFileData(
                        "config.xml",
                        "values",
                        makeResourceXml(
                            mutableListOf(
                                ResourceData(
                                    "dimen",
                                    "navigation_bar_height",
                                    "${prefs.navHeight}dp"
                                ),
                                ResourceData(
                                    "dimen",
                                    "navigation_bar_width",
                                    "${prefs.navHeight}dp"
                                )
                            ).apply {
                                if (prefs.oldRecents) {
                                    add(
                                        ResourceData(
                                            "string",
                                            "config_recentsComponentName",
                                            "com.android.systemui/.recents.RecentsActivity",
                                            "translatable=\"false\""
                                        )
                                    )
                                }
                            }
                        )
                    )
                )
            )
        }
        else -> return
    }

    doCompileAlignAndSign(
        data,
        listener
    )
}

fun Context.uninstall(which: String) {
    val pkg = when (which) {
        Keys.clock -> Keys.clockPkg
        Keys.qs -> Keys.qsPkg
        Keys.misc -> Keys.miscPkg
        else -> return
    }

    val uninstalIntent = Intent(Intent.ACTION_DELETE)
    uninstalIntent.data = Uri.parse("package:$pkg")
    startActivity(uninstalIntent)
}

fun Context.doCompileAlignAndSign(
    overlayInfo: OverlayInfo,
    listener: ((apk: File) -> Unit)? = null
) {
    val base = makeBaseDir(overlayInfo.overlayPkg)
    val manifest = getManifest(base, overlayInfo.targetPkg, overlayInfo.overlayPkg)
    val unsignedUnaligned = makeOverlayFile(base, overlayInfo.overlayPkg, OverlayType.UNSIGNED_UNALIGNED)
    val unsigned = makeOverlayFile(base, overlayInfo.overlayPkg, OverlayType.UNSIGNED)
    val signed = makeOverlayFile(base, overlayInfo.overlayPkg, OverlayType.SIGNED)
    val resDir = makeResDir(base)

    overlayInfo.data.forEach {
        val dir = File(resDir, it.fileDirectory)

        dir.mkdirs()
        dir.mkdir()

        val resFile = File(dir, it.filename)
        if (resFile.exists()) resFile.delete()

        if (it is ResourceImageData) {
            it.image?.let {
                FileOutputStream(resFile).use { stream ->
                    it.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
        } else {
            OutputStreamWriter(resFile.outputStream()).use { writer ->
                writer.write(it.contents)
                writer.write("\n")
            }
        }
    }

    compileOverlay(manifest, unsignedUnaligned, resDir, overlayInfo.targetPkg)
    alignOverlay(unsignedUnaligned, unsigned)
    signOverlay(unsigned, signed)

    Shell.run("sh", arrayOf("cp ${signed.absolutePath} ${signed.absolutePath}"), null, true)

    listener?.invoke(signed)
}

fun Context.compileOverlay(manifest: File, overlayFile: File, resFile: File, targetPackage: String) {
    if (overlayFile.exists()) {
        overlayFile.delete()
    }

    val aaptCmd = StringBuilder()
        .append(aapt)
        .append(" p")
        .append(" -M ")
        .append(manifest)
        .append(" -I ")
        .append("/system/framework/framework-res.apk")
        .apply {
            if (targetPackage != "android") {
                append(" -I ")
                append(packageManager.getApplicationInfo(targetPackage, 0).sourceDir)
            }
        }
        .append(" -S ")
        .append(resFile)
        .append(" -F ")
        .append(overlayFile)
        .toString()

    Shell.run("sh", arrayOf(aaptCmd), null, true)
    Shell.SH.run("chmod 777 ${overlayFile.absolutePath}")
}

fun Context.alignOverlay(overlayFile: File, alignedOverlayFile: File) {
    if (alignedOverlayFile.exists()) alignedOverlayFile.delete()

    val zipalignCmd = StringBuilder()
        .append(zipalign)
        .append(" 4 ")
        .append(overlayFile.absolutePath)
        .append(" ")
        .append(alignedOverlayFile.absolutePath)
        .toString()

    Shell.run("sh", arrayOf(zipalignCmd), null, true)

    Shell.SH.run("chmod 777 ${alignedOverlayFile.absolutePath}")
}

fun Context.signOverlay(overlayFile: File, signed: File) {
    Shell.SH.run("chmod 777 ${overlayFile.absolutePath}")

    val key = File(cacheDir, "/signing-key-new")
    val pass = "overlay".toCharArray()

    if (key.exists()) key.delete()

    val store = KeyStore.getInstance(KeyStore.getDefaultType())
    store.load(assets.open("signing-key-new"), pass)

    val privKey = store.getKey("key", pass) as PrivateKey
    val certs = ArrayList<X509Certificate>()

    certs.add(store.getCertificateChain("key")[0] as X509Certificate)

    val signConfig = ApkSigner.SignerConfig.Builder("overlay", privKey, certs).build()
    val signConfigs = ArrayList<ApkSigner.SignerConfig>()

    signConfigs.add(signConfig)

    val signer = ApkSigner.Builder(signConfigs)
    signer.setV1SigningEnabled(true)
        .setV2SigningEnabled(true)
        .setInputApk(overlayFile)
        .setOutputApk(signed)
        .setMinSdkVersion(Build.VERSION.SDK_INT)
        .build()
        .sign()

    Shell.SH.run("chmod 777 ${signed.absolutePath}")
}
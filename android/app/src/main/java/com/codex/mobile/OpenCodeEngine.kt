package com.codex.mobile

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Manages the OpenCode engine: download of the engine pack (glibc runtime +
 * opencode binary) from a GitHub release, extraction, config, and the proot
 * launch of `opencode serve` behind a local web UI.
 */
class OpenCodeEngine(private val context: Context) {

    companion object {
        private const val TAG = "OpenCodeEngine"
        const val PORT = 18944
        const val HTTP_URL = "http://127.0.0.1:$PORT/"
        private const val ENGINE_ZIP_URL =
            "https://github.com/rroot4546-a11y/zclaw-builds/releases/download/v1.2.0/engine.zip"
        private const val PREFS = "zclaw.settings"
        private const val KEY_API = "zclaw.opencode.apikey"
    }

    data class State(val running: Boolean, val message: String, val installed: Boolean)

    private var process: Process? = null

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val isInstalled: Boolean
        get() = File(engineRoot(""), "bin/opencode").exists()

    val isRunning: Boolean
        get() {
            val proc = process ?: return false
            return try {
                proc.exitValue()
                false
            } catch (_: IllegalThreadStateException) {
                true
            }
        }

    fun getSavedApiKey(): String = prefs.getString(KEY_API, "") ?: ""

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API, key.trim()).apply()
    }

    private fun engineRoot(sub: String): File {
        @Suppress("DiscouragedApi")
        val dir = File(context.filesDir, "opencode")
        if (sub.isEmpty()) return dir
        val target = File(dir, sub)
        target.mkdirs()
        return target
    }

    /**
     * Download the engine zip (single GitHub release asset) and extract it.
     * Progress is reported from the calling background thread.
     */
    fun install(onProgress: (Int) -> Unit) {
        val zipFile = engineRoot("engine.zip")
        val root = engineRoot("")

        if (!isInstalled) {
            if (!zipFile.exists() || zipFile.length() < 50L * 1024 * 1024) {
                if (!copyBundledAsset(zipFile)) {
                    Log.i(TAG, "Downloading engine pack from GitHub release…")
                    download(ENGINE_ZIP_URL, zipFile, onProgress)
                } else {
                    onProgress(100)
                }
            } else {
                onProgress(100)
            }

            Log.i(TAG, "Extracting engine pack…")
            extract(zipFile, root)
            zipFile.delete()

            val bin = File(root, "bin/opencode")
            if (!bin.exists()) throw RuntimeException("Engine binary missing after extraction")
            bin.setExecutable(true)
        }
        onProgress(100)
    }

    private fun copyBundledAsset(target: File): Boolean {
        return try {
            context.assets.open("opencode/engine.zip").use { input ->
                BufferedOutputStream(FileOutputStream(target)).use { out ->
                    input.copyTo(out)
                }
            }
            target.length() >= 50L * 1024 * 1024
        } catch (e: Exception) {
            Log.w(TAG, "No bundled engine asset: ${e.message}")
            target.delete()
            false
        }
    }

    private fun download(url: String, target: File, onProgress: (Int) -> Unit) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000
        conn.instanceFollowRedirects = true
        val total = conn.contentLengthLong
        conn.inputStream.use { input ->
            BufferedOutputStream(FileOutputStream(target)).use { out ->
                val buffer = ByteArray(128 * 1024)
                var read: Int
                var done = 0L
                var lastPct = -1
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    done += read
                    if (total > 0) {
                        val pct = ((done * 100) / total).toInt()
                        if (pct != lastPct) {
                            lastPct = pct
                            onProgress(pct)
                        }
                    }
                }
            }
        }
        if (target.length() < 50L * 1024 * 1024) {
            throw RuntimeException("Download incomplete: ${target.length()}")
        }
    }

    private fun extract(zipFile: File, root: File) {
        FileInputStream(zipFile).use { fis ->
            ZipInputStream(BufferedInputStream(fis)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.removePrefix("rootfs/")
                    if (name.isNotEmpty()) {
                        val outFile = File(root, name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out ->
                                zis.copyTo(out)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    /**
     * Ensure the config file inside the rootfs exists with the user's API key.
     */
    private fun ensureConfig(apiKey: String) {
        val config = engineRoot("root/.config/opencode/opencode.json")
        config.parentFile?.mkdirs()
        val content = """
            {
              "provider": {
                "openai": {
                  "options": {
                    "apiKey": "$apiKey",
                    "model": "gpt-5.2-codex",
                    "small_model": "gpt-5.2-codex-mini"
                  }
                }
              },
              "data": "${engineRoot("root/.local/share/opencode")}",
              "disableTelemetry": true
            }
        """.trimIndent()
        config.writeText(content)
    }

    /**
     * Start `opencode serve` inside a minimal glibc rootfs via proot.
     * Returns once the web UI responds on PORT.
     */
    fun start(apiKey: String, onReady: (Boolean) -> Unit) {
        try {
            if (isRunning) {
                onReady(true)
                return
            }
            ensureConfig(apiKey)
            launch()
            val ok = waitReady(timeoutMs = 60_000)
            onReady(ok)
        } catch (e: Exception) {
            Log.e(TAG, "Start failed: ${e.message}", e)
            onReady(false)
        }
    }

    private fun launch() {
        val paths = BootstrapInstaller.getPaths(context)
        val root = engineRoot("root")
        val proot = File(paths.prefixDir, "bin/proot")
        if (!proot.exists()) throw RuntimeException("proot not installed")

        val binds = mutableListOf(
            "/dev:/dev",
            "/proc:/proc",
            "/sys:/sys",
            "${paths.prefixDir}/etc/tls/certs:/etc/ssl/certs",
            "${paths.prefixDir}/etc/resolv.conf:/etc/resolv.conf",
        )
        // Replace the placeholder resolv.conf shipped in the pack with the live one.
        val cmd = mutableListOf(
            proot.absolutePath,
            "-0",
            "-r",
            root.absolutePath,
        ).also { c -> binds.forEach { b -> c.add("-b"); c.add(b) } }
        cmd.add("/bin/opencode")
        cmd.add("serve")
        cmd.add("--port")
        cmd.add(PORT.toString())
        cmd.add("--hostname")
        cmd.add("127.0.0.1")

        val pb = ProcessBuilder(cmd)
        pb.environment().clear()
        pb.environment()["HOME"] = "/root"
        pb.environment()["OPENAI_API_KEY"] = getSavedApiKey()
        pb.redirectErrorStream(true)
        @Suppress("DiscouragedApi")
        pb.directory(engineRoot(""))
        process = pb.start()

        Thread {
            try {
                process?.inputStream?.bufferedReader()?.forEachLine { line -> Log.d(TAG, line) }
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun waitReady(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!isRunning) return false
            if (probe()) return true
            Thread.sleep(500)
        }
        return false
    }

    private fun probe(): Boolean {
        return try {
            val conn = URL(HTTP_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 1500
            conn.readTimeout = 1500
            conn.requestMethod = "GET"
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (_: Exception) {
            false
        }
    }

    fun stop() {
        val proc = process ?: return
        try {
            proc.destroy()
        } catch (_: Exception) {
        }
        process = null
    }
}
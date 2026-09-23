package com.codex.mobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CodexMainActivity"
    }

    private lateinit var webView: WebView
    private lateinit var loadingOverlay: View
    private lateinit var statusText: TextView
    private lateinit var statusDetail: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var serverManager: CodexServerManager
    private lateinit var openCodeEngine: OpenCodeEngine
    private lateinit var engineMenu: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        statusText = findViewById(R.id.statusText)
        statusDetail = findViewById(R.id.statusDetail)
        progressBar = findViewById(R.id.progressBar)

        serverManager = CodexServerManager(this)
        openCodeEngine = OpenCodeEngine(this)

        requestBatteryOptimizationExemption()
        startForegroundService()
        setupWebView()
        setupEnginePicker()

        startSetupFlow()
    }

    private var enginePickerOk = false
    private lateinit var codexStatus: TextView
    private lateinit var codexOpenBtn: TextView
    private lateinit var codexStartBtn: TextView
    private lateinit var openCodeStatus: TextView
    private lateinit var openCodeOpenBtn: TextView
    private lateinit var openCodeStartBtn: TextView

    private fun setupEnginePicker() {
        try {
            @Suppress("DiscouragedApi")
            val menuHost = findViewById<android.widget.FrameLayout>(R.id.engineHost)
            engineMenu = layoutInflater.inflate(R.layout.engine_picker, menuHost, false)
            menuHost.addView(engineMenu)
            engineMenu.visibility = View.GONE

            codexStatus = findViewById(R.id.engineCodexStatus)
            codexStartBtn = findViewById(R.id.engineCodexStart)
            codexOpenBtn = findViewById(R.id.engineCodexOpen)
            openCodeStatus = findViewById(R.id.engineOpenCodeStatus)
            openCodeStartBtn = findViewById(R.id.engineOpenCodeStart)
            openCodeOpenBtn = findViewById(R.id.engineOpenCodeOpen)

            codexStartBtn.setOnClickListener { startCodexEngine() }
            codexOpenBtn.setOnClickListener { openCodexEngine() }
            openCodeStartBtn.setOnClickListener { startOpenCodeEngine() }
            openCodeOpenBtn.setOnClickListener { openOpenCodeEngine() }
            findViewById<View>(R.id.engineCardCodex).setOnClickListener { openCodexEngine() }
            findViewById<View>(R.id.engineCardOpenCode).setOnClickListener { openOpenCodeEngine() }
            findViewById<View>(R.id.engineCardOpenClaw).setOnClickListener { onOpenClawTapped() }

            enginePickerOk = true
        } catch (e: Exception) {
            Log.e(TAG, "Engine picker failed to init: ${e.message}")
        }
    }

    private fun setCodexState(running: Boolean) {
        runOnUiThread {
            codexStatus.text = getString(if (running) R.string.engine_status_running else R.string.engine_status_stopped)
            codexStatus.setBackgroundResource(if (running) R.drawable.status_run else R.drawable.status_soon)
            codexStatus.setTextColor(resources.getColor(if (running) R.color.engine_ready else android.R.color.darker_gray, null))
            codexOpenBtn.isEnabled = running
        }
    }

    private fun setOpenCodeState(running: Boolean, label: String) {
        runOnUiThread {
            openCodeStatus.text = label
            openCodeStatus.setBackgroundResource(if (running) R.drawable.status_run else R.drawable.status_warn2)
            openCodeStatus.setTextColor(resources.getColor(if (running) R.color.engine_ready else android.R.color.darker_gray, null))
            openCodeOpenBtn.isEnabled = running
        }
    }

    private fun startCodexEngine() {
        setCodexState(serverManager.isRunning)
    }

    override fun onDestroy() {
        super.onDestroy()
        serverManager.stopServer()
        openCodeEngine.stop()
        stopService(Intent(this, CodexForegroundService::class.java))
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val pm = getSystemService(PowerManager::class.java) ?: return
        if (pm.isIgnoringBatteryOptimizations(packageName)) return

        try {
            @Suppress("BatteryLife")
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not request battery optimization exemption: ${e.message}")
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, CodexForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            setSupportZoom(false)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String,
            ): Boolean = false

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String,
            ) {
                Log.e(TAG, "WebView error $errorCode ($description) @ $failingUrl")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                Log.d(TAG, "[WebView] ${msg.sourceId()}:${msg.lineNumber()} ${msg.message()}")
                return true
            }
        }
    }

    private fun startSetupFlow() {
        showLoading(true)
        setStatus("Initializing…")

        Thread {
            try {
                runSetup()
            } catch (e: Exception) {
                Log.e(TAG, "Setup failed", e)
                runOnUiThread {
                    showError(e.message ?: "Unknown error")
                }
            }
        }.start()
    }

    private fun runSetup() {
        // Step 1: Extract bootstrap
        if (!BootstrapInstaller.isBootstrapInstalled(this)) {
            updateStatus("Extracting environment…")
            BootstrapInstaller.install(this) { msg -> updateDetail(msg) }
        }
        updateStatus("Environment ready")

        // Step 1b: Install proot (needed for dpkg/apt-get path remapping)
        if (!serverManager.isProotInstalled()) {
            updateStatus("Installing proot…", "Needed for package management")
            val prootOk = serverManager.installProot { msg -> updateDetail(msg) }
            if (!prootOk) {
                throw RuntimeException(getString(R.string.error_bootstrap))
            }
        }
        updateStatus("proot ready")

        // Step 2: Install Node.js
        if (!serverManager.isNodeInstalled()) {
            updateStatus("Installing Node.js (first run)…", "This may take a few minutes")
            val nodeOk = serverManager.installNode { msg -> updateDetail(msg) }
            if (!nodeOk) {
                throw RuntimeException("Failed to install Node.js")
            }
        }
        updateStatus("Node.js ready")

        // Step 2b: Install Python
        if (!serverManager.isPythonInstalled()) {
            updateStatus("Installing Python…")
            val pyOk = serverManager.installPython { msg -> updateDetail(msg) }
            if (!pyOk) {
                Log.w(TAG, "Python install failed — continuing without it")
            }
        }

        // Step 2c: Install bionic-compat.js (Android platform shim for Node.js)
        serverManager.ensureBionicCompat()

        // Step 2d: Install OpenClaw
        if (!serverManager.isOpenClawInstalled()) {
            updateStatus("Installing build dependencies…")
            serverManager.installOpenClawDeps { msg -> updateDetail(msg) }

            updateStatus("Installing OpenClaw…", "This may take several minutes")
            val openclawOk = serverManager.installOpenClaw { msg -> updateDetail(msg) }
            if (!openclawOk) {
                Log.w(TAG, "OpenClaw install failed — continuing without it")
            } else {
                updateStatus("OpenClaw installed")
            }
        }

        // Step 3: Install Codex CLI
        if (!serverManager.isCodexInstalled()) {
            updateStatus("Installing Codex CLI…", "This may take a few minutes")
            val codexOk = serverManager.installCodex { msg -> updateDetail(msg) }
            if (!codexOk) {
                throw RuntimeException("Failed to install Codex")
            }
        }

        // Ensure codex wrapper script exists
        serverManager.ensureCodexWrapperScript()

        // Step 3a: Extract web UI from APK assets (every launch)
        updateStatus("Updating web UI…")
        serverManager.installServerBundle { msg -> updateDetail(msg) }

        // Step 3b: Install native platform binary
        if (!serverManager.isPlatformBinaryInstalled()) {
            updateStatus("Installing Codex platform binary…")
            val binOk = serverManager.installPlatformBinary { msg -> updateDetail(msg) }
            if (!binOk) {
                throw RuntimeException("Failed to install Codex platform binary")
            }
        }
        updateStatus("Codex ready")

        // Step 3c: Write full-access config and create default workspace
        serverManager.ensureFullAccessConfig()
        serverManager.ensureDefaultWorkspace()

        // Step 4: Start CONNECT proxy (needed for native binary DNS/TLS)
        updateStatus("Starting network proxy…")
        if (!serverManager.startProxy()) {
            throw RuntimeException("فشل تشغيل الخادم الوسيط")
        }

        // Step 5: Authenticate via `codex login`
        updateStatus("Checking authentication…")
        if (!serverManager.isLoggedIn()) {
            updateStatus("Login required — opening browser…")
            val authOk = serverManager.loginWithUrl(
                onLoginUrl = { url ->
                    runOnUiThread {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                onProgress = { msg -> updateDetail(msg) },
            )
            if (!authOk && !serverManager.isLoggedIn()) {
                updateStatus("Browser login failed — enter API key manually")
                val apiKey = requestApiKey()
                if (apiKey.isBlank()) {
                    throw RuntimeException("No API key provided")
                }
                val loginOk = serverManager.loginWithApiKey(apiKey)
                if (!loginOk) {
                    throw RuntimeException("Login failed — check your API key")
                }
            }
        }
        updateStatus("Authenticated")

        // Step 6: Health check (informational only — never blocks)
        updateStatus("Verifying API access…", "Optional connectivity check")
        val healthOk = serverManager.healthCheck { msg -> updateDetail(msg) }
        if (!healthOk) {
            Log.w(TAG, "Health check failed — continuing anyway (login already confirmed)")
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Connectivity check failed — continuing anyway.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
        updateStatus("Ready")

        // Step 7: Configure and start OpenClaw
        if (serverManager.isOpenClawInstalled()) {
            updateStatus("Configuring OpenClaw…")
            serverManager.configureOpenClawAuth()

            updateStatus("Starting OpenClaw gateway…")
            serverManager.startOpenClawGateway()

            updateStatus("Starting OpenClaw Control UI…")
            serverManager.startOpenClawControlUiServer()
        }

        // Step 8: Start web server
        updateStatus("Starting server…")
        val started = serverManager.startServer()
        if (!started) {
            throw RuntimeException(getString(R.string.error_server))
        }

        // Step 9: Wait for ready
        updateStatus("Waiting for server…", "Starting…")
        val ready = serverManager.waitForServer(timeoutMs = 90_000) { line ->
            updateStatus("Waiting for server…", line)
        }
        if (!ready) {
            val tail = serverManager.serverLogTail()
            val detail = if (tail.isNotEmpty()) "\n\nLast server output:\n$tail" else ""
            throw RuntimeException("Server did not start in time.$detail")
        }
        updateStatus("Server ready")

        // Step 10: Show engine picker
        runOnUiThread {
            showLoading(false)
            if (enginePickerOk) {
                showEngineMenu()
            } else {
                openCodexEngine()
            }
        }
    }

    /**
     * Engine picker screen. The Codex server keeps running in the background,
     * so tapping "Codex" opens the chat instantly.
     */
    private fun showEngineMenu() {
        engineMenu.visibility = View.VISIBLE
        findViewById<View>(R.id.engineHost).visibility = View.VISIBLE
        webView.visibility = View.GONE
        setCodexState(serverManager.isRunning)
        setOpenCodeState(openCodeEngine.isRunning, getString(R.string.engine_status_stopped))
    }

    private fun openEngine(url: String) {
        engineMenu.visibility = View.GONE
        findViewById<View>(R.id.engineHost).visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    private fun openCodexEngine() {
        if (serverManager.isRunning) {
            openEngine("http://127.0.0.1:${CodexServerManager.SERVER_PORT}/")
            return
        }
        Thread {
            runOnUiThread {
                showLoading(true)
                setStatus("Codex engine", "Starting server…")
            }
            val started = serverManager.startServer()
            val ok = started && serverManager.waitForServer(timeoutMs = 60_000)
            runOnUiThread {
                showLoading(false)
                if (ok) {
                    setCodexState(true)
                    openEngine("http://127.0.0.1:${CodexServerManager.SERVER_PORT}/")
                } else {
                    setCodexState(false)
                    AlertDialog.Builder(this)
                        .setTitle(R.string.error_title)
                        .setMessage(R.string.error_server)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }.start()
    }

    private fun onOpenCodeTapped() {
        startOpenCodeEngine()
    }

    private fun startOpenCodeEngine() {
        if (openCodeEngine.isRunning) {
            setOpenCodeState(true, getString(R.string.engine_status_running))
            return
        }
        val thread = Thread {
            try {
                runOnUiThread {
                    showLoading(true)
                    setStatus("OpenCode engine", "Starting…")
                }

                if (!openCodeEngine.isInstalled) {
                    runOnUiThread {
                        setStatus("OpenCode engine", "Extracting bundled runtime…")
                    }
                    openCodeEngine.install { pct ->
                        runOnUiThread {
                            setStatus("OpenCode engine", "Preparing… $pct%")
                        }
                    }
                    runOnUiThread { setStatus("OpenCode engine", "Preparing runtime…") }
                }

                runOnUiThread { setStatus("OpenCode engine", "Starting server…") }
                openCodeEngine.start { ok ->
                    runOnUiThread {
                        showLoading(false)
                        if (ok) {
                            setOpenCodeState(true, getString(R.string.engine_status_running))
                            openOpenCodeEngine()
                        } else {
                            setOpenCodeState(false, getString(R.string.engine_status_stopped))
                            AlertDialog.Builder(this)
                                .setTitle(R.string.engine_opencode_fail_title)
                                .setMessage(R.string.engine_opencode_fail_msg)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "OpenCode start failed: ${e.message}", e)
                runOnUiThread {
                    showLoading(false)
                    setOpenCodeState(false, getString(R.string.engine_status_stopped))
                    AlertDialog.Builder(this)
                        .setTitle(R.string.engine_opencode_fail_title)
                        .setMessage("${getString(R.string.engine_opencode_fail_msg)}\n${e.message}")
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }
        thread.isDaemon = true
        thread.start()
    }

    private fun openOpenCodeEngine() {
        openEngine(OpenCodeEngine.HTTP_URL)
    }

    private fun onOpenClawTapped() {
        Thread {
            val reachable = probeUrl("http://127.0.0.1:${CodexServerManager.OPENCLAW_CONTROL_UI_PORT}/")
            runOnUiThread {
                if (reachable) {
                    openEngine("http://127.0.0.1:${CodexServerManager.OPENCLAW_CONTROL_UI_PORT}/")
                } else {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.engine_openclaw_dialog_title)
                        .setMessage(R.string.engine_openclaw_dialog_msg)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }.start()
    }

    private fun probeUrl(url: String): Boolean {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 2500
            conn.readTimeout = 2500
            conn.requestMethod = "GET"
            val code = conn.responseCode
            conn.disconnect()
            code in 200..399
        } catch (e: Exception) {
            Log.w(TAG, "probeUrl failed for $url: ${e.message}")
            false
        }
    }

    /**
     * Fallback: prompt for API key if browser login fails.
     */
    private fun requestApiKey(): String {
        var result = ""
        val lock = Object()

        runOnUiThread {
            val input = EditText(this).apply {
                hint = getString(R.string.api_key_hint)
                setSingleLine(true)
            }
            val padding = (24 * resources.displayMetrics.density).toInt()
            val container = android.widget.FrameLayout(this).apply {
                setPadding(padding, padding / 2, padding, 0)
                addView(input)
            }

            AlertDialog.Builder(this)
                .setTitle(R.string.api_key_title)
                .setMessage(R.string.api_key_message)
                .setView(container)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { _, _ ->
                    result = input.text.toString().trim()
                    synchronized(lock) { lock.notifyAll() }
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    synchronized(lock) { lock.notifyAll() }
                }
                .show()
        }

        synchronized(lock) {
            lock.wait(300_000)
        }
        return result
    }

    // ── UI helpers ──────────────────────────────────────────────────────────

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.error_title)
            .setMessage(message)
            .setPositiveButton(R.string.retry) { _, _ ->
                startSetupFlow()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setStatus(text: String, detail: String? = null) {
        statusText.text = text
        if (detail != null) {
            statusDetail.text = detail
            statusDetail.visibility = View.VISIBLE
        } else {
            statusDetail.visibility = View.GONE
        }
    }

    private fun updateStatus(text: String, detail: String? = null) {
        runOnUiThread { setStatus(text, detail) }
    }

    private fun updateDetail(text: String) {
        runOnUiThread {
            statusDetail.text = text
            statusDetail.visibility = View.VISIBLE
        }
    }
}

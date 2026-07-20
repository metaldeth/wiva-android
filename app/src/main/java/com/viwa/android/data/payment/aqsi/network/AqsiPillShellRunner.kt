package com.viwa.android.data.payment.aqsi.network

import android.content.Context
import android.provider.Settings
import java.util.concurrent.TimeUnit
import timber.log.Timber

/**
 * Runs host-level network commands from the app (no adb).
 * Tries direct shell, then common `su` paths used on RK3568 vendor images.
 */
internal object AqsiPillShellRunner {
    private const val TAG = "AQSI_SETUP"
    private const val COMMAND_TIMEOUT_SEC = 8L

    fun clearStaleHttpProxy(context: Context): Boolean {
        val resolver = context.applicationContext.contentResolver
        val current = Settings.Global.getString(resolver, Settings.Global.HTTP_PROXY).orEmpty()
        if (current.isBlank() || current == ":0") {
            Timber.tag(TAG).d("http_proxy already clear: %s", current.ifBlank { "(empty)" })
            return true
        }
        return runSettingsPut("global", "http_proxy", ":0") ||
            runCatching {
                Settings.Global.putString(resolver, Settings.Global.HTTP_PROXY, ":0")
                true
            }.getOrElse {
                Timber.tag(TAG).w(it, "Settings.Global http_proxy clear failed")
                false
            }
    }

    fun runShell(command: String): Boolean {
        val shells =
            listOf(
                listOf("sh", "-c", command),
                listOf("su", "0", "sh", "-c", command),
                listOf("su", "-c", command),
                listOf("/system/bin/su", "0", "sh", "-c", command),
                listOf("/system/xbin/su", "0", "sh", "-c", command),
            )
        for (shell in shells) {
            if (runProcess(shell)) {
                Timber.tag(TAG).d("shell ok: %s", command.take(120))
                return true
            }
        }
        Timber.tag(TAG).w("shell failed: %s", command.take(120))
        return false
    }

    fun runIp(vararg args: String): Boolean {
        val commands =
            listOf(
                listOf("ip", *args),
                listOf("/system/bin/ip", *args),
                listOf("su", "0", "ip", *args),
                listOf("su", "0", "/system/bin/ip", *args),
            )
        for (command in commands) {
            if (runProcess(command)) {
                Timber.tag(TAG).d("ip ok: %s", command.joinToString(" "))
                return true
            }
        }
        return false
    }

    private fun runSettingsPut(namespace: String, key: String, value: String): Boolean =
        runShell("settings put $namespace $key $value")

    private fun runProcess(command: List<String>): Boolean {
        return try {
            val process =
                ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
            val finished = waitForProcess(process)
            if (!finished) {
                process.destroy()
                return false
            }
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.exitValue() != 0) {
                if (output.isNotEmpty()) {
                    Timber.tag(TAG).d(
                        "cmd exit=%d %s -> %s",
                        process.exitValue(),
                        command.firstOrNull(),
                        output.take(160),
                    )
                }
                return false
            }
            true
        } catch (error: Exception) {
            Timber.tag(TAG).d(error, "cmd failed: %s", command.joinToString(" "))
            false
        }
    }

    private fun waitForProcess(process: Process): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(COMMAND_TIMEOUT_SEC)
        while (System.nanoTime() < deadline) {
            try {
                process.exitValue()
                return true
            } catch (_: IllegalThreadStateException) {
                Thread.sleep(100)
            }
        }
        return runCatching {
            process.exitValue()
            true
        }.getOrDefault(false)
    }
}

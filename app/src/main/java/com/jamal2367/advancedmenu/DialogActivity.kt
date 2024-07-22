package com.jamal2367.advancedmenu

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import java.net.Socket

class DialogActivity : AppCompatActivity() {
    private var connection: AdbConnection? = null
    private var stream: AdbStream? = null
    private var myAsyncTask: MyAsyncTask? = null
    private var isDialogReopened = false
    private val ipAddress = "0.0.0.0"
    private val publicKeyName: String = "public.key"
    private val privateKeyName: String = "private.key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onKeyCE()
    }

    private fun showMainOptionsDialog() {
        val shutdown = AppCompatResources.getDrawable(this, R.drawable.ic_power_24dp)!!
        val restart = AppCompatResources.getDrawable(this, R.drawable.ic_restart_24dp)!!
        val forward = AppCompatResources.getDrawable(this, R.drawable.ic_forward_24dp)!!

        shutdown.setBounds(0, 0, shutdown.intrinsicWidth, shutdown.intrinsicHeight)
        restart.setBounds(0, 0, restart.intrinsicWidth, restart.intrinsicHeight)
        forward.setBounds(0, 0, forward.intrinsicWidth, forward.intrinsicHeight)

        val dialogItems = arrayOf(
            DialogItem(shutdown, getString(R.string.shutdown), forward),
            DialogItem(restart, getString(R.string.reboot), forward),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_sleep_24dp)!!, getString(R.string.standby)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_restore_24dp)!!, getString(R.string.recovery)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_fastboot_24dp)!!, getString(R.string.fastboot)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_coreelec_24dp)!!, getString(R.string.coreelec)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_forward_24dp)!!, getString(R.string.more))
        )

        val adapter = DialogAdapter(this, dialogItems)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.advanced_reboot))
            .setIcon(R.drawable.ic_replay_24dp)
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> {
                        isDialogReopened = true
                        showShutdownOptionsDialog()
                    }
                    1 -> {
                        isDialogReopened = true
                        showRebootOptionsDialog()
                    }
                    2 -> standbyCommand()
                    3 -> recoveryCommand()
                    4 -> fastbootCommand()
                    5 -> ceCommand()
                    6 -> {
                        isDialogReopened = true
                        showMoreOptionsDialog()
                    }
                }
            }
            .setOnDismissListener {
                if (!isDialogReopened) {
                    finish()
                }
                isDialogReopened = false
            }
            .show()
    }

    private fun showShutdownOptionsDialog() {
        val dialogItems = arrayOf(
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_power_24dp)!!, getString(R.string.shutdown)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_power_24dp)!!, getString(R.string.hard_shutdown)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_back_24dp)!!, getString(R.string.back))
        )

        val adapter = DialogAdapter(this, dialogItems)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.shutdown)
            .setIcon(R.drawable.ic_power_24dp)
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> shutdownCommand()
                    1 -> hardShutdownCommand()
                    2 -> {
                        isDialogReopened = true
                        showMainOptionsDialog()
                    }
                }
            }
            .setOnDismissListener {
                if (!isDialogReopened) {
                    finish()
                }
                isDialogReopened = false
            }
            .show()
    }

    private fun showRebootOptionsDialog() {
        val dialogItems = arrayOf(
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_restart_24dp)!!, getString(R.string.reboot)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_restart_24dp)!!, getString(R.string.hard_reboot)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_safe_mode_24dp)!!, getString(R.string.reboot_to_safe_mode)),
                    DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_back_24dp)!!, getString(R.string.back))
        )

        val adapter = DialogAdapter(this, dialogItems)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reboot)
            .setIcon(R.drawable.ic_restart_24dp)
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> rebootCommand()
                    1 -> hardRebootCommand()
                    2 -> warningDialog()
                    3 -> {
                        isDialogReopened = true
                        showMainOptionsDialog()
                    }
                }
            }
            .setOnDismissListener {
                if (!isDialogReopened) {
                    finish()
                }
                isDialogReopened = false
            }
            .show()
    }

    private fun showMoreOptionsDialog() {
        val shizuku = AppCompatResources.getDrawable(this, R.drawable.ic_shizuku_24dp)!!
        val hidden = AppCompatResources.getDrawable(this, R.drawable.ic_disabled_visible_24dp)!!
        val forward = AppCompatResources.getDrawable(this, R.drawable.ic_forward_24dp)!!

        shizuku.setBounds(0, 0, shizuku.intrinsicWidth, shizuku.intrinsicHeight)
        hidden.setBounds(0, 0, hidden.intrinsicWidth, hidden.intrinsicHeight)
        forward.setBounds(0, 0, forward.intrinsicWidth, forward.intrinsicHeight)

        val dialogItems = arrayOf(
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_window_24dp)!!, getString(R.string.recents)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_panorama_24dp)!!, getString(R.string.screensaver)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_screenshot_24dp)!!, getString(R.string.screenshot)),
            DialogItem(shizuku, getString(R.string.shizuku), forward),
            DialogItem(hidden, getString(R.string.hidden_menus), forward),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_back_24dp)!!, getString(R.string.back))
        )

        val adapter = DialogAdapter(this, dialogItems)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.more)
            .setIcon(R.drawable.ic_forward_24dp)
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> recentsCommand()
                    1 -> screensaverCommand()
                    2 -> screenshotCommand()
                    3 -> {
                        if (isAppInstalled()) {
                            isDialogReopened = true
                            showShizukuOptionsDialog()
                        } else {
                            shizukuNotInstalledDialog()
                        }
                    }
                    4 -> {
                        isDialogReopened = true
                        showHiddenOptionsDialog()
                    }
                    5 -> {
                        isDialogReopened = true
                        showMainOptionsDialog()
                    }
                }
            }
            .setOnDismissListener {
                if (!isDialogReopened) {
                    finish()
                }
                isDialogReopened = false
            }
            .show()
    }

    private fun showHiddenOptionsDialog() {
        val dialogItems = arrayOf(
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.default_apps)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.standard_launcher)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.edid_information)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.drm_information)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.extended_sound_settings)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.more_extended_sound_settings)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.advanced_options)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.advanced_hdmi_cec_settings)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.dessert_case_easter_egg)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.marshmallow_land_easter_egg)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_back_24dp)!!, getString(R.string.back))
        )

        val adapter = DialogAdapter(this, dialogItems)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hidden_menus_experimental)
            .setIcon(R.drawable.ic_disabled_visible_24dp)
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> changeDefaultAppsCommand()
                    1 -> changeLauncherCommand()
                    2 -> edidCommand()
                    3 -> drmCommand()
                    4 -> extendedSoundCommand()
                    5 -> moreExtendedSoundCommand()
                    6 -> advancedOptionsCommand()
                    7 -> hdmiCECSettingsCommand()
                    8 -> easterEggDessertCaseCommand()
                    9 -> easterEggMarshmallowLandCommand()
                    10 -> {
                        isDialogReopened = true
                        showMoreOptionsDialog()
                    }
                }
            }
            .setOnDismissListener {
                if (!isDialogReopened) {
                    finish()
                }
                isDialogReopened = false
            }
            .show()
    }

    private fun showShizukuOptionsDialog() {
        val dialogItems = arrayOf(
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.open_shizuku)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_build_24dp)!!, getString(R.string.start_shizuku)),
            DialogItem(AppCompatResources.getDrawable(this, R.drawable.ic_back_24dp)!!, getString(R.string.back))
        )

        val adapter = DialogAdapter(this, dialogItems)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.shizuku)
            .setIcon(R.drawable.ic_shizuku_24dp)
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> openShizukuCommand()
                    1 -> startShizukuCommand()
                    2 -> {
                        isDialogReopened = true
                        showMoreOptionsDialog()
                    }
                }
            }
            .setOnDismissListener {
                if (!isDialogReopened) {
                    finish()
                }
                isDialogReopened = false
            }
            .show()
    }

    private fun warningDialog() {
        isDialogReopened = true

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(android.R.string.dialog_alert_title) + "!")
            .setMessage(R.string.not_press_button)
            .setIcon(R.drawable.ic_warning_24dp)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                safeModeCommand()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                isDialogReopened = true
                showMoreOptionsDialog()
            }
            .setOnDismissListener {
                if (!isDialogReopened) {
                    finish()
                }
                isDialogReopened = false
            }
            .show()
    }

    private fun shizukuNotInstalledDialog() {
        isDialogReopened = true

        MaterialAlertDialogBuilder(this)

            .setMessage(R.string.shizuku_is_not_installed)
            .setIcon(R.drawable.ic_warning_24dp)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                isDialogReopened = true
                showMoreOptionsDialog()
            }
            .setOnDismissListener {
                if (!isDialogReopened) {
                    finish()
                }
                isDialogReopened = false
            }
            .show()
    }

    private fun onKeyCE() {
        connection = null
        stream = null

        myAsyncTask?.cancel()
        myAsyncTask = MyAsyncTask(this)
        myAsyncTask?.execute(ipAddress)
    }

    fun adbCommander(ip: String?) {
        Thread {
            try {
                val socket = Socket(ip, 5555)
                val crypto = readCryptoConfig(filesDir) ?: writeNewCryptoConfig(filesDir)

                if (crypto == null) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to generate/load RSA key pair", Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }

                if (stream == null || connection == null) {
                    connection = AdbConnection.create(socket, crypto)
                    connection?.connect()
                }

                runOnUiThread {
                    showMainOptionsDialog()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun shutdownCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:input keyevent 26")
                Log.d("TAG", "State: Normal Standby!")

                // Wait for 5 seconds
                Thread.sleep(5000)

                connection?.open("shell:svc power shutdown")
                Log.d("TAG", "State: Normal Shutdown!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun standbyCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:input keyevent 26")
                Log.d("TAG", "State: Standby!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun rebootCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:svc power reboot")
                Log.d("TAG", "State: Normal Reboot!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun recoveryCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:reboot recovery")
                Log.d("TAG", "State: Recovery!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fastbootCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:reboot bootloader")
                Log.d("TAG", "State: Fastboot!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun ceCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:reboot update")
                Log.d("TAG", "State: CoreELEC!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hardShutdownCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:input keyevent 26")
                Log.d("TAG", "State: Normal Standby!")

                // Wait for 5 seconds
                Thread.sleep(5000)

                connection?.open("shell:reboot -p")
                Log.d("TAG", "State: Hard Shutdown!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hardRebootCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:reboot")
                Log.d("TAG", "State: Hard Reboot!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun safeModeCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am force-stop com.android.tv.settings")

                Thread.sleep(1000)
                connection?.open("shell:am start -n com.android.tv.settings/.MainSettings")

                Thread.sleep(1000)
                connection?.open("shell:for i in \$(seq 1 5); do input keyevent 19; done")

                Thread.sleep(1000)
                connection?.open("shell:for i in \$(seq 1 3); do input keyevent 20; done")

                Thread.sleep(1000)
                connection?.open("shell:input keyevent 66")

                Thread.sleep(1000)
                connection?.open("shell:for i in \$(seq 1 19); do input keyevent 20; done")

                Thread.sleep(2500)
                connection?.open("shell:input keyevent --longpress 23")

                Thread.sleep(2000)
                connection?.open("shell:for i in \$(seq 1 2); do input keyevent 19; done")

                Thread.sleep(2000)
                connection?.open("shell:input keyevent 23")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun recentsCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.nes.recents/.RecentsActivity")
                Log.d("TAG", "State: Recents!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun screensaverCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.android.systemui/.Somnambulator")
                Log.d("TAG", "State: Screensaver!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun screenshotCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Wait for 0.5 seconds
                Thread.sleep(500)

                connection?.open("shell:input keyevent 120")
                Log.d("TAG", "State: Screenshot!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openShizukuCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n moe.shizuku.privileged.api/moe.shizuku.manager.MainActivity")
                Log.d("TAG", "State: Shizuku opened!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startShizukuCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh")

                runOnUiThread {
                    Toast.makeText(baseContext, "Shikuzu started!", Toast.LENGTH_SHORT).show()
                }

                Log.d("TAG", "State: Shizuku started!")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun changeDefaultAppsCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.google.android.permissioncontroller/com.android.permissioncontroller.role.ui.DefaultAppListActivity")
                Log.d("TAG", "State: Default Apps Activity!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun changeLauncherCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.google.android.permissioncontroller/com.android.permissioncontroller.role.ui.HomeSettingsActivity")
                Log.d("TAG", "State: Home Settings Activity!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun edidCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.nes.tvbugtracker/.EdidActivity")
                Log.d("TAG", "State: EDID Activity!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun drmCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.nes.tvbugtracker/.DrmActivity")
                Log.d("TAG", "State: DRM Activity!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun extendedSoundCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.droidlogic.tv.settings/.SoundActivity")
                Log.d("TAG", "State: Extended Sound Activity!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun moreExtendedSoundCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.droidlogic.tv.settings/.soundeffect.SoundModeActivity")
                Log.d("TAG", "State: More Extended Sound Activity!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun advancedOptionsCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.droidlogic.tv.settings/.customdt.AdvancedOptionsActivity")
                Log.d("TAG", "State: Advanced Options Activity!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hdmiCECSettingsCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.droidlogic.tv.settings/.customdt.HdmiCecForDTActivity")
                Log.d("TAG", "State: HDMI CEC Settings Activity!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun easterEggDessertCaseCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.android.systemui/.DessertCase")
                Log.d("TAG", "State: Easter Egg Dessert Case Activity!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun easterEggMarshmallowLandCommand() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.open("shell:am start -n com.android.systemui/.egg.MLandActivity")
                Log.d("TAG", "State: Easter Egg Marshmallow Land Activity!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isAppInstalled(): Boolean {
        val packageName = "moe.shizuku.privileged.api"

        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun readCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, publicKeyName)
        val privKey = File(dataDir, privateKeyName)

        var crypto: AdbCrypto? = null
        if (pubKey.exists() && privKey.exists()) {
            crypto = try {
                AdbCrypto.loadAdbKeyPair(AndroidBase64(), privKey, pubKey)
            } catch (e: Exception) {
                null
            }
        }

        return crypto
    }

    private fun writeNewCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, publicKeyName)
        val privKey = File(dataDir, privateKeyName)

        var crypto: AdbCrypto?

        try {
            crypto = AdbCrypto.generateAdbKeyPair(AndroidBase64())
            crypto.saveAdbKeyPair(privKey, pubKey)
        } catch (e: Exception) {
            crypto = null
        }

        return crypto
    }

    class MyAsyncTask internal constructor(context: DialogActivity) {
        private val activityReference: WeakReference<DialogActivity> = WeakReference(context)
        private var thread: Thread? = null

        fun execute(ip: String?) {
            thread = Thread {
                val activity = activityReference.get()
                activity?.adbCommander(ip)

                if (Thread.interrupted()) {
                    return@Thread
                }
            }
            thread?.start()
        }

        fun cancel() {
            thread?.interrupt()
        }
    }

    class AndroidBase64 : AdbBase64 {
        override fun encodeToString(bArr: ByteArray): String {
            return Base64.encodeToString(bArr, Base64.NO_WRAP)
        }
    }

    data class DialogItem(val icon: Drawable, val text: String, val icon1: Drawable? = null)

    class DialogAdapter(context: Context, private val items: Array<DialogItem>) :
        ArrayAdapter<DialogItem>(context, android.R.layout.select_dialog_item, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = convertView ?: inflater.inflate(R.layout.list_item_with_icon, parent, false)

            val item = items[position]
            val textView = view.findViewById<TextView>(R.id.text)
            textView.text = item.text
            textView.setCompoundDrawablesWithIntrinsicBounds(item.icon, null, item.icon1, null)
            textView.compoundDrawablePadding = 16

            if (position < items.size - 1) {
                view.findViewById<View>(R.id.divider).visibility = View.VISIBLE
            } else {
                view.findViewById<View>(R.id.divider).visibility = View.GONE
            }

            return view
        }
    }
}

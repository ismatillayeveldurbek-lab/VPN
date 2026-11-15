package uz.tulpor.vpn.vpn

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One log file per connection attempt: app events plus Xray's own warnings/errors
 * (Xray appends to the same file via the "log.error" setting). Shown and shared from
 * Settings so problems on a real phone can be diagnosed.
 */
object ConnectionLog {
    private const val MAX_READ = 64 * 1024
    private val time = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun file(context: Context): File = File(context.filesDir, "connection.log")

    fun start(context: Context, header: String) {
        file(context).writeText("")
        add(context, header)
    }

    @Synchronized
    fun add(context: Context, line: String) {
        runCatching { file(context).appendText("${time.format(Date())}  $line\n") }
    }

    fun read(context: Context): String {
        val f = file(context)
        if (!f.exists()) return ""
        val len = f.length()
        return f.inputStream().use { input ->
            if (len > MAX_READ) input.skip(len - MAX_READ)
            input.readBytes().toString(Charsets.UTF_8)
        }
    }

    fun clear(context: Context) {
        file(context).delete()
    }
}

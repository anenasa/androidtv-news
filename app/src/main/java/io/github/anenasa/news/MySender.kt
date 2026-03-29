package io.github.anenasa.news

import android.content.Context
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.io.bufferedWriter


class MySender : ReportSender {
    override fun send(context: Context, errorContent: CrashReportData) {
        try {
            val file = File(context.getExternalFilesDir(null), "log.txt")
            FileOutputStream(file, true).bufferedWriter().use { writer ->
                for ((key, value) in errorContent.toMap()) {
                    writer.write("$key $value\n")
                }
            }
        } catch (e: IOException) {
            throw ReportSenderException("ReportSenderException", e)
        }
    }
}

package sk.pluk64.unibakonto

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.URLConnection
import java.net.URLEncoder
import java.nio.charset.Charset

object Util {
    @Throws(IOException::class, ConnectionFailedException::class)
    fun connInput2String(conn: URLConnection): String {
        val contentType = conn.contentType
        if (contentType == null) {
            throw ConnectionFailedException()
        } else {
            var bufferedReader: BufferedReader? = null
            try {
                val charsetBegining = contentType.indexOf("charset=")
                val charsetName = if (charsetBegining != -1) {
                    contentType.substring(charsetBegining + "charset=".length)
                } else {
                    "utf-8"
                }

                bufferedReader = BufferedReader(
                    InputStreamReader(conn.getInputStream(), charsetName)
                )

                val sb = StringBuilder()
                var line: String? = bufferedReader.readLine()
                while (line != null) {
                    sb.append(line)
                    line = bufferedReader.readLine()
                }
                bufferedReader.close()
                return sb.toString()
            } finally {
                bufferedReader?.close()
            }
        }
    }

    fun paramsMap2PostData(params: Map<String, Any>): ByteArray {
        return paramsArray2PostData(
            params.asSequence()
                .flatMap { sequenceOf(it.key, it.value.toString()) }
                .toList()
        )
    }

    internal fun paramsArray2PostData(postParams: List<String>): ByteArray {
        return postParams.asSequence()
            .map {
                try {
                    URLEncoder.encode(it, "UTF-8")
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                    it
                }
            }
            .zipWithNext { a, b -> "$a=$b" }
            .joinToString("&")
            .toByteArray(Charset.forName("UTF-8"))
    }

    class ConnectionFailedException : Exception()
}
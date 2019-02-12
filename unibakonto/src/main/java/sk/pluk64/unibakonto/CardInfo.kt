package sk.pluk64.unibakonto

import kotlinx.serialization.Serializable

@Serializable
data class CardInfo internal constructor(val number: String, val released: String, val validFrom: String, val validUntil: String)


fun String.divideBy4Digits(): String {
    val resultBuilder = StringBuilder()

    val blockLength = 4
    val length = length
    val firstBlockLength = length % blockLength
    resultBuilder.append(this, 0, firstBlockLength)

    var i = firstBlockLength
    while (i < length) {
        if (i > 0) {
            resultBuilder.append(" ")
        }
        resultBuilder.append(this, i, i + blockLength)
        i += blockLength
    }

    return resultBuilder.toString()
}

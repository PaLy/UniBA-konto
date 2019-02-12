package sk.pluk64.unibakonto

import kotlinx.serialization.Serializable

@Serializable
data class Balance internal constructor(val label: String, val price: String)

@Serializable
data class Balances (val map: Map<String, Balance>) {
    companion object {
        val EMPTY = Balances(emptyMap())
    }
}

package sk.pluk64.unibakonto

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Transaction(val transactionItems: List<TransactionItem>) {

    @Transient
    val timestamp by lazy {
        transactionItems.asSequence().mapNotNull { it.timestamp }.lastOrNull() ?: ""
    }

    @Transient
    val parsedTimestamp by lazy {
        transactionItems.asSequence().mapNotNull { it.parsedTimestamp }.lastOrNull()
    }

    @Transient
    val totalAmount by lazy {
        transactionItems.asSequence().mapNotNull { it.parsedAmount }.sum()
    }
}

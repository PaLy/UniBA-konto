package sk.pluk64.unibakonto

interface IsUnibaKonto {
    val username: String

    val password: String

    val isLoggedIn: Boolean

    val balances: Balances
        @Throws(Util.ConnectionFailedException::class)
        get() = TODO()

    val clientName: String
        @Throws(Util.ConnectionFailedException::class)
        get() = TODO()

    val allTransactions: List<Transaction>
        @Throws(Util.ConnectionFailedException::class)
        get() = TODO()

    val transactions: List<Transaction>
        @Throws(Util.ConnectionFailedException::class)
        get() = TODO()

    val cards: List<CardInfo>
        @Throws(Util.ConnectionFailedException::class)
        get() = TODO()

    @Throws(Util.ConnectionFailedException::class)
    fun login()

    fun isLoggedIn(refresh: Boolean): Boolean
}

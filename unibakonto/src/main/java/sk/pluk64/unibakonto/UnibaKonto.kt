package sk.pluk64.unibakonto

import com.google.common.base.Joiner
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.DataOutputStream
import java.io.IOException
import java.net.CookieHandler
import java.net.CookieManager
import java.net.URL
import java.net.URLConnection
import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.LinkedHashMap
import kotlin.collections.set

class UnibaKonto(override val username: String, override val password: String) : IsUnibaKonto {
    private val documents = ParsedDocumentCache()

    @Throws(Util.ConnectionFailedException::class)
    override fun login() {
        CookieHandler.setDefault(CookieManager())
        documents.cache.clear()
        try {
            httpGet(MOJA_UNIBA_LOGIN_PAGE) // sets cookie
            mojaUnibaLogin()
            kontoLogin() // TODO toto treba spravit asi po nejakom case opakovane???
        } catch (e: IOException) {
            throw Util.ConnectionFailedException()
        }
    }

    override fun isLoggedIn(refresh: Boolean): Boolean {
        return if (refresh) {
            try {
                !documents.getRefreshed(CLIENT_INF_PAGE).select(ID_VAR_SYMBOL).isEmpty()
            } catch (e: Util.ConnectionFailedException) {
                false
            }
        } else {
            isLoggedIn
        }
    }

    override val isLoggedIn: Boolean
        get() = try {
            !documents[CLIENT_INF_PAGE].select(ID_VAR_SYMBOL).isEmpty()
        } catch (e: Util.ConnectionFailedException) {
            false
        }

    @Throws(IOException::class)
    private fun mojaUnibaLogin() {
        // TODO it would be better to parse these data from login page
        httpPost(
            UNIBA_LOGIN_PAGE,
            Util.paramsArray2PostData(
                listOf("ref", MOJA_UNIBA_LOGIN_PAGE, "login", username, "password", password)
            )
        )
    }

    override val balances: Balances
        @Throws(Util.ConnectionFailedException::class)
        get() {
            val result = LinkedHashMap<String, Balance>()
            val doc = documents[CLIENT_INF_PAGE]

            val ids = arrayOf(ID_ACCOUNT, ID_DEPOSIT, ID_DEPOSIT2, ID_ZALOHA)

            for (id in ids) {
                val valueElem = doc.select(id)
                // ak je zaloha na ubytovanie nulova, tak nie je zobrazena
                if (valueElem.size != 0) {
                    val label = valueElem[0].previousElementSibling().text()
                    val price = valueElem.text()
                    val condensedPrice =
                        Joiner.on("").join(
                            price.split(" ").filterNot { it.isEmpty() }
                        )
                    result[id] = Balance(label, condensedPrice)
                }
            }
            return Balances(result)
        }

    override val clientName: String
        @Throws(Util.ConnectionFailedException::class)
        get() {
            val doc = documents[CLIENT_INF_PAGE]

            val result = StringBuilder()

            val ids = arrayOf(ID_NAME, ID_SURNAME)
            for (id in ids) {
                val names = doc.select(id)
                val name = names.first()
                if (name != null) {
                    if (result.isNotEmpty()) {
                        result.append(" ")
                    }
                    result.append(name.text())
                }
            }

            return result.toString()
    }

    override val allTransactions: List<Transaction>
        @Throws(Util.ConnectionFailedException::class)
        get() {
            val forms = documents[TRANSACTIONS_PAGE].select(ID_TRANSACTIONS_FORM)
            val form = forms.first()

            if (form != null) {
                val formData = HashMap<String, String>()

                val hiddenInputs = form.select("input[type=hidden]")
                for (hiddenInput in hiddenInputs) {
                    val name = hiddenInput.attr("name")
                    val value = hiddenInput.attr("value")
                    formData[name] = value
                }
                formData[INPUT_NAME_TRANSACTIONS_EVENT_TARGET] = ID_TRANSACTIONS_ALL_OPERATIONS
                formData[INPUT_NAME_TRANSACTIONS_EVENT_ARGUMENT] = ""

                val allOperationsRadios = form.select(ID_TRANSACTIONS_ALL_OPERATIONS)
                val allOperationsRadio = allOperationsRadios.first()
                if (allOperationsRadio != null) {
                    val name = allOperationsRadio.attr("name")
                    val value = allOperationsRadio.attr("value")
                    formData[name] = value
                }

                val postData = Util.paramsMap2PostData(formData)

                try {
                    val post = httpPost(TRANSACTIONS_PAGE, postData)
                    val allTransactionsHtml = Util.connInput2String(post)
                    val allTransactionsDoc = Jsoup.parse(allTransactionsHtml)

                    return parseTransactions(allTransactionsDoc)
                } catch (e: IOException) {
                    throw Util.ConnectionFailedException()
                }

            }

            return emptyList()
    }

    override val transactions: List<Transaction>
        @Throws(Util.ConnectionFailedException::class)
        get() {
            return parseTransactions(documents.getRefreshed(TRANSACTIONS_PAGE))
        }

    private fun parseTransactions(page: Document): List<Transaction> {
        val table = page.select(ID_TRANSACTIONS_HISTORY)
        val first = table.first()
        val tableRows = if (first == null || first.children().size == 0) {
            Elements()
        } else {
            first.child(0).children()
        }

        val items = ArrayList<TransactionItem>()
        for (i in 1 until tableRows.size) {
            items.add(TransactionItem.fromElement(tableRows[i]))
        }

        val groupedItems = ArrayList<ArrayList<TransactionItem>>()
        if (!items.isEmpty()) {
            groupedItems.add(ArrayList())
            groupedItems.last().add(items[0])
        }
        for (i in 1 until items.size) {
            val curItem = items[i]
            val curItemTime = curItem.parsedTimestamp?.time
            val prevItemTime = items[i - 1].parsedTimestamp?.time

            if (curItemTime != null
                && prevItemTime != null
                && Math.abs(curItemTime - prevItemTime) >= 10 * 1000) { // 10 seconds
                groupedItems.add(ArrayList())
            }
            groupedItems.last().add(curItem)
        }

        return groupedItems.asSequence()
            .map { it.asReversed() }
            .map { Transaction(it) }
            .toList()
    }

    override val cards: List<CardInfo>
        @Throws(Util.ConnectionFailedException::class)
        get() {
            val tables = documents.getRefreshed(CARDS_PAGE).select(ID_CARDS_TABLE)
            val firstTable = tables.first()

            val cards = ArrayList<CardInfo>()

            if (firstTable != null) {
                val rows = firstTable.select("tr")
                for (i in 1 until rows.size) {
                    val row = rows[i]
                    val cols = row.select("td")
                    if (cols.size >= 5) {
                        cards.add(CardInfo(
                            cols[1].text().divideBy4Digits(),
                            cols[2].text(),
                            cols[3].text(),
                            cols[4].text()
                        ))
                    }
                }
            }
            return cards
    }

    private class KontoParsedData @Throws(IOException::class, Util.ConnectionFailedException::class)
    constructor(parseFrom: URLConnection) {
        val postData: ByteArray
        val action: String

        init {
            val html = Util.connInput2String(parseFrom)
            val kontoDoc = Jsoup.parse(html)
            val paramsArray = ArrayList<String>()
            for (input in kontoDoc.select("input")) {
                if (input.hasAttr("value")) {
                    paramsArray.add(input.attr("name"))
                    paramsArray.add(input.attr("value"))
                }
            }
            postData = Util.paramsArray2PostData(paramsArray)
            action = kontoDoc.select("form").attr("action")
        }
    }

    private class ParsedDocumentCache {
        val cache = HashMap<String, Document>()

        @Throws(Util.ConnectionFailedException::class)
        operator fun get(location: String): Document {
            return cache[location] ?: refresh(location)
        }

        @Throws(Util.ConnectionFailedException::class)
        private fun refresh(location: String): Document {
            val html: String
            try {
                val connection = UnibaKonto.httpGet(location)
                html = Util.connInput2String(connection)
            } catch (e: IOException) {
                throw Util.ConnectionFailedException()
            }

            val document = Jsoup.parse(html)
            cache[location] = document
            return document
        }

        @Throws(Util.ConnectionFailedException::class)
        fun getRefreshed(location: String): Document {
            return refresh(location)
        }
    }

    companion object {
        private const val MOJA_UNIBA_LOGIN_PAGE = "https://moja.uniba.sk//cosauth/cosauth.php"
        private const val UNIBA_LOGIN_PAGE = "https://login.uniba.sk/cosign.cgi"
        private const val KONTO_LOGIN_PAGE = "https://konto.uniba.sk/"

        private const val CLIENT_INF_PAGE = "https://konto.uniba.sk/Secure/UserAccount.aspx"
        const val ID_ACCOUNT = "#ctl00_ContentPlaceHolderMain_lblAccount"
        const val ID_DEPOSIT = "#ctl00_ContentPlaceHolderMain_lblFund"
        const val ID_DEPOSIT2 = "#ctl00_ContentPlaceHolderMain_lblFund2"
        const val ID_ZALOHA = "#ctl00_ContentPlaceHolderMain_lblZaloha"
        private const val ID_VAR_SYMBOL = "#ctl00_ContentPlaceHolderMain_lblVarSymbol"
        private const val ID_NAME = "#ctl00_ContentPlaceHolderMain_lblName"
        private const val ID_SURNAME = "#ctl00_ContentPlaceHolderMain_lblSurname"

        private const val TRANSACTIONS_PAGE = "https://konto.uniba.sk/Secure/Operace.aspx"
        private const val ID_TRANSACTIONS_HISTORY = "#ctl00_ContentPlaceHolderMain_gvAccountHistory"
        private const val ID_TRANSACTIONS_FORM = "#aspnetForm"
        private const val ID_TRANSACTIONS_ALL_OPERATIONS = "#ctl00_ContentPlaceHolderMain_rbttnComplAccHist"
        private const val INPUT_NAME_TRANSACTIONS_EVENT_TARGET = "__EVENTTARGET"
        private const val INPUT_NAME_TRANSACTIONS_EVENT_ARGUMENT = "__EVENTARGUMENT"

        private const val CARDS_PAGE = "https://konto.uniba.sk/Secure/UserCards.aspx"
        private const val ID_CARDS_TABLE = "#ctl00_ContentPlaceHolderMain_gvUserCards"

        val EMPTY = UnibaKonto("", "")

        @Throws(IOException::class, Util.ConnectionFailedException::class)
        private fun kontoLogin() {
            val kontoLoginPageConn = httpGet(KONTO_LOGIN_PAGE) // sets cookie

            val parsedData = KontoParsedData(kontoLoginPageConn)
            httpPost(KONTO_LOGIN_PAGE + parsedData.action, parsedData.postData)
        }

        @Throws(IOException::class)
        fun httpGet(location: String): URLConnection {
            val conn = URL(location).openConnection()
            conn.connect()
            conn.headerFields
            return conn
        }

        @Throws(IOException::class)
        private fun httpPost(location: String, postData: ByteArray): URLConnection {
            val conn = URL(location).openConnection()
            conn.doOutput = true
            DataOutputStream(conn.getOutputStream()).write(postData)
            conn.connect()
            conn.headerFields
            return conn
        }

        private fun printCookies() {
            println((CookieHandler.getDefault() as CookieManager).cookieStore.cookies)
        }
    }
}

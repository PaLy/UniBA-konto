package sk.pluk64.unibakontoapp.fragments.menu

import kotlinx.serialization.Serializable
import sk.pluk64.unibakontoapp.preferencesutils.DateSerializer
import java.util.*

@Serializable
data class FBPhoto(
    val source: String = "",
    val width: Int = 0,
    val height: Int = 0,
    @Serializable(with = DateSerializer::class)
    val createdTime: Date? = null,
    val caption: String = "",
    val seqNo: Int = 0,
    val fbUrl: String = ""
)

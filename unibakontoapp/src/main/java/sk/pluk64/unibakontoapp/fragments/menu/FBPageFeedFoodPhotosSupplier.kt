package sk.pluk64.unibakontoapp.fragments.menu

import android.os.Bundle
import com.facebook.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import sk.pluk64.unibakonto.Util
import sk.pluk64.unibakontoapp.DateUtils
import sk.pluk64.unibakontoapp.FBUtils
import sk.pluk64.unibakontoapp.Utils
import sk.pluk64.unibakontoapp.meals.Canteen
import java.text.ParseException
import java.util.*

class FBPageFeedFoodPhotosSupplier(private val jedalen: Canteen) : FoodPhotosSupplier {

    override val photos: List<FBPhoto>
        @Throws(Utils.FBAuthenticationException::class, Util.ConnectionFailedException::class)
        get() {
            val getPostsRequest = createGetPostsRequest(jedalen.fbId)

            val graphResponse = getPostsRequest.executeAndWait()

            return processResponse(graphResponse)
        }

    @Throws(Utils.FBAuthenticationException::class, Util.ConnectionFailedException::class)
    private fun processResponse(response: GraphResponse): List<FBPhoto> {
        var response = response
        val result = ArrayList<FBPhoto>()

        val requestsLimit = 10
        var requestCounter = 1
        var lastPostIsTooOld = false
        while (true) {
            val error = response.error
            val responseJSONObject = response.jsonObject

            if (error != null) {
                return resolveFBRequestError(error)
            } else if (responseJSONObject != null) {
                var posts: JSONArray? = null
                try {
                    posts = responseJSONObject.getJSONArray("data")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                if (posts != null) {
                    for (i in 0 until posts.length()) {
                        try {
                            val post = posts.getJSONObject(i)
                            if (post != null) {
                                val createdTime = FBUtils.parseDate(post.getString("created_time"))

                                lastPostIsTooOld = !DateUtils.isAtMostXHoursOld(createdTime, 20)

                                if (!lastPostIsTooOld) {
                                    if (hasOnlyOneAttachment(post)) {
                                        parseSingleAttachment(result, createdTime, post)
                                    } else {
                                        if (!someAttachmentDescriptionEqualsPostMessage(post)) {
                                            val message = post.optString("message")
                                            if (!message.isEmpty()) {
                                                result.add(FBPhoto(
                                                    caption = message,
                                                    createdTime = createdTime)
                                                )
                                            }
                                        }

                                        post.optJSONObject("attachments")?.let {
                                            parseAttachments(result, createdTime, it)
                                        }
                                    }
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }

                    }
                }
            }

            if (lastPostIsTooOld || requestCounter == requestsLimit) {
                break
            } else {
                val nextPage = response.getRequestForPagedResults(GraphResponse.PagingDirection.NEXT)
                if (nextPage != null) {
                    requestCounter += 1
                    response = nextPage.executeAndWait()
                }
            }
        }

        return result
    }

    private fun parseSingleAttachment(result: ArrayList<FBPhoto>, createdTime: Date, post: JSONObject) {
        val message = post.optString("message")
        val attachments = post.optJSONObject("attachments")
        val attachmentsArray = attachments?.optJSONArray("data")
        if (attachmentsArray?.length() == 1) {
            val attachment = attachmentsArray.optJSONObject(0)
            attachment?.let {
                parseAttachment(result, createdTime, it, message)
            }
        }
    }

    private fun parseAttachments(result: ArrayList<FBPhoto>, createdTime: Date, attachments: JSONObject) {
        val attachmentsArray = attachments.optJSONArray("data")
        if (attachmentsArray != null) {
            for (i in 0 until attachmentsArray.length()) {
                attachmentsArray.optJSONObject(i)?.let {
                    parseAttachment(result, createdTime, it)
                }
            }
        }
    }

    private fun parseAttachment(result: ArrayList<FBPhoto>, createdTime: Date, attachment: JSONObject, prefixMessage: String = "") {
        val attachmentType = attachment.optString("type")
        when (attachmentType) {
            "album" -> {
                attachment.optJSONObject("subattachments")?.let {
                    parseAttachments(result, createdTime, it)
                }
            }
            "photo" -> parsePhoto(result, attachment, createdTime, prefixMessage)
            else -> {
                val url = attachment.optString("url")
                var description = attachment.optString("description")
                if (!prefixMessage.isEmpty() && prefixMessage != description) {
                    description = if (description.isEmpty()) {
                        prefixMessage
                    } else {
                        prefixMessage + "\n\n" + description
                    }
                }
                val caption = if (url.isEmpty()) {
                    description
                } else {
                    description + "\n\n" + url
                }
                val fbPhoto = FBPhoto(
                    createdTime = createdTime,
                    caption = caption
                )
                result.add(fbPhoto)
            }
        }
    }

    private fun hasOnlyOneAttachment(post: JSONObject): Boolean {
        post.optJSONObject("attachments")?.let {
            val attachmentsArray = it.optJSONArray("data")
            if (attachmentsArray?.length() == 1) {
                attachmentsArray.optJSONObject(0)?.let {
                    val type = it.optString("type")
                    if (type != "album") {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun someAttachmentDescriptionEqualsPostMessage(post: JSONObject): Boolean {
        val message = post.optString("message")
        if (!message.isEmpty()) {
            post.optJSONObject("attachments")?.let {
                it.optJSONArray("data")?.let {
                    for (i in 0 until it.length()) {
                        it.optJSONObject(i)?.let {
                            val description = it.optString("description")
                            if (description == message) {
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    private fun parsePhoto(result: ArrayList<FBPhoto>, subattachment: JSONObject, createdTime: Date, prefixMessage: String) {
        subattachment.optJSONObject("media")?.let {
            it.optJSONObject("image")?.let {
                val height = it.optInt("height")
                val width = it.optInt("width")
                val src = it.optString("src")
                if (height != 0 && width != 0 && !src.isEmpty()) {
                    var description = subattachment.optString("description")
                    if (!prefixMessage.isEmpty() && prefixMessage != description) {
                        description = prefixMessage + "\n\n" + description
                    }

                    result.add(FBPhoto(
                        width = width,
                        height = height,
                        source = src,
                        caption = description,
                        createdTime = createdTime,
                        fbUrl = subattachment.optString("url")
                    ))
                }
            }
        }
    }

    private fun createGetPostsRequest(menzaFBid: String): GraphRequest {
        val request = GraphRequest(
            AccessToken.getCurrentAccessToken(),
            "/$menzaFBid/posts?limit=2",
            null,
            HttpMethod.GET
        )
        val params = Bundle()
        params.putString("fields", "created_time,message,attachments{description,media,type,url,subattachments.limit(100){description,media,type,url}}")
        request.parameters = params
        return request
    }

    companion object {

        @Throws(Utils.FBAuthenticationException::class, Util.ConnectionFailedException::class)
        internal fun resolveFBRequestError(error: FacebookRequestError): List<FBPhoto> {
            val errorCode = error.errorCode
            val errorType = error.errorType
            if (errorCode == 102 || errorCode == 190 || "OAuthException" == errorType) {
                throw Utils.FBAuthenticationException()
            } else {
                throw Util.ConnectionFailedException()
            }
        }
    }
}

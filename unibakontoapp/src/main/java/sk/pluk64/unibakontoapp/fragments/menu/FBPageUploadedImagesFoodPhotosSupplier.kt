package sk.pluk64.unibakontoapp.fragments.menu

import android.os.Bundle
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.HttpMethod
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

class FBPageUploadedImagesFoodPhotosSupplier(private val canteen: Canteen) : FoodPhotosSupplier {

    override val photos: List<FBPhoto>
        @Throws(Utils.FBAuthenticationException::class, Util.ConnectionFailedException::class)
        get() {
        val getPhotosRequest = createGetPhotosRequest(canteen.fbId)

        val photosResponse = getPhotosRequest.executeAndWait()

        return processPhotosResponse(photosResponse)
    }

    private fun createGetPhotosRequest(menzaFBid: String): GraphRequest {
        val request = GraphRequest(
            AccessToken.getCurrentAccessToken(),
            "/$menzaFBid/photos?type=uploaded&limit=30",
            null,
            HttpMethod.GET
        )
        val params = Bundle()
        params.putString("fields", "created_time, images, name")
        request.parameters = params
        return request
    }

    @Throws(Utils.FBAuthenticationException::class, Util.ConnectionFailedException::class)
    private fun processPhotosResponse(response: GraphResponse): List<FBPhoto> {
        var response = response
        val result = ArrayList<FBPhoto>()

        var lastPhotoIsTooOld = false
        val requestsLimit = 4
        var requestCounter = 1
        while (true) {
            val error = response.error
            val responseJSONObject = response.jsonObject

            if (error != null) {
                FBPageFeedFoodPhotosSupplier.resolveFBRequestError(error)
            } else if (responseJSONObject != null) {
                var photos: JSONArray? = null
                try {
                    photos = responseJSONObject.getJSONArray("data")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                if (photos != null) {
                    for (i in 0 until photos.length()) {
                        try {
                            val photo = photos.getJSONObject(i)

                            val createdTime = FBUtils.parseDate(photo.getString("created_time"))
                            lastPhotoIsTooOld = !DateUtils.isAtMostXHoursOld(createdTime, 20)

                            if (!lastPhotoIsTooOld) {
                                val chosenImage = chooseBestFbPhotoImage(photo)
                                result.add(FBPhoto(
                                    source = chosenImage.getString("source"),
                                    width = chosenImage.getInt("width"),
                                    height = chosenImage.getInt("height"),
                                    createdTime = createdTime,
                                    caption = photo.optString("name")
                                ))
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }

                    }
                }
            }

            if (lastPhotoIsTooOld || requestCounter == requestsLimit) {
                break
            } else {
                val nextPage = response.getRequestForPagedResults(GraphResponse.PagingDirection.NEXT)
                if (nextPage != null) {
                    requestCounter += 1
                    response = nextPage.executeAndWait()
                }
            }
        }

        return result.asSequence()
            .zip((0 until result.size).asSequence())
            .map { it.first.copy(seqNo = it.second) }
            .sortedWith(
                Comparator { o1, o2 ->
                    val o1CreatedTime = o1.createdTime
                    val o2CreatedTime = o2.createdTime
                    val compDates: Int = if (o1CreatedTime == null && o2CreatedTime == null) {
                        0
                    } else if (o1CreatedTime == null) {
                        1
                    } else if (o2CreatedTime == null) {
                        -1
                    } else {
                        o1CreatedTime.compareTo(o2CreatedTime)
                    }

                    if (compDates == 0) {
                        -compareInts(o1.seqNo, o2.seqNo)
                    } else {
                        compDates
                    }
                }
            ).toList()
    }

    private fun compareInts(x: Int, y: Int): Int {
        return if (x < y) -1 else if (x == y) 0 else 1
    }

    companion object {

        @Throws(JSONException::class)
        internal fun chooseBestFbPhotoImage(photo: JSONObject): JSONObject {
            val images = photo.getJSONArray("images")

            val maxWidth = 999999
            var bestWidth = 0
            var chosenImage: JSONObject? = null

            for (j in 0 until images.length()) {
                try {
                    val image = images.getJSONObject(j)
                    val width = image.getInt("width")
                    if (bestWidth == 0 || width in (bestWidth + 1)..maxWidth || bestWidth > maxWidth && width < bestWidth) {
                        bestWidth = width
                        chosenImage = image
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
            // photos widths: 480, 960, 800, 640, 426, 720, 320, 405, 130, 168, 2048, 1440, 173, 300, 1080, 960,

            if (chosenImage == null) {
                throw JSONException("")
            }
            return chosenImage
        }
    }
}

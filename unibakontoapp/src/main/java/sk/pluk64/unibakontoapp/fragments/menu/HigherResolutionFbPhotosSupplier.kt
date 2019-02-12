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
import sk.pluk64.unibakontoapp.FBUtils
import sk.pluk64.unibakontoapp.Utils
import sk.pluk64.unibakontoapp.meals.Canteen
import java.text.ParseException
import java.util.*

class HigherResolutionFbPhotosSupplier(private val canteen: Canteen, private val lowerResolutionPhotos: List<FBPhoto>) : FoodPhotosSupplier {
    private val oldestPhotoDate by lazy {
        getOldestDate(lowerResolutionPhotos)
    }

    private fun getOldestDate(lowerResolutionPhotos: List<FBPhoto>): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, -24)
        val before24Hours = calendar.time

        return lowerResolutionPhotos.asSequence()
            .map { it.createdTime }
            .filterNotNull()
            .sorted()
            .ifEmpty { sequenceOf(before24Hours) }
            .first()
    }

    override val photos: List<FBPhoto>
        @Throws(Utils.FBAuthenticationException::class, Util.ConnectionFailedException::class)
        get() {
        val getPhotosRequest = createGetPhotosRequest(canteen.fbId)

        val photosResponse = getPhotosRequest.executeAndWait()

        val linkToHighResImage = processPhotosResponse(photosResponse)

        return updateWithHighResImage(lowerResolutionPhotos, linkToHighResImage)
    }

    private fun updateWithHighResImage(lowerResolutionPhotos: List<FBPhoto>, linkToHighResImage: Map<String, JSONObject>): List<FBPhoto> {
        return lowerResolutionPhotos.asSequence()
            .map {
                val image = linkToHighResImage[it.fbUrl]
                if (image != null) {
                    val highResWidth = image.optInt("width")
                    val highResHeight = image.optInt("height")
                    val source = image.optString("source")
                    if (highResWidth > it.width && highResHeight > it.height && !source.isEmpty()) {
                        it.copy(
                            height = highResHeight,
                            width = highResWidth,
                            source = source
                        )
                    } else {
                        it
                    }
                } else {
                    it
                }
            }.toList()
    }

    @Throws(Utils.FBAuthenticationException::class, Util.ConnectionFailedException::class)
    private fun processPhotosResponse(response: GraphResponse): Map<String, JSONObject> {
        var response = response
        val linkToImage = HashMap<String, JSONObject>()

        var lastPhotoIsTooOld = false
        val REQUESTS_LIMIT = 4
        var requestCounter = 1
        while (true) {
            val error = response.error

            if (error != null) {
                FBPageFeedFoodPhotosSupplier.resolveFBRequestError(error)
            } else if (response.jsonObject != null) {
                var photos: JSONArray? = null
                try {
                    photos = response.jsonObject.getJSONArray("data")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                if (photos != null) {
                    for (i in 0 until photos.length()) {
                        try {
                            val photo = photos.getJSONObject(i)

                            val createdTime = FBUtils.parseDate(photo.getString("created_time"))
                            lastPhotoIsTooOld = createdTime < oldestPhotoDate

                            if (!lastPhotoIsTooOld) {
                                val link = photo.getString("link")
                                val chosenImage = FBPageUploadedImagesFoodPhotosSupplier.chooseBestFbPhotoImage(photo)
                                linkToImage[link] = chosenImage
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }

                    }
                }
            }

            if (lastPhotoIsTooOld || requestCounter == REQUESTS_LIMIT) {
                break
            } else {
                val nextPage = response.getRequestForPagedResults(GraphResponse.PagingDirection.NEXT)
                if (nextPage != null) {
                    requestCounter += 1
                    response = nextPage.executeAndWait()
                }
            }
        }

        return linkToImage
    }

    private fun createGetPhotosRequest(canteenFbId: String): GraphRequest {
        val request = GraphRequest(
            AccessToken.getCurrentAccessToken(),
            "/$canteenFbId/photos?type=uploaded&limit=30", null,
            HttpMethod.GET
        )
        val params = Bundle()
        params.putString("fields", "created_time, images, link")
        request.parameters = params
        return request
    }
}

package sk.pluk64.unibakontoapp.fragments.menu;

import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java9.util.Objects;
import java9.util.stream.Collectors;
import java9.util.stream.StreamSupport;
import sk.pluk64.unibakonto.Util;
import sk.pluk64.unibakontoapp.FBUtils;
import sk.pluk64.unibakontoapp.Utils;
import sk.pluk64.unibakontoapp.meals.Menza;

public class HigherResolutionFbPhotosSupplier implements FoodPhotosSupplier {
    private final Menza menza;
    private final List<FBPhoto> lowerResolutionPhotos;
    private final Date oldestPhotoDate;

    public HigherResolutionFbPhotosSupplier(Menza menza, List<FBPhoto> lowerResolutionPhotos) {
        this.menza = menza;
        this.lowerResolutionPhotos = lowerResolutionPhotos;
        oldestPhotoDate = getOldestDate(lowerResolutionPhotos);
    }

    private Date getOldestDate(List<FBPhoto> lowerResolutionPhotos) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -24);
        Date before24Hours = calendar.getTime();

        return StreamSupport.stream(lowerResolutionPhotos)
            .map(FBPhoto::getCreatedTime)
            .filter(Objects::nonNull)
            .sorted()
            .findFirst()
            .orElse(before24Hours);
    }

    @Override
    public List<FBPhoto> getPhotos() throws Utils.FBAuthenticationException, Util.ConnectionFailedException {
        GraphRequest getPhotosRequest = createGetPhotosRequest(menza.getFbId());

        GraphResponse photosResponse = getPhotosRequest.executeAndWait();

        Map<String, JSONObject> linkToHighResImage = processPhotosResponse(photosResponse);

        return updateWithHighResImage(lowerResolutionPhotos, linkToHighResImage);
    }

    private static List<FBPhoto> updateWithHighResImage(List<FBPhoto> lowerResolutionPhotos, Map<String, JSONObject> linkToHighResImage) {
        return StreamSupport.stream(lowerResolutionPhotos)
            .peek(photo -> {
                JSONObject image = linkToHighResImage.get(photo.getFbUrl());
                if (image != null) {
                    int highResWidth = image.optInt("width");
                    int highResHeight = image.optInt("height");
                    String source = image.optString("source");
                    if (highResWidth > photo.getWidth() && highResHeight > photo.getHeight() && !source.isEmpty()) {
                        photo.setHeight(highResHeight);
                        photo.setWidth(highResWidth);
                        photo.setSource(source);
                    }
                }
            })
            .collect(Collectors.toList());
    }

    private Map<String, JSONObject> processPhotosResponse(GraphResponse response) throws Utils.FBAuthenticationException, Util.ConnectionFailedException {
        Map<String, JSONObject> linkToImage = new HashMap<>();

        boolean lastPhotoIsTooOld = false;
        int REQUESTS_LIMIT = 4;
        int requestCounter = 1;
        while (true) {
            FacebookRequestError error = response.getError();
            JSONObject responseJSONObject = response.getJSONObject();

            if (error != null) {
                FBPageFeedFoodPhotosSupplier.resolveFBRequestError(error);
            } else if (responseJSONObject != null) {
                JSONArray photos = null;
                try {
                    photos = responseJSONObject.getJSONArray("data");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (photos != null) {
                    for (int i = 0; i < photos.length(); i++) {
                        try {
                            JSONObject photo = photos.getJSONObject(i);

                            Date createdTime = FBUtils.parseDate(photo.getString("created_time"));
                            lastPhotoIsTooOld = createdTime.compareTo(oldestPhotoDate) < 0;

                            if (!lastPhotoIsTooOld) {
                                String link = photo.getString("link");
                                JSONObject chosenImage = FBPageUploadedImagesFoodPhotosSupplier.chooseBestFbPhotoImage(photo);
                                linkToImage.put(link, chosenImage);
                            }
                        } catch (JSONException | ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (lastPhotoIsTooOld || requestCounter == REQUESTS_LIMIT) {
                break;
            } else {
                GraphRequest nextPage = response.getRequestForPagedResults(GraphResponse.PagingDirection.NEXT);
                if (nextPage != null) {
                    requestCounter += 1;
                    response = nextPage.executeAndWait();
                }
            }
        }

        return linkToImage;
    }

    private GraphRequest createGetPhotosRequest(String menzaFbId) {
        GraphRequest request = new GraphRequest(
            AccessToken.getCurrentAccessToken(),
            "/" + menzaFbId + "/photos?type=uploaded&limit=30",
            null,
            HttpMethod.GET
        );
        Bundle params = new Bundle();
        params.putString("fields", "created_time, images, link");
        request.setParameters(params);
        return request;
    }
}

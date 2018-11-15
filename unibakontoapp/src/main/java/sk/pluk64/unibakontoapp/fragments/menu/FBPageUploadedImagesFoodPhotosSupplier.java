package sk.pluk64.unibakontoapp.fragments.menu;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.facebook.AccessToken;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import sk.pluk64.unibakonto.Util;
import sk.pluk64.unibakontoapp.DateUtils;
import sk.pluk64.unibakontoapp.FBUtils;
import sk.pluk64.unibakontoapp.Utils;
import sk.pluk64.unibakontoapp.meals.Menza;

public class FBPageUploadedImagesFoodPhotosSupplier implements FoodPhotosSupplier {
    private final Menza menza;

    public FBPageUploadedImagesFoodPhotosSupplier(Menza menza) {
        this.menza = menza;
    }

    @Override
    public List<FBPhoto> getPhotos() throws Utils.FBAuthenticationException, Util.ConnectionFailedException {
        GraphRequest getPhotosRequest = createGetPhotosRequest(menza.getFbId());

        GraphResponse photosResponse = getPhotosRequest.executeAndWait();

        return processPhotosResponse(photosResponse);
    }

    private GraphRequest createGetPhotosRequest(final String menzaFBid) {
        GraphRequest request = new GraphRequest(
            AccessToken.getCurrentAccessToken(),
            "/" + menzaFBid + "/photos?type=uploaded&limit=30",
            null,
            HttpMethod.GET
        );
        Bundle params = new Bundle();
        params.putString("fields", "created_time, images, name");
        request.setParameters(params);
        return request;
    }

    private List<FBPhoto> processPhotosResponse(GraphResponse response) throws Utils.FBAuthenticationException, Util.ConnectionFailedException {
        ArrayList<FBPhoto> result = new ArrayList<>();

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
                            lastPhotoIsTooOld = !DateUtils.isAtMostXHoursOld(createdTime, 20);

                            if (!lastPhotoIsTooOld) {
                                JSONObject chosenImage = chooseBestFbPhotoImage(photo);
                                result.add(new FBPhoto()
                                    .setSource(chosenImage.getString("source"))
                                    .setWidth(chosenImage.getInt("width"))
                                    .setHeight(chosenImage.getInt("height"))
                                    .setCreatedTime(createdTime)
                                    .setCaption(photo.optString("name"))
                                );
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

        for (int i = 0; i < result.size(); i++) {
            result.get(i).setSeqNo(i);
        }
        Collections.sort(result, new Comparator<FBPhoto>() {
            @Override
            public int compare(FBPhoto o1, FBPhoto o2) {
                int compDates;
                if (o1.getCreatedTime() == null && o2.getCreatedTime() == null) {
                    compDates = 0;
                } else if (o1.getCreatedTime() == null) {
                    compDates = 1;
                } else if (o2.getCreatedTime() == null) {
                    compDates = -1;
                } else {
                    compDates = o1.getCreatedTime().compareTo(o2.getCreatedTime());
                }

                if (compDates == 0) {
                    return -compareInts(o1.getSeqNo(), o2.getSeqNo());
                } else {
                    return compDates;
                }
            }
        });
        return result;
    }

    private int compareInts(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    @NonNull
    static JSONObject chooseBestFbPhotoImage(JSONObject photo) throws JSONException {
        JSONArray images = photo.getJSONArray("images");

        int maxWidth = 999999;
        int bestWidth = 0;
        JSONObject chosenImage = null;

        for (int j = 0; j < images.length(); j++) {
            try {
                JSONObject image = images.getJSONObject(j);
                int width = image.getInt("width");
                if (bestWidth == 0 || (width > bestWidth && width <= maxWidth) || (bestWidth > maxWidth && width < bestWidth)) {
                    bestWidth = width;
                    chosenImage = image;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // photos widths: 480, 960, 800, 640, 426, 720, 320, 405, 130, 168, 2048, 1440, 173, 300, 1080, 960,

        if (chosenImage == null) {
            throw new JSONException("");
        }
        return chosenImage;
    }
}

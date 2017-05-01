package sk.pluk64.unibakonto.fragments.menu;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.facebook.AccessToken;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import sk.pluk64.unibakonto.FBUtils;
import sk.pluk64.unibakonto.Utils;
import sk.pluk64.unibakonto.meals.Menza;

public class FBPageUploadedImagesFoodPhotosSupplier implements FoodPhotosSupplier {
    private final Menza jedalen;

    public FBPageUploadedImagesFoodPhotosSupplier(Menza jedalen) {
        this.jedalen = jedalen;
    }

    @Override
    public List<FBPhoto> getPhotos() {
        GraphRequest getPhotosRequest = createGetPhotosRequest(jedalen.getFBid());

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
        params.putString("fields", "id, created_time, images, name");
        request.setParameters(params);
        return request;
    }

    private List<FBPhoto> processPhotosResponse(GraphResponse response) {
        ArrayList<FBPhoto> result = new ArrayList<>();

        boolean lastPhotoIsTooOld = false;
        int REQUESTS_LIMIT = 4;
        int requestCounter = 1;
        while (true) {
            FacebookRequestError error = response.getError();
            JSONObject responseJSONObject = response.getJSONObject();

            if (error != null) {
                return null;
            } else if (responseJSONObject != null) {
                try {
                    JSONArray photos = responseJSONObject.getJSONArray("data");
                    for (int i = 0; i < photos.length(); i++) {
                        JSONObject photo = photos.getJSONObject(i);

                        Date createdTime = FBUtils.parseDate(photo.optString("created_time"));
                        lastPhotoIsTooOld = !Utils.isToday(createdTime);

                        if (!lastPhotoIsTooOld) {
                            JSONObject chosenSource = chooseFBPhotoSource(photo);
                            if (chosenSource != null) {
                                result.add(new FBPhoto()
                                    .setID(photo.getString("id"))
                                    .setSource(chosenSource.getString("source"))
                                    .setWidth(chosenSource.optInt("width"))
                                    .setHeight(chosenSource.optInt("height"))
                                    .setCreatedTime(createdTime)
                                    .setCaption(photo.optString("name"))
                                );
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
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

    @Nullable
    private JSONObject chooseFBPhotoSource(JSONObject photo) throws JSONException {
        JSONArray images = photo.getJSONArray("images");

        int maxWidth = 800;
        int bestWidth = 0;
        JSONObject chosenSource = null;

        for (int j = 0; j < images.length(); j++) {
            JSONObject image = images.getJSONObject(j);
            int width = image.getInt("width");
            if (bestWidth == 0 || (width > bestWidth && width <= maxWidth) || (bestWidth > maxWidth && width < bestWidth)) {
                bestWidth = width;
                chosenSource = image;
            }
        }
        // photos widths: 480, 960, 800, 640, 426, 720, 320, 405, 130, 168, 2048, 1440, 173, 300, 1080, 960,
        return chosenSource;
    }
}

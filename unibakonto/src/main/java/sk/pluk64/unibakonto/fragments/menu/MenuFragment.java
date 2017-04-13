package sk.pluk64.unibakonto.fragments.menu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookException;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import sk.pluk64.unibakonto.R;
import sk.pluk64.unibakonto.UpdateMenusListener;
import sk.pluk64.unibakonto.Utils;
import sk.pluk64.unibakonto.http.Util;
import sk.pluk64.unibakonto.meals.Meals;
import sk.pluk64.unibakonto.meals.Menza;

public class MenuFragment extends Fragment {
    private static final String ARG_JEDALEN = "jedalen";
    private static final String PREF_MEALS = "meals";
    private static final String PREF_MEALS_PHOTOS = "meals_photos";
    //    private static final String PREF_MEALS_REFRESH_TIMESTAMP = "meals_refreshed_timestamp"; // DO NOT USE - could contain old data (string)
    private static final String PREF_MEALS_REFRESH_TIMESTAMP = "meals_refreshed_timestamp_date";
    private Menza jedalen;
    private MenuListAdapter adapter = new MenuListAdapter(this);
    private SwipeRefreshLayout swipeRefresh;
    private SharedPreferences preferences;
    private AsyncTask<Void, Void, Meals> updateDataTask;
    private Date refreshTime;
    CallbackManager fbCallbackManager;

    public MenuFragment() {
    }

    public static MenuFragment newInstance(Menza jedalen) {
        MenuFragment f = new MenuFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_JEDALEN, jedalen);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        adapter.setUpdateMenusListener((UpdateMenusListener) activity);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            jedalen = (Menza) getArguments().getSerializable(ARG_JEDALEN);
        }
        preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        fbCallbackManager = CallbackManager.Factory.create();
    }

    public void updateData() {
        if (updateDataTask != null) {
            return;
        }
        View view = getView();
        if (view != null) {
            setRefreshing(view);
        }
        updateDataTask = new AsyncTask<Void, Void, Meals>() {
            private List<FBPhoto> photos;
            private boolean needAuthenticate = false;

            @Override
            protected Meals doInBackground(Void... params) {
                // TODO otestovat, co sa stane ak je FB nedostupny.
                // jedalne listky by sa mali stiahnut aj bez FB

                GraphRequest getPhotosRequest = createGetPhotosRequest(jedalen.getFBid());
                try {
                    GraphResponse photosResponse = getPhotosRequest.executeAndWait();
                    photos = processPhotosResponse(photosResponse);
                    if (photos == null) {
                        needAuthenticate = true;
                    }
                } catch (FacebookException e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.showNoInternetConnection(getActivity().getApplicationContext());
                        }
                    });
                }

                try {
                    return jedalen.getMenu();
                } catch (Util.ConnectionFailedException e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.showNoInternetConnection(getActivity().getApplicationContext());
                        }
                    });
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Meals meals) {
                if (needAuthenticate) {
                    saveData(photos);
                    adapter.showFBButton();
                }
                if (photos != null) {
                    saveData(photos);
                    adapter.updatePhotos(photos);
                }
                if (meals != null) {
                    saveData(meals);
                    adapter.updateMeals(meals);
                }

                View view = getView();
                if (view != null) {
                    updateRefreshTime(view);
                }
                swipeRefresh.setRefreshing(false);
                updateDataTask = null;
            }
        };
        updateDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setRefreshing(View view) {
        TextView timestamp = (TextView) view.findViewById(R.id.refresh_timestamp);
        timestamp.setText(getString(R.string.refreshing));
    }

    private void updateRefreshTime(View view) {
        TextView timestamp = (TextView) view.findViewById(R.id.refresh_timestamp);
        String refreshTimeFormatted = Utils.getTimeFormatted(refreshTime, getString(R.string.never));
        timestamp.setText(getString(R.string.refreshed, refreshTimeFormatted));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        RecyclerView listView = (RecyclerView) view.findViewById(R.id.menu_list);

        listView.setHasFixedSize(true);
        RecyclerView.LayoutManager tLayoutManager = new LinearLayoutManager(getContext());
        listView.setLayoutManager(tLayoutManager);
        listView.setAdapter(adapter);

        loadData();
        updateRefreshTime(view);

        swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateData();
            }
        });
        return view;
    }


    private List<FBPhoto> processPhotosResponse(GraphResponse response) {
        ArrayList<FBPhoto> result = new ArrayList<>();

        boolean foundTooOldPhoto = false;
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

                        Date createdTime = parseDate(photo.optString("created_time"));
                        if (!FBPhoto.isToday(createdTime)) {
                            foundTooOldPhoto = true;
                            continue;
                        }

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
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (foundTooOldPhoto || requestCounter == REQUESTS_LIMIT) {
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

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");

    private Date parseDate(String fbTime) {
        try {
            return dateFormatter.parse(fbTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
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

    GraphRequest createGetPhotosRequest(final String menzaFBid) {
        GraphRequest request = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + menzaFBid + "/photos?type=uploaded&limit=25",
                null,
                HttpMethod.GET);
        Bundle params = new Bundle();
        params.putString("fields", "id, created_time, images, name");
        request.setParameters(params);
        return request;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fbCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Utils.isTimeDiffMoreThanXHours(refreshTime, 1)) {
            swipeRefresh.post(new Runnable() {
                @Override
                public void run() {
                    updateData();
                }
            });
        }
    }

    private void saveData(Meals meals) {
        Gson gson = new Gson();
        String jsonMeals = gson.toJson(meals);
        refreshTime = Utils.getCurrentTime();
        String jsonCurrentTime = gson.toJson(refreshTime);
        preferences.edit()
                .putString(PREF_MEALS + jedalen, jsonMeals)
                .putString(PREF_MEALS_REFRESH_TIMESTAMP + jedalen, jsonCurrentTime)
                .apply();
    }

    private void loadData() {
        Gson gson = new Gson();

        String jsonMeals = preferences.getString(PREF_MEALS + jedalen, "null");
        Meals meals = gson.fromJson(jsonMeals, Meals.class);
        adapter.updateMeals(meals);

        String jsonPhotos = preferences.getString(PREF_MEALS_PHOTOS + jedalen, "null");
        List<FBPhoto> photos = gson.fromJson(jsonPhotos, new TypeToken<List<FBPhoto>>() {
        }.getType());
        adapter.updatePhotos(photos);

        String jsonRefreshTime = preferences.getString(PREF_MEALS_REFRESH_TIMESTAMP + jedalen, "null");
        refreshTime = gson.fromJson(jsonRefreshTime, new TypeToken<Date>() {
        }.getType());
    }

    private void saveData(List<FBPhoto> photosWithSources) {
        Gson gson = new Gson();
        String jsonPhotos = gson.toJson(photosWithSources);
        preferences.edit()
                .putString(PREF_MEALS_PHOTOS + jedalen, jsonPhotos)
                .apply();
    }
}

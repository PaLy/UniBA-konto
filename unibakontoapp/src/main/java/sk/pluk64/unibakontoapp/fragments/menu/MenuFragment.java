package sk.pluk64.unibakontoapp.fragments.menu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Date;
import java.util.List;

import sk.pluk64.unibakontoapp.R;
import sk.pluk64.unibakontoapp.UpdateMenusListener;
import sk.pluk64.unibakontoapp.Utils;
import sk.pluk64.unibakontoapp.meals.Meals;
import sk.pluk64.unibakontoapp.meals.Menza;

public class MenuFragment extends Fragment {
    private static final String ARG_JEDALEN = "jedalen";
    private static final String PREF_MEALS = "meals";
    private static final String PREF_MEALS_PHOTOS = "meals_photos";
    //    private static final String PREF_MEALS_REFRESH_TIMESTAMP = "meals_refreshed_timestamp"; // DO NOT USE - could contain old data (string)
    private static final String PREF_MEALS_REFRESH_TIMESTAMP = "meals_refreshed_timestamp_date";
    private Menza jedalen;
    private final MenuListAdapter adapter = new MenuListAdapter(this);
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            jedalen = (Menza) getArguments().getSerializable(ARG_JEDALEN);
        }
        FragmentActivity activity = getActivity();
        if (activity != null) {
            preferences = activity.getPreferences(Context.MODE_PRIVATE);
        }
        fbCallbackManager = CallbackManager.Factory.create();
        adapter.setUpdateMenusListener((UpdateMenusListener) getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (updateDataTask != null) {
            updateDataTask.cancel(true);
        }
    }

    public void updateData() {
        if (updateDataTask != null) {
            return;
        }
        View view = getView();
        if (view != null) {
            setRefreshing(view);
        }
        updateDataTask = new UpdateMenuDataTask(jedalen, getActivity(), this);
        updateDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setRefreshing(View view) {
        TextView timestamp = view.findViewById(R.id.refresh_timestamp);
        timestamp.setText(getString(R.string.refreshing));
    }

    private void updateRefreshTime(View view) {
        TextView timestamp = view.findViewById(R.id.refresh_timestamp);
        String refreshTimeFormatted = Utils.getTimeFormatted(refreshTime, getString(R.string.never));
        timestamp.setText(getString(R.string.refreshed, refreshTimeFormatted));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        RecyclerView listView = view.findViewById(R.id.menu_list);

        listView.setHasFixedSize(true);
        RecyclerView.LayoutManager tLayoutManager = new LinearLayoutManager(getContext());
        listView.setLayoutManager(tLayoutManager);
        listView.setAdapter(adapter);

        loadData();
        updateRefreshTime(view);

        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateData();
            }
        });
        return view;
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

    public void onUpdateTaskFinished(boolean needAuthenticate, Meals meals, List<FBPhoto> photos) {
        if (needAuthenticate) {
            saveData(photos);
            adapter.showFBButton();
        } else if (photos != null) {
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

    public void onUpdateTaskCancelled() {
        updateDataTask = null;
    }
}

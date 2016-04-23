package sk.pluk64.unibakonto;

import android.content.Context;
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

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import sk.pluk64.unibakonto.http.JedalneListky;
import sk.pluk64.unibakonto.http.Util;

public class MenuFragment extends Fragment {
    private static final String ARG_JEDALEN = "jedalen";
    private static final String PREF_MEALS = "meals";
    private static final String PREF_MEALS_REFRESH_TIMESTAMP = "meals_refreshed_timestamp";
    private JedalneListky.Jedalne jedalen;
    private MenuListAdapter adapter = new MenuListAdapter();
    private SwipeRefreshLayout swipeRefresh;
    private boolean wasRefreshed = false;
    private SharedPreferences preferences;
    private AsyncTask<Void, Void, JedalneListky.Meals> updateDataTask;

    public MenuFragment() {
    }

    public static MenuFragment newInstance(JedalneListky.Jedalne jedalen) {
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
            jedalen = (JedalneListky.Jedalne) getArguments().getSerializable(ARG_JEDALEN);
        }
        preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
    }

    private void updateData() {
        if (updateDataTask != null) {
            return;
        }
        View view = getView();
        if (view != null) {
            setRefreshing(view);
        }
        updateDataTask = new AsyncTask<Void, Void, JedalneListky.Meals>() {
            @Override
            protected JedalneListky.Meals doInBackground(Void... params) {
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
            protected void onPostExecute(JedalneListky.Meals meals) {
                if (meals != null) {
                    saveData(meals);
                    wasRefreshed = true;
                    adapter.updateData(meals);
                    adapter.notifyDataSetChanged();
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
        String refreshTime = preferences.getString(PREF_MEALS_REFRESH_TIMESTAMP + jedalen, getString(R.string.never));
        timestamp.setText(getString(R.string.refreshed, refreshTime));
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
        if (!wasRefreshed) {
            swipeRefresh.post(new Runnable() {
                @Override
                public void run() {
                    updateData();
                }
            });
        }
        return view;
    }

    private void saveData(JedalneListky.Meals meals) {
        String jsonMeals = new Gson().toJson(meals);
        String formattedTime = Utils.getCurrentTimeFormatted();
        preferences.edit()
                .putString(PREF_MEALS + jedalen, jsonMeals)
                .putString(PREF_MEALS_REFRESH_TIMESTAMP + jedalen, formattedTime)
                .apply();
    }

    private void loadData() {
        String jsonMeals = preferences.getString(PREF_MEALS + jedalen, "null");
        JedalneListky.Meals meals = new Gson().fromJson(jsonMeals, JedalneListky.Meals.class);
        if (meals != null) {
            adapter.updateData(meals);
            adapter.notifyDataSetChanged();
        }
    }

    private static class MenuListAdapter extends RecyclerView.Adapter<MenuListAdapter.ViewHolder> {
        private enum ViewType {
            DAY_NAME(0), SUBMENU_NAME(1), MEAL(2);

            final int id;

            ViewType(int id) {
                this.id = id;
            }
        }

        private int itemCount = 0;
        private final Map<Integer, Object> positionToItem = new HashMap<>();
        private final Map<Integer, ViewType> positionToViewType = new HashMap<>();

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public View view;

            public ViewHolder(View v) {
                super(v);
                view = v;
            }
        }

        public MenuListAdapter() {
        }

        public void updateData(JedalneListky.Meals meals) {
            int pos = 0;
            if (meals != null) {
                for (JedalneListky.Meals.DayMenu dayMenu : meals.menus) {
                    positionToItem.put(pos, dayMenu.dayName);
                    positionToViewType.put(pos, ViewType.DAY_NAME);
                    pos++;
                    for (JedalneListky.Meals.SubMenu subMenu : dayMenu.subMenus) {
                        positionToItem.put(pos, subMenu.name);
                        positionToViewType.put(pos, ViewType.SUBMENU_NAME);
                        pos++;
                        for (JedalneListky.Meals.Meal meal : subMenu.meals) {
                            positionToItem.put(pos, meal);
                            positionToViewType.put(pos, ViewType.MEAL);
                            pos++;
                        }
                    }
                }
            }
            itemCount = pos;
        }

        @Override
        public int getItemViewType(int position) {
            return positionToViewType.get(position).id;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == ViewType.DAY_NAME.id) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item_day, parent, false);
            } else if (viewType == ViewType.SUBMENU_NAME.id) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item_submenu, parent, false);
            } else {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item_meal, parent, false);
            }
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ViewType viewType = positionToViewType.get(position);
            if (viewType == ViewType.DAY_NAME) {
                TextView view = (TextView) holder.view;
                view.setText((CharSequence) positionToItem.get(position));
            } else if (viewType == ViewType.SUBMENU_NAME) {
                TextView view = (TextView) holder.view;
                view.setText((CharSequence) positionToItem.get(position));
            } else if (viewType == ViewType.MEAL) {
                View view = holder.view;
                JedalneListky.Meals.Meal meal = (JedalneListky.Meals.Meal) positionToItem.get(position);

                TextView nameView = (TextView) view.findViewById(R.id.meal_name);
                nameView.setText(meal.name);

                TextView costView = (TextView) view.findViewById(R.id.meal_cost);
                costView.setText(meal.price);
            }
        }

        @Override
        public int getItemCount() {
            return itemCount;
        }
    }
}

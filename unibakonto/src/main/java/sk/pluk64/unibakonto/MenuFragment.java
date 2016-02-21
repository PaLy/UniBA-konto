package sk.pluk64.unibakonto;

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

import java.util.HashMap;
import java.util.Map;

import sk.pluk64.unibakonto.http.JedalneListky;

public class MenuFragment extends Fragment {
    private static final String ARG_JEDALEN = "jedalen";
    private JedalneListky.Jedalne jedalen;
    private MenuListAdapter adapter = new MenuListAdapter();
    private SwipeRefreshLayout swipeRefresh;
    private boolean wasRefreshed = false;

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
    }

    private void updateData() {
        new AsyncTask<Void, Void, JedalneListky.Meals>() {
            @Override
            protected JedalneListky.Meals doInBackground(Void... params) {
                return jedalen.getMenu();
            }

            @Override
            protected void onPostExecute(JedalneListky.Meals meals) {
                wasRefreshed = true;
                adapter.updateData(meals);
                adapter.notifyDataSetChanged();
                swipeRefresh.setRefreshing(false);
            }
        }.execute();
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
                    swipeRefresh.setRefreshing(true);
                    updateData();
                }
            });
        }
        return view;
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

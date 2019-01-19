package sk.pluk64.unibakontoapp.fragments.menu;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import sk.pluk64.unibakontoapp.DateUtils;
import sk.pluk64.unibakontoapp.R;
import sk.pluk64.unibakontoapp.RefreshMenusListener;
import sk.pluk64.unibakontoapp.meals.Meals;

class MenuListAdapter extends RecyclerView.Adapter<MenuListAdapter.ViewHolder> {
    private static final int REFRESH_TIMESTAMP_POSITION = 0;
    private static final int FB_FEED_POSITION = 1;
    private final MenuFragment menuFragment;
    private final MenuImagesAdapter menuImagesAdapter;
    private RefreshMenusListener refreshMenusListener;

    private enum ViewType {
        DAY_NAME(0), SUBMENU_NAME(1), MEAL(2), GALLERY(3), FB_LOGIN(4), NO_GALLERY_IMAGES(5), REFRESH_TIMESTAMP(6);

        final int id;

        ViewType(int id) {
            this.id = id;
        }
    }

    private int itemCount = 0;
    private final SparseArray<Object> positionToItem = new SparseArray<>();
    private final SparseArray<ViewType> positionToViewType = new SparseArray<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;

        public ViewHolder(View v) {
            super(v);
            view = v;
        }
    }

    public MenuListAdapter(MenuFragment menuFragment) {
        this.menuFragment = menuFragment;
        menuImagesAdapter = new MenuImagesAdapter(menuFragment);
        positionToViewType.put(REFRESH_TIMESTAMP_POSITION, ViewType.REFRESH_TIMESTAMP);
        positionToViewType.put(FB_FEED_POSITION, ViewType.FB_LOGIN);
        itemCount = 2;
    }

    public void setRefreshMenusListener(RefreshMenusListener refreshMenusListener) {
        this.refreshMenusListener = refreshMenusListener;
    }

    public void showFBButton() {
        positionToViewType.put(FB_FEED_POSITION, ViewType.FB_LOGIN);
        notifyDataSetChanged();
    }

    public void updatePhotos(List<FBPhoto> photos) {
        if (photos != null) {
            if (photos.isEmpty()) {
                positionToViewType.put(FB_FEED_POSITION, ViewType.NO_GALLERY_IMAGES);
            } else {
                positionToViewType.put(FB_FEED_POSITION, ViewType.GALLERY);
            }
            menuImagesAdapter.updateData(photos);
            notifyDataSetChanged();
        }
    }

    public void updateMeals(Meals meals) {
        int pos = 2;
        if (meals != null) {
            for (Meals.DayMenu dayMenu : meals.menus) {
                positionToItem.put(pos, dayMenu.dayName);
                positionToViewType.put(pos, ViewType.DAY_NAME);
                pos++;
                for (Meals.SubMenu subMenu : dayMenu.subMenus) {
                    positionToItem.put(pos, subMenu.name);
                    positionToViewType.put(pos, ViewType.SUBMENU_NAME);
                    pos++;
                    for (Meals.Meal meal : subMenu.meals) {
                        positionToItem.put(pos, meal);
                        positionToViewType.put(pos, ViewType.MEAL);
                        pos++;
                    }
                }
            }
        }
        itemCount = pos;
        notifyDataSetChanged();
    }

    void setRefreshing() {
        positionToItem.put(REFRESH_TIMESTAMP_POSITION, menuFragment.getString(R.string.refreshing));
        notifyDataSetChanged();
    }

    void updateRefreshTime(Date refreshTime) {
        String refreshTimeFormatted = DateUtils.getReadableTime(refreshTime, menuFragment.getString(R.string.never), menuFragment.getContext());
        positionToItem.put(REFRESH_TIMESTAMP_POSITION, menuFragment.getString(R.string.refreshed, refreshTimeFormatted));
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return positionToViewType.get(position).id;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view;
        if (viewType == ViewType.DAY_NAME.id) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item_day, parent, false);
        } else if (viewType == ViewType.SUBMENU_NAME.id) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item_submenu, parent, false);
        } else if (viewType == ViewType.MEAL.id) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item_meal, parent, false);
        } else if (viewType == ViewType.GALLERY.id) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fb_images, parent, false);

            RecyclerView recyclerView = view.findViewById(R.id.recycler);
            recyclerView.setHasFixedSize(false);
            RecyclerView.LayoutManager layoutManager = new GridLayoutManager(parent.getContext(), 1, LinearLayoutManager.HORIZONTAL, false);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(menuImagesAdapter);

            SnapHelper snapHelper = new GravitySnapHelper(Gravity.START);
            snapHelper.attachToRecyclerView(recyclerView);
        } else if (viewType == ViewType.NO_GALLERY_IMAGES.id) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.no_fb_images, parent, false);
        } else if (viewType == ViewType.FB_LOGIN.id) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fb_login_button, parent, false);

            LoginButton loginButton = view.findViewById(R.id.login_button);
            loginButton.setReadPermissions(Collections.<String>emptyList());
            loginButton.setFragment(menuFragment);
            loginButton.registerCallback(menuFragment.fbCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    if (refreshMenusListener != null) {
                        refreshMenusListener.refreshMenus();
                    } else {
                        menuFragment.refresh();
                    }
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onCancel() {
                    // TODO
                }

                @Override
                public void onError(FacebookException exception) {
                    // TODO
                }
            });
        } else if (viewType == ViewType.REFRESH_TIMESTAMP.id) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.refreshed_timestamp_menu, parent, false);
        } else {
            view = null;
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
            Meals.Meal meal = (Meals.Meal) positionToItem.get(position);

            TextView nameView = view.findViewById(R.id.meal_name);
            nameView.setText(meal.name);

            TextView costView = view.findViewById(R.id.meal_cost);
            costView.setText(meal.price);
        } else if (viewType == ViewType.FB_LOGIN) {
            holder.view.setVisibility(View.VISIBLE);
        } else if (viewType == ViewType.REFRESH_TIMESTAMP) {
            TextView view = (TextView) holder.view;
            view.setText((CharSequence) positionToItem.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }
}

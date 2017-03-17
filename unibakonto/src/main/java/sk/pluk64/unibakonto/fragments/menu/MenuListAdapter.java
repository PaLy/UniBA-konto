package sk.pluk64.unibakonto.fragments.menu;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.pluk64.unibakonto.R;
import sk.pluk64.unibakonto.UpdateMenusListener;
import sk.pluk64.unibakonto.meals.Meals;

class MenuListAdapter extends RecyclerView.Adapter<MenuListAdapter.ViewHolder> {
    private final MenuFragment menuFragment;
    private MenuImagesAdapter menuImagesAdapter;
    private UpdateMenusListener updateMenusListener;

    private enum ViewType {
        DAY_NAME(0), SUBMENU_NAME(1), MEAL(2), GALLERY(3), FB_LOGIN(4), NO_GALLERY_IMAGES(5);

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

    public MenuListAdapter(MenuFragment menuFragment) {
        this.menuFragment = menuFragment;
        menuImagesAdapter = new MenuImagesAdapter(menuFragment);
        positionToViewType.put(0, ViewType.FB_LOGIN);
        itemCount = 1;
    }

    public void setUpdateMenusListener(UpdateMenusListener updateMenusListener) {
        this.updateMenusListener = updateMenusListener;
    }

    public void showFBButton() {
        positionToViewType.put(0, ViewType.FB_LOGIN);
        notifyItemChanged(0);
    }

    public void updatePhotos(List<FBPhoto> photos) {
        if (photos != null) {
            photos = filterTodayPhotos(photos);
            if (photos.isEmpty()) {
                positionToViewType.put(0, ViewType.NO_GALLERY_IMAGES);
            } else {
                positionToViewType.put(0, ViewType.GALLERY);
            }
            menuImagesAdapter.updateData(photos);
            notifyItemChanged(0);
        }
    }

    private List<FBPhoto> filterTodayPhotos(List<FBPhoto> photos) {
        List<FBPhoto> result = new ArrayList<>();
        for (FBPhoto photo : photos) {
            if (photo.isCreatedToday()) {
                result.add(photo);
            }
        }
        return result;
    }

    public void updateMeals(Meals meals) {
        int pos = 1;
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

            RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler);
            recyclerView.setHasFixedSize(true);
            StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(menuImagesAdapter);
        } else if (viewType == ViewType.NO_GALLERY_IMAGES.id) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.no_fb_images, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fb_login_button, parent, false);

            LoginButton loginButton = (LoginButton) view.findViewById(R.id.login_button);
            loginButton.setReadPermissions(Collections.<String>emptyList());
            loginButton.setFragment(menuFragment);
            loginButton.registerCallback(menuFragment.fbCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    if (updateMenusListener != null) {
                        updateMenusListener.updateMenus();
                    } else {
                        menuFragment.updateData();
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

            TextView nameView = (TextView) view.findViewById(R.id.meal_name);
            nameView.setText(meal.name);

            TextView costView = (TextView) view.findViewById(R.id.meal_cost);
            costView.setText(meal.price);
        } else if (viewType == ViewType.FB_LOGIN) {
            holder.view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }
}

package sk.pluk64.unibakontoapp.fragments.menu;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;

import com.facebook.FacebookException;

import java.lang.ref.WeakReference;
import java.util.List;

import sk.pluk64.unibakonto.Util;
import sk.pluk64.unibakontoapp.R;
import sk.pluk64.unibakontoapp.Utils;
import sk.pluk64.unibakontoapp.meals.Meals;
import sk.pluk64.unibakontoapp.meals.Menza;

class UpdateMenuDataTask extends AsyncTask<Void, Void, Meals> {
    private final Menza jedalen;
    private final WeakReference<FragmentActivity> activityReference;
    private final WeakReference<MenuFragment> menuFragmentReference;
    private List<FBPhoto> photos;
    private boolean needAuthenticate = false;

    public UpdateMenuDataTask(Menza jedalen, FragmentActivity activity, MenuFragment menuFragment) {
        this.jedalen = jedalen;
        activityReference = new WeakReference<>(activity);
        menuFragmentReference = new WeakReference<>(menuFragment);
    }

    @Override
    protected Meals doInBackground(Void... params) {
        // TODO otestovat, co sa stane ak je FB nedostupny.
        // jedalne listky by sa mali stiahnut aj bez FB

        try {
            photos = new FBPageFeedFoodPhotosSupplier(jedalen).getPhotos();
            if (photos.isEmpty()) {
                photos = new FBPageUploadedImagesFoodPhotosSupplier(jedalen).getPhotos();
            }
        } catch (FacebookException e) {
            final FragmentActivity activity = activityReference.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.showToast(activity.getApplicationContext(), R.string.internal_error);
                    }
                });
            }
        } catch (Utils.FBAuthenticationException e) {
            needAuthenticate = true;
        } catch (Util.ConnectionFailedException e) {
            final FragmentActivity activity = activityReference.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.showNoInternetConnection(activity.getApplicationContext());
                    }
                });
            }
        }

        if (isCancelled()) {
            return null;
        }

        try {
            return jedalen.getMenu();
        } catch (Util.ConnectionFailedException e) {
            final FragmentActivity activity = activityReference.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.showNoInternetConnection(activity.getApplicationContext());
                    }
                });
            }
            return null;
        }
    }

    @Override
    protected void onPostExecute(Meals meals) {
        MenuFragment menuFragment = menuFragmentReference.get();
        if (menuFragment != null) {
            menuFragment.onUpdateTaskFinished(needAuthenticate, meals, photos);
        }
    }

    @Override
    protected void onCancelled(Meals meals) {
        MenuFragment menuFragment = menuFragmentReference.get();
        if (menuFragment != null) {
            menuFragment.onUpdateTaskCancelled();
        }
    }
}

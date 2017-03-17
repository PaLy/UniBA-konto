package sk.pluk64.unibakonto.fragments.menu;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import sk.pluk64.unibakonto.R;
import sk.pluk64.unibakonto.Utils;

public class MenuImagesAdapter extends RecyclerView.Adapter<MenuImagesAdapter.ViewHolder> {
    private final MenuFragment menuFragment;
    private int itemCount = 0;
    private final SparseArray<FBPhoto> positionToPhoto = new SparseArray<>();
    private final PicassoWrapper picassoWrapper;
    private int imageWidth;
    private int imageHeight;

    private static class PicassoWrapper {
        private Picasso picasso;
    }

    public MenuImagesAdapter(MenuFragment menuFragment) {
        this.menuFragment = menuFragment;
        picassoWrapper = new PicassoWrapper();
    }

    private Picasso getPicasso() {
        if (picassoWrapper.picasso == null) {
            picassoWrapper.picasso = Picasso.with(menuFragment.getContext());
            picassoWrapper.picasso.setIndicatorsEnabled(true);
        }
        return picassoWrapper.picasso;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fb_image, parent, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(imageWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        int marginPx = Utils.dpToPx(4);
        layoutParams.setMargins(marginPx, marginPx, marginPx, marginPx);
        view.setLayoutParams(layoutParams);

        ImageView imageView = (ImageView) view.findViewById(R.id.image);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(imageWidth, imageHeight));
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FBPhoto photo = positionToPhoto.get(position);
        if (photo != null) {
            ImageView imageView = (ImageView) holder.view.findViewById(R.id.image);
            getPicasso().load(photo.getSource()).into(imageView);

            TextView captionView = (TextView) holder.view.findViewById(R.id.text);
            if ("".equals(photo.getCaption())) {
                captionView.setVisibility(View.GONE);
            } else {
                captionView.setVisibility(View.VISIBLE);
                captionView.setText(photo.getCaption());
            }
        }
    }

    public void updateData(List<FBPhoto> photos) {
        positionToPhoto.clear();

        itemCount = photos.size();
        for (int i = 0; i < photos.size(); i++) {
            positionToPhoto.put(i, photos.get(i));
        }
        computeImagesWidthAndHeight(photos);
        notifyDataSetChanged();
    }

    private void computeImagesWidthAndHeight(List<FBPhoto> photos) {
        int maxWidth = Utils.getScreenWidth() - Utils.dpToPx(8) - Utils.dpToPx(8) - Utils.dpToPx(16);
        int maxPhotoWidth = 0;
        int maxPhotoHeight = 0;
        for (FBPhoto photo : photos) {
            int photoWidth = photo.getWidth();
            int photoHeight = photo.getHeight();
            if (photoWidth > maxWidth) {
                double scale = maxWidth / (double) photoWidth;
                photoHeight = (int) (photoHeight * scale);
                photoWidth = maxWidth;
            }

            if (photoWidth > maxPhotoWidth) {
                maxPhotoWidth = photoWidth;
                maxPhotoHeight = photoHeight;
            }
        }

        imageWidth = maxPhotoWidth;
        imageHeight = maxPhotoHeight;
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View view;

        public ViewHolder(View v) {
            super(v);
            view = v;
        }
    }
}

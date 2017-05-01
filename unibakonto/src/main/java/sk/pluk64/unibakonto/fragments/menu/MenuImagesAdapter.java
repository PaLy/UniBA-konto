package sk.pluk64.unibakonto.fragments.menu;

import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import sk.pluk64.unibakonto.R;
import sk.pluk64.unibakonto.Utils;

public class MenuImagesAdapter extends RecyclerView.Adapter<MenuImagesAdapter.ViewHolder> {
    private final MenuFragment menuFragment;
    private int itemCount = 0;
    private final PicassoWrapper picassoWrapper;
    private int imageWidth;
    private final SparseArray<Object> positionToItem = new SparseArray<>();
    private final SparseArray<ViewType> positionToViewType = new SparseArray<>();

    private enum ViewType {
        PHOTO_WITH_OPTIONAL_CAPTION(0), DAY(1), POST_MESSAGE(2);

        final int id;

        ViewType(int id) {
            this.id = id;
        }
    }

    private static class PicassoWrapper {
        private Picasso picasso;
    }

    public MenuImagesAdapter(MenuFragment menuFragment) {
        this.menuFragment = menuFragment;
        picassoWrapper = new PicassoWrapper();
        imageWidth = Utils.getScreenWidth() - Utils.dpToPx(8) - Utils.dpToPx(8) - Utils.dpToPx(16);
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
        View view = null;
        if (viewType == ViewType.PHOTO_WITH_OPTIONAL_CAPTION.id) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fb_image, parent, false);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(imageWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
            int marginPx = Utils.dpToPx(4);
            layoutParams.setMargins(marginPx, marginPx, marginPx, marginPx);
            view.setLayoutParams(layoutParams);

            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(imageWidth, LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView textView = (TextView) view.findViewById(R.id.text);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        } else if (viewType == ViewType.DAY.id) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_images_day, parent, false);
        } else if (viewType == ViewType.POST_MESSAGE.id) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_images_post_message, parent, false);
            TextView textView = (TextView) view.findViewById(R.id.text);
            // thanks to http://stackoverflow.com/questions/2734270/how-do-i-make-links-in-a-textview-clickable
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
        return new ViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        return positionToViewType.get(position).id;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        ViewType viewType = positionToViewType.get(position);
        if (viewType == ViewType.PHOTO_WITH_OPTIONAL_CAPTION) {
            FBPhoto photo = (FBPhoto) positionToItem.get(position);
            if (photo != null) {
                ImageView imageView = (ImageView) holder.view.findViewById(R.id.image);
                int photoWidth = photo.getWidth();
                double scale = photoWidth == 0 ? 0 : imageWidth / (double) photoWidth;
                final int imageHeight = (int) (photo.getHeight() * scale);
                getPicasso().load(photo.getSource()).resize(imageWidth, imageHeight).into(imageView);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(imageWidth, imageHeight));

                TextView captionView = (TextView) holder.view.findViewById(R.id.text);
                if ("".equals(photo.getCaption())) {
                    captionView.setVisibility(View.GONE);
                } else {
                    captionView.setVisibility(View.VISIBLE);
                    CharSequence textWithShortenedLinks = Utils.shortenLinks(photo.getCaption());
                    captionView.setText(textWithShortenedLinks);
                }
            }
        } else if (viewType == ViewType.DAY) {
            String dayText = (String) positionToItem.get(position);
            TextView view = (TextView) holder.view;
            view.setText(dayText);
        } else if (viewType == ViewType.POST_MESSAGE) {
            TextView textView = (TextView) holder.view.findViewById(R.id.text);
            FBPhoto photo = (FBPhoto) positionToItem.get(position);
            CharSequence textWithShortenedLinks = Utils.shortenLinks(photo.getCaption());
            textView.setText(textWithShortenedLinks);
        }
    }

    public void updateData(List<FBPhoto> photos) {
        positionToItem.clear();
        positionToViewType.clear();

        int pos = 0;
        String lastDay = "";
        for (int i = 0; i < photos.size(); i++) {
            FBPhoto photo = photos.get(i);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(photo.getCreatedTime());
            String day = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()).toUpperCase();
            day = day.replaceAll("(.)", "$1\n").trim();

            if (pos == 0 || !lastDay.equals(day)) {
                positionToItem.put(pos, day);
                positionToViewType.put(pos, ViewType.DAY);
                pos++;
                lastDay = day;
            }

            positionToItem.put(pos, photo);
            if (photo.getSource() == null) { // TODO lepsi check
                positionToViewType.put(pos, ViewType.POST_MESSAGE);
            } else {
                positionToViewType.put(pos, ViewType.PHOTO_WITH_OPTIONAL_CAPTION);
            }
            pos++;
        }
        itemCount = pos;
        notifyDataSetChanged();
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

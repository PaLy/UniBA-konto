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

public class MenuImagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final MenuFragment menuFragment;
    private int itemCount = 0;
    private final PicassoWrapper picassoWrapper;
    private final int imageWidth;
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
        }
        return picassoWrapper.picasso;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ViewType.PHOTO_WITH_OPTIONAL_CAPTION.id) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fb_image, parent, false);

            ImageView imageView = view.findViewById(R.id.image);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(imageWidth, LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView textView = view.findViewById(R.id.text);
            textView.setMovementMethod(LinkMovementMethod.getInstance());

            return new PhotoWithOptionalCaptionViewHolder(view, textView, imageView);
        } else if (viewType == ViewType.DAY.id) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_images_day, parent, false);
            TextView textView = view.findViewById(R.id.food_images_day);
            return new DayViewHolder(view, textView);
        } else if (viewType == ViewType.POST_MESSAGE.id) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_images_post_message, parent, false);
            TextView textView = view.findViewById(R.id.text);
            // thanks to http://stackoverflow.com/questions/2734270/how-do-i-make-links-in-a-textview-clickable
            textView.setMovementMethod(LinkMovementMethod.getInstance());

            return new PostMessageViewHolder(view, textView);
        } else {
            return null;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return positionToViewType.get(position).id;
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // TODO this is hack...
        if (holder instanceof PhotoWithOptionalCaptionViewHolder) {
            final TextView textView = ((PhotoWithOptionalCaptionViewHolder) holder).textView;
            textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.setLayoutParams(
                        new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            Utils.getViewHeightFromTextHeight(textView) + Utils.dpToPx(8)
                        )
                    );
                }
            });

            ImageView imageView = ((PhotoWithOptionalCaptionViewHolder) holder).imageView;
            int imageHeight = ((PhotoWithOptionalCaptionViewHolder) holder).imageHeight;
            if (imageHeight != -1) {
                imageView.setLayoutParams(new LinearLayout.LayoutParams(imageWidth, imageHeight));
            }
        } else if (holder instanceof PostMessageViewHolder) {
            final TextView textView = ((PostMessageViewHolder) holder).textView;

            textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.setLayoutParams(
                        new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            Utils.getViewHeightFromTextHeight(textView) + Utils.dpToPx(8)
                        )
                    );
                }
            });
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        ViewType viewType = positionToViewType.get(position);
        if (viewType == ViewType.PHOTO_WITH_OPTIONAL_CAPTION) {
            if (!(holder instanceof PhotoWithOptionalCaptionViewHolder)) return;
            PhotoWithOptionalCaptionViewHolder h = (PhotoWithOptionalCaptionViewHolder) holder;

            FBPhoto photo = (FBPhoto) positionToItem.get(position);
            if (photo != null) {
                int photoWidth = photo.getWidth();
                double scale = photoWidth == 0 ? 0 : imageWidth / (double) photoWidth;
                final int imageHeight = (int) (photo.getHeight() * scale);
                getPicasso().load(photo.getSource()).resize(imageWidth, imageHeight).into(h.imageView);
                h.imageView.setLayoutParams(new LinearLayout.LayoutParams(imageWidth, imageHeight));
                h.imageHeight = imageHeight;

                if ("".equals(photo.getCaption())) {
                    h.textView.setText("");
                    h.textView.setVisibility(View.GONE);
                } else {
                    h.textView.setVisibility(View.VISIBLE);
                    CharSequence textWithShortenedLinks = Utils.shortenLinks(photo.getCaption());
                    h.textView.setText(textWithShortenedLinks);
                }
            }
        } else if (viewType == ViewType.DAY) {
            if (!(holder instanceof DayViewHolder)) return;
            DayViewHolder h = (DayViewHolder) holder;

            String dayText = (String) positionToItem.get(position);
            h.textView.setText(dayText);
        } else if (viewType == ViewType.POST_MESSAGE) {
            if (!(holder instanceof PostMessageViewHolder)) return;
            PostMessageViewHolder h = (PostMessageViewHolder) holder;

            FBPhoto photo = (FBPhoto) positionToItem.get(position);
            CharSequence textWithShortenedLinks = Utils.shortenLinks(photo.getCaption());
            h.textView.setText(textWithShortenedLinks);
        }
    }

    public void updateData(List<FBPhoto> photos) {
        positionToItem.clear();
        positionToViewType.clear();

        int pos = 0;
        String lastDay = "";
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < photos.size(); i++) {
            FBPhoto photo = photos.get(i);

            String day;
            if (Utils.isToday(photo.getCreatedTime())) {
                day = menuFragment.getString(R.string.today);
            } else if (Utils.isYesterday(photo.getCreatedTime())) {
                day = menuFragment.getString(R.string.yesterday);
            } else {
                calendar.setTime(photo.getCreatedTime());
                day = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
            }
            day = day.toUpperCase().replaceAll("(.)", "$1\n").trim();

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

    private static class PhotoWithOptionalCaptionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView textView;
        private int imageHeight = -1;

        PhotoWithOptionalCaptionViewHolder(View view, TextView textView, ImageView imageView) {
            super(view);
            this.imageView = imageView;
            this.textView = textView;
        }
    }

    private static class DayViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        DayViewHolder(View view, TextView textView) {
            super(view);
            this.textView = textView;
        }
    }

    private static class PostMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        PostMessageViewHolder(View view, TextView textView) {
            super(view);
            this.textView = textView;
        }
    }
}

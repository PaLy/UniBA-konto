package sk.pluk64.unibakontoapp.fragments.menu

import androidx.recyclerview.widget.RecyclerView
import android.text.method.LinkMovementMethod
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.squareup.picasso.Picasso

import java.util.Calendar
import java.util.Locale

import sk.pluk64.unibakontoapp.DateUtils
import sk.pluk64.unibakontoapp.R
import sk.pluk64.unibakontoapp.Utils

class MenuImagesAdapter(private val menuFragment: MenuFragment) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var itemCount = 0
    private val imageWidth: Int = Utils.screenWidth - Utils.dpToPx(8) - Utils.dpToPx(8) - Utils.dpToPx(16)
    private val positionToItem = SparseArray<Any>()
    private val positionToViewType = SparseArray<ViewType>()

    private val picasso by lazy {
        Picasso.with(menuFragment.context)
    }

    private enum class ViewType(internal val id: Int) {
        PHOTO_WITH_OPTIONAL_CAPTION(0), DAY(1), POST_MESSAGE(2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PHOTO_WITH_OPTIONAL_CAPTION.id -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.fb_image, parent, false)

                val imageView = view.findViewById<ImageView>(R.id.image)
                imageView.layoutParams = LinearLayout.LayoutParams(imageWidth, LinearLayout.LayoutParams.WRAP_CONTENT)

                val textView = view.findViewById<TextView>(R.id.text)
                textView.movementMethod = LinkMovementMethod.getInstance()

                PhotoWithOptionalCaptionViewHolder(view, textView, imageView)
            }
            ViewType.DAY.id -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.food_images_day, parent, false)
                val textView = view.findViewById<TextView>(R.id.food_images_day)
                DayViewHolder(view, textView)
            }
            ViewType.POST_MESSAGE.id -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.food_images_post_message, parent, false)
                val textView = view.findViewById<TextView>(R.id.text)
                // thanks to http://stackoverflow.com/questions/2734270/how-do-i-make-links-in-a-textview-clickable
                textView.movementMethod = LinkMovementMethod.getInstance()

                PostMessageViewHolder(view, textView)
            }
            else -> throw Throwable()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return positionToViewType.get(position).id
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        // TODO this is hack...
        if (holder is PhotoWithOptionalCaptionViewHolder) {
            val textView = holder.textView
            textView.post {
                textView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Utils.getViewHeightFromTextHeight(textView) + Utils.dpToPx(8)
                )
            }

            val imageView = holder.imageView
            val imageHeight = holder.imageHeight
            if (imageHeight != -1) {
                imageView.layoutParams = LinearLayout.LayoutParams(imageWidth, imageHeight)
            }
        } else if (holder is PostMessageViewHolder) {
            val textView = holder.textView

            textView.post {
                textView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Utils.getViewHeightFromTextHeight(textView) + Utils.dpToPx(8)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewType = positionToViewType.get(position)
        if (viewType == ViewType.PHOTO_WITH_OPTIONAL_CAPTION) {
            if (holder !is PhotoWithOptionalCaptionViewHolder) return

            val photo = positionToItem.get(position) as FBPhoto
            val photoWidth = photo.width
            val scale = if (photoWidth == 0) 0.0 else imageWidth / photoWidth.toDouble()
            val imageHeight = (photo.height * scale).toInt()
            picasso!!.load(photo.source).resize(imageWidth, imageHeight).into(holder.imageView)
            holder.imageView.layoutParams = LinearLayout.LayoutParams(imageWidth, imageHeight)
            holder.imageHeight = imageHeight

            if ("" == photo.caption) {
                holder.textView.text = ""
                holder.textView.visibility = View.GONE
            } else {
                holder.textView.visibility = View.VISIBLE
                val textWithShortenedLinks = Utils.shortenLinks(photo.caption)
                holder.textView.text = textWithShortenedLinks
            }
        } else if (viewType == ViewType.DAY) {
            if (holder !is DayViewHolder) return

            val dayText = positionToItem.get(position) as String
            holder.textView.text = dayText
        } else if (viewType == ViewType.POST_MESSAGE) {
            if (holder !is PostMessageViewHolder) return

            val (_, _, _, _, caption) = positionToItem.get(position) as FBPhoto
            val textWithShortenedLinks = Utils.shortenLinks(caption)
            holder.textView.text = textWithShortenedLinks
        }
    }

    fun updateData(photos: List<FBPhoto>) {
        positionToItem.clear()
        positionToViewType.clear()

        var pos = 0
        var lastDay = ""
        val calendar = Calendar.getInstance()
        for (photo in photos) {
            val day = when {
                DateUtils.isToday(photo.createdTime) -> menuFragment.getString(R.string.today)
                DateUtils.isYesterday(photo.createdTime) -> menuFragment.getString(R.string.yesterday)
                else -> {
                    calendar.time = photo.createdTime!!
                    calendar.getDisplayName(
                        Calendar.DAY_OF_WEEK,
                        Calendar.LONG,
                        Locale.getDefault()
                    )
                }
            }.toUpperCase()
                .replace("(.)".toRegex(), "$1\n")
                .trim()

            if (pos == 0 || lastDay != day) {
                positionToItem.put(pos, day)
                positionToViewType.put(pos, ViewType.DAY)
                pos++
                lastDay = day
            }

            positionToItem.put(pos, photo)
            if (photo.source.isEmpty()) {
                positionToViewType.put(pos, ViewType.POST_MESSAGE)
            } else {
                positionToViewType.put(pos, ViewType.PHOTO_WITH_OPTIONAL_CAPTION)
            }
            pos++
        }
        itemCount = pos
        notifyDataSetChanged()
    }

    override fun getItemCount() = itemCount

    private class PhotoWithOptionalCaptionViewHolder internal constructor(view: View, val textView: TextView, val imageView: ImageView) : RecyclerView.ViewHolder(view) {
        var imageHeight = -1
    }

    private class DayViewHolder internal constructor(view: View, val textView: TextView) : RecyclerView.ViewHolder(view)

    private class PostMessageViewHolder internal constructor(view: View, val textView: TextView) : RecyclerView.ViewHolder(view)
}

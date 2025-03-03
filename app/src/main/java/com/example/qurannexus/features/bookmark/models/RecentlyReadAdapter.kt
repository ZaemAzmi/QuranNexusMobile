package com.example.qurannexus.features.bookmark.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.customViews.AutoFitTextView
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.features.bookmark.enums.RecentlyReadType
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

// RecentlyReadAdapter.kt
class RecentlyReadAdapter(
    private var currentType: RecentlyReadType,
    private val onItemClick: (RecentlyRead, RecentlyReadType) -> Unit,
    private val onDeleteClick: (RecentlyRead, RecentlyReadType) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<RecentlyRead> = emptyList()

    // Define view type constants
    companion object {
        private const val VIEW_TYPE_CHAPTER = 1
        private const val VIEW_TYPE_PAGE = 2
        private const val VIEW_TYPE_JUZ = 3
    }

    class ChapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val number: AutoFitTextView = view.findViewById(R.id.recentlyReadSurahNumber)
        val title: TextView = view.findViewById(R.id.recentlyReadSurahTitle)
        val date: TextView = view.findViewById(R.id.recentlyReadDate)
        val time: TextView = view.findViewById(R.id.recentlyReadTime)
        val menu: ImageView = view.findViewById(R.id.recentlyReadMenu)
        val info: TextView = view.findViewById(R.id.recentlyReadSurahInfo)
        val card: CardView = view.findViewById(R.id.recentlyReadSurahCard)
    }

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val number: AutoFitTextView = view.findViewById(R.id.recentlyReadPageNumber)
        val title: TextView = view.findViewById(R.id.recentlyReadPageTitle)
        val surahInfo: TextView = view.findViewById(R.id.recentlyReadPageSurahInfo)
        val date: TextView = view.findViewById(R.id.recentlyReadPageDate)
        val time: TextView = view.findViewById(R.id.recentlyReadPageTime)
        val menu: ImageView = view.findViewById(R.id.recentlyReadPageMenu)
        val juzInfo: TextView = view.findViewById(R.id.recentlyReadPageJuzInfo)
        val card: CardView = view.findViewById(R.id.recentlyReadPageCard)
    }

    class JuzViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val number: AutoFitTextView = view.findViewById(R.id.recentlyReadJuzNumber)
        val title: TextView = view.findViewById(R.id.recentlyReadJuzTitle)
        val date: TextView = view.findViewById(R.id.recentlyReadJuzDate)
        val time: TextView = view.findViewById(R.id.recentlyReadJuzTime)
        val menu: ImageView = view.findViewById(R.id.recentlyReadJuzMenu)
        val pageInfo: TextView = view.findViewById(R.id.recentlyReadJuzPageInfo)
        val card: CardView = view.findViewById(R.id.recentlyReadJuzCard)
    }

    // Override getItemViewType to return the correct view type based on currentType
    override fun getItemViewType(position: Int): Int {
        return when (currentType) {
            RecentlyReadType.CHAPTER -> VIEW_TYPE_CHAPTER
            RecentlyReadType.PAGE -> VIEW_TYPE_PAGE
            RecentlyReadType.JUZ -> VIEW_TYPE_JUZ
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CHAPTER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_item_recently_read_surah, parent, false)
                ChapterViewHolder(view)
            }
            VIEW_TYPE_PAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_item_recently_read_page, parent, false)
                PageViewHolder(view)
            }
            VIEW_TYPE_JUZ -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_item_recently_read_juz, parent, false)
                JuzViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        when (holder) {
            is ChapterViewHolder -> bindChapterView(holder, item)
            is PageViewHolder -> bindPageView(holder, item)
            is JuzViewHolder -> bindJuzView(holder, item)
        }
    }

    private fun bindChapterView(holder: ChapterViewHolder, item: RecentlyRead) {
        val surahDetails = QuranMetadata.getInstance().getSurahDetails(item.itemId.toInt())

        holder.apply {
            number.text = item.itemId
            title.text = "${surahDetails?.englishName} (${surahDetails?.arabicName})"
            setDateTime(date, time, item.readAt)
            info.text = "${surahDetails?.numberOfVerses} Verses • ${surahDetails?.revelationPlace}"

            card.setOnClickListener { onItemClick(item, RecentlyReadType.CHAPTER) }
            menu.setOnClickListener { showDeleteMenu(it, item, RecentlyReadType.CHAPTER) }
        }
    }

    private fun bindPageView(holder: PageViewHolder, item: RecentlyRead) {
        val pageNumber = item.itemId.toInt()
        val surahNumber = QuranMetadata.getInstance().getSurahNumberForPage(pageNumber)
        val surahDetails = QuranMetadata.getInstance().getSurahDetails(surahNumber)

        holder.apply {
            number.text = item.itemId
            title.text = "Page ${item.itemId}"
            surahInfo.text = "${surahDetails?.englishName} (${surahDetails?.arabicName})"
            setDateTime(date, time, item.readAt)
            juzInfo.text = "Juz ${QuranMetadata.getInstance().getJuzForPage(pageNumber)}"

            card.setOnClickListener { onItemClick(item, RecentlyReadType.PAGE) }
            menu.setOnClickListener { showDeleteMenu(it, item, RecentlyReadType.PAGE) }
        }
    }

    private fun bindJuzView(holder: JuzViewHolder, item: RecentlyRead) {
        holder.apply {
            number.text = item.itemId
            title.text = "Juz ${item.itemId}"
            setDateTime(date, time, item.readAt)
            pageInfo.text = "Pages ${QuranMetadata.getInstance().getJuzPageRange(item.itemId.toInt())}"

            card.setOnClickListener { onItemClick(item, RecentlyReadType.JUZ) }
            menu.setOnClickListener { showDeleteMenu(it, item, RecentlyReadType.JUZ) }
        }
    }

    private fun setDateTime(dateView: TextView, timeView: TextView, readAt: String) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = dateFormat.parse(readAt)

            val outputDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val outputTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            dateView.text = date?.let { outputDateFormat.format(it) }
            timeView.text = date?.let { outputTimeFormat.format(it) }
        } catch (e: ParseException) {
            dateView.text = readAt
            timeView.text = ""
        }
    }

    private fun showDeleteMenu(view: View, item: RecentlyRead, type: RecentlyReadType) {
        val popup = PopupMenu(view.context, view)
        popup.inflate(R.menu.recently_read_menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    onDeleteClick(item, type)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    fun updateCurrentType(type: RecentlyReadType) {
        if (currentType != type) {
            currentType = type
            notifyDataSetChanged()  // Need to completely redraw since view types change
        }
    }

    fun updateData(newItems: List<RecentlyRead>, type: RecentlyReadType) {
        val typeChanged = currentType != type
        currentType = type
        items = newItems

        if (typeChanged) {
            notifyDataSetChanged() // Complete redraw for type changes
        } else if (items.size != newItems.size) {
            notifyDataSetChanged() // Size changed, simple refresh
        } else {
            // If just the content changed but size is the same, could optimize with DiffUtil
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = items.size
}
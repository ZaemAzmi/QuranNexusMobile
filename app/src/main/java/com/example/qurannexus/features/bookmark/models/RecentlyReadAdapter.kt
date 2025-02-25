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
) : RecyclerView.Adapter<RecentlyReadAdapter.RecentlyReadViewHolder>() {

    private var items: List<RecentlyRead> = emptyList()
    sealed class RecentlyReadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class ChapterViewHolder(view: View) : RecentlyReadViewHolder(view) {
            val number: AutoFitTextView = view.findViewById(R.id.recentlyReadSurahNumber)
            val title: TextView = view.findViewById(R.id.recentlyReadSurahTitle)
            val date: TextView = view.findViewById(R.id.recentlyReadDate)
            val time: TextView = view.findViewById(R.id.recentlyReadTime)
            val menu: ImageView = view.findViewById(R.id.recentlyReadMenu)
            val info: TextView = view.findViewById(R.id.recentlyReadSurahInfo)
            val card: CardView = view.findViewById(R.id.recentlyReadSurahCard)
        }

        class PageViewHolder(view: View) : RecentlyReadViewHolder(view) {
            val number: AutoFitTextView = view.findViewById(R.id.recentlyReadPageNumber)
            val title: TextView = view.findViewById(R.id.recentlyReadPageTitle)
            val surahInfo: TextView = view.findViewById(R.id.recentlyReadPageSurahInfo)
            val date: TextView = view.findViewById(R.id.recentlyReadPageDate)
            val time: TextView = view.findViewById(R.id.recentlyReadPageTime)
            val menu: ImageView = view.findViewById(R.id.recentlyReadPageMenu)
            val juzInfo: TextView = view.findViewById(R.id.recentlyReadPageJuzInfo)
            val card: CardView = view.findViewById(R.id.recentlyReadPageCard)
        }

        class JuzViewHolder(view: View) : RecentlyReadViewHolder(view) {
            val number: AutoFitTextView = view.findViewById(R.id.recentlyReadJuzNumber)
            val title: TextView = view.findViewById(R.id.recentlyReadJuzTitle)
            val date: TextView = view.findViewById(R.id.recentlyReadJuzDate)
            val time: TextView = view.findViewById(R.id.recentlyReadJuzTime)
            val menu: ImageView = view.findViewById(R.id.recentlyReadJuzMenu)
            val pageInfo: TextView = view.findViewById(R.id.recentlyReadJuzPageInfo)
            val card: CardView = view.findViewById(R.id.recentlyReadJuzCard)
        }
    }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentlyReadViewHolder {
            return when (currentType) {
                RecentlyReadType.CHAPTER -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.card_item_recently_read_surah, parent, false)
                    RecentlyReadViewHolder.ChapterViewHolder(view)
                }
                RecentlyReadType.PAGE -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.card_item_recently_read_page, parent, false)
                    RecentlyReadViewHolder.PageViewHolder(view)
                }
                RecentlyReadType.JUZ -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.card_item_recently_read_juz, parent, false)
                    RecentlyReadViewHolder.JuzViewHolder(view)
                }
            }
        }

        override fun onBindViewHolder(holder: RecentlyReadViewHolder, position: Int) {
            val item = items[position]
            val context = holder.itemView.context

            when (holder) {
                is RecentlyReadViewHolder.ChapterViewHolder -> bindChapterView(holder, item)
                is RecentlyReadViewHolder.PageViewHolder -> bindPageView(holder, item)
                is RecentlyReadViewHolder.JuzViewHolder -> bindJuzView(holder, item)
            }
        }

        private fun bindChapterView(holder: RecentlyReadViewHolder.ChapterViewHolder, item: RecentlyRead) {
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

        private fun bindPageView(holder: RecentlyReadViewHolder.PageViewHolder, item: RecentlyRead) {
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

        private fun bindJuzView(holder: RecentlyReadViewHolder.JuzViewHolder, item: RecentlyRead) {
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
            currentType = type
            notifyDataSetChanged()  // Or use more specific notify methods if needed
        }
        fun updateData(newItems: List<RecentlyRead>, type: RecentlyReadType) {
            currentType = type
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size
}

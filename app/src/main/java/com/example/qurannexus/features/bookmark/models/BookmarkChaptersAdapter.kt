package com.example.qurannexus.features.bookmark.models

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.features.recitation.RecitationPageFragment
import com.example.qurannexus.features.recitation.models.SurahModel

class BookmarkChaptersAdapter(private var chaptersList: List<BookmarkChapter>) : RecyclerView.Adapter<BookmarkChaptersAdapter.BookmarkChapterViewHolder>() {

    class BookmarkChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chapterNumberTextView: TextView = itemView.findViewById(R.id.bookmarkChapterNumber)
        val chapterTitleTextView: TextView = itemView.findViewById(R.id.bookmarkChapterTitle)
        val chapterInfoTextView: TextView = itemView.findViewById(R.id.bookmarkChapterRevelationPlace)
        val cardView: CardView = itemView.findViewById(R.id.bookmarkChapterCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_bookmark_chapter, parent, false)
        return BookmarkChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkChapterViewHolder, position: Int) {
        val chapter = chaptersList[position]
        val context = holder.itemView.context

        holder.chapterNumberTextView.text = "${chapter.chapterNumber}"
        holder.chapterTitleTextView.text = chapter.chapterTitle
        holder.chapterInfoTextView.text = chapter.chapterInfo

        holder.cardView.setOnClickListener {
            navigateToChapter(context, chapter.chapterNumber.toInt())
        }
    }
    private fun navigateToChapter(context: Context, chapterNumber: Int) {
        val activity = context as? FragmentActivity ?: return

        val surahDetails = QuranMetadata.getInstance().getSurahDetails(chapterNumber)
        val surahModel = SurahModel(
            surahDetails?.englishName ?: "",
            surahDetails?.arabicName ?: "",
            chapterNumber.toString(),
            surahDetails?.translationName ?: "",
            surahDetails?.numberOfVerses.toString(),
            false // Set initial bookmark state
        )

        val fragment = RecitationPageFragment.newInstance(surahModel, "verseByVerse", chapterNumber - 1)

        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()

    }
    override fun getItemCount() = chaptersList.size

    fun updateData(newList: List<BookmarkChapter>) {
        chaptersList = newList
        notifyDataSetChanged()
    }
}

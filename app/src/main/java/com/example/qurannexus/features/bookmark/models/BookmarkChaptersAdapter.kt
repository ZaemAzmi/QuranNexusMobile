package com.example.qurannexus.features.bookmark.models

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.features.recitation.RecitationPageFragment

class BookmarkChaptersAdapter(private var chaptersList: List<BookmarkChapter>) :
    RecyclerView.Adapter<BookmarkChaptersAdapter.BookmarkChapterViewHolder>() {

    class BookmarkChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chapterNumberTextView: TextView = itemView.findViewById(R.id.bookmarkChapterNumber)
        val chapterTitleTextView: TextView = itemView.findViewById(R.id.bookmarkChapterTitle)
        val chapterEnglishTitle: TextView = itemView.findViewById(R.id.bookmarkChapterEnglishTitle)
        val chapterRevelationPlace: TextView = itemView.findViewById(R.id.bookmarkChapterRevelationPlace)
        val bookmarkChapterArabicTitle: TextView = itemView.findViewById(R.id.bookmarkChapterArabicTitle)
        val numberOfVerses: TextView = itemView.findViewById(R.id.bookmarkChapterNumberOfVerses)
        val cardView: CardView = itemView.findViewById(R.id.bookmarkChapterCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_bookmark_chapter, parent, false)
        return BookmarkChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkChapterViewHolder, position: Int) {
        val bookmarkChapter = chaptersList[position]
        val context = holder.itemView.context

        val chapterNumber = try {
            bookmarkChapter.itemProperties.chapterId.toInt()
        } catch (e: NumberFormatException) {
            // Handle error if chapterId is not a valid number
            Log.e("BookmarkChapterAdapter", "Invalid chapter_id: ${bookmarkChapter.itemProperties.chapterId}")
            return // Stop binding this invalid item
        }
        val surahDetails = QuranMetadata.getInstance().getSurahDetails(chapterNumber)
        if(surahDetails != null){
            holder.chapterNumberTextView.text = surahDetails.surahIndex.toString()
            holder.chapterTitleTextView.text = surahDetails.englishName
            holder.chapterEnglishTitle.text = surahDetails.translationName
            holder.chapterRevelationPlace.text = surahDetails.revelationPlace
            holder.bookmarkChapterArabicTitle.text = surahDetails.arabicName
            val numberOfVerse = surahDetails.numberOfVerses.toString()
            holder.numberOfVerses.text = numberOfVerse + "Verses"
        }


        holder.cardView.setOnClickListener {
            navigateToChapter(context, chapterNumber)
        }
    }
    private fun navigateToChapter(context: Context, chapterNumber: Int) {
        // Get the correct Activity context, even from a nested fragment
        var activityContext = context
        while (activityContext !is FragmentActivity && activityContext is ContextWrapper) {
            activityContext = activityContext.baseContext
        }

        val activity = activityContext as? FragmentActivity
        if (activity == null) {
            // Handle error if activity context cannot be found
            return
        }

        // Check user's preferred layout
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val isByPage = sharedPreferences.getBoolean("recitation_layout_by_page", false)

        val fragment: RecitationPageFragment

        if (isByPage) {
            // User prefers Page-by-Page view. We need to find the starting page of the chapter.
            val startingPage = QuranMetadata.getInstance().getStartingPage(chapterNumber)

            fragment = RecitationPageFragment.newInstanceForNavigation(
                true, chapterNumber.toString(), "1", startingPage, null, null
                // Default to first verse
                // The most important piece
                // No specific scroll needed
            )
        } else {
            // User prefers Verse-by-Verse view.
            fragment = RecitationPageFragment.newInstanceForNavigation(
                false, chapterNumber.toString(), "1", null, null, chapterNumber - 1
                // Default to first verse
                // Not needed, will be calculated from chapter
            )
        }

        // Perform the fragment transaction
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

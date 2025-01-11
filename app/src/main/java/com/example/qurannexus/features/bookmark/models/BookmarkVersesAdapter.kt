package com.example.qurannexus.features.bookmark.models

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.features.recitation.RecitationPageFragment
import com.example.qurannexus.features.recitation.models.SurahModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookmarkVersesAdapter(private var versesList: List<BookmarkVerse>) :
    RecyclerView.Adapter<BookmarkVersesAdapter.BookmarkVerseViewHolder>() {

    inner class BookmarkVerseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val verseTitle: TextView = itemView.findViewById(R.id.bookmarkVerseTitle)
        val verseChapter: TextView = itemView.findViewById(R.id.bookmarkVerseChapter)
        val verseDescription: TextView = itemView.findViewById(R.id.bookmarkVerseDescription)
        val verseChapterAndVerseNumber: TextView = itemView.findViewById(R.id.bookmarkVerseChapterAndVerseNumber)
        val menuButton: ImageView = itemView.findViewById(R.id.bookmarkVerseMenu)
        val cardView: CardView = itemView.findViewById(R.id.bookmarkVerseCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkVerseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_bookmark_verse, parent, false)
        return BookmarkVerseViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkVerseViewHolder, position: Int) {
        val verse = versesList[position]
        val context = holder.itemView.context

        // Get chapter details from QuranMetadata
        val chapterDetails = QuranMetadata.getInstance().getSurahDetails(verse.chapter_id.toInt())

        chapterDetails?.let { details ->
            // Set the verse title
            holder.verseTitle.text = "Verse of ${details.englishName}"

            // Set the Arabic chapter name
            holder.verseChapter.text = "(${details.arabicName}) - ${details.translationName}"

            // Set the description (notes)
            holder.verseDescription.text = if (!verse.notes.isNullOrBlank()) {
                verse.notes
            } else {
                "-"
            }

            // Set chapter and verse number
            holder.verseChapterAndVerseNumber.text = "Chapter ${verse.chapter_id}, Verse ${verse.ayah_id}"

            // Setup menu button
            holder.menuButton.setOnClickListener { view ->
                showPopupMenu(view, verse, context)
            }

            holder.cardView.setOnClickListener {
                navigateToVerse(context, verse.chapter_id.toInt(), verse.ayah_id.toInt())
            }
        }
    }
    private fun navigateToVerse(context: Context, chapterId: Int, verseId: Int) {
        val activity = context as? FragmentActivity ?: return

        // Get SurahDetails using QuranMetadata
        val surahDetails = QuranMetadata.getInstance().getSurahDetails(chapterId)
        val surahModel = SurahModel(
            surahDetails?.englishName ?: "",
            surahDetails?.arabicName ?: "",
            chapterId.toString(),
            surahDetails?.translationName ?: "",
            surahDetails?.numberOfVerses.toString(),
            false // Set initial bookmark state
        )
        // Create bundle for the verse to scroll to
        val bundle = Bundle().apply {
            putInt("scrollToVerse", verseId)
        }

        // Replace current fragment
        val fragment = RecitationPageFragment.newInstance(surahModel, "verseByVerse", chapterId - 1)

        // Replace current fragment
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    private fun showPopupMenu(view: View, verse: BookmarkVerse, context: Context) {
        val popup = PopupMenu(context, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.bookmark_verse_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    showDeleteConfirmationDialog(verse, context)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun showDeleteConfirmationDialog(verse: BookmarkVerse, context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Delete Bookmark")
            .setMessage("Are you sure you want to delete this bookmark?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteBookmark(verse, context)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteBookmark(verse: BookmarkVerse, context: Context) {
        // Get token from SharedPreferences
        val token = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)

        if (token == null) {
            Toast.makeText(context, "Please login to delete bookmarks", Toast.LENGTH_SHORT).show()
            return
        }

        val quranApi = ApiService.getQuranClient().create(QuranApi::class.java)
        quranApi.removeBookmark("Bearer $token", "verse", verse.ayah_id)
            .enqueue(object : Callback<RemoveBookmarkResponse> {
                override fun onResponse(
                    call: Call<RemoveBookmarkResponse>,
                    response: Response<RemoveBookmarkResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        // Remove the item from the list and update the adapter
                        val updatedList = versesList.toMutableList()
                        updatedList.remove(verse)
                        updateData(updatedList)
                        Toast.makeText(context, "Bookmark deleted successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to delete bookmark", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RemoveBookmarkResponse>, t: Throwable) {
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun getItemCount() = versesList.size

    fun updateData(newList: List<BookmarkVerse>) {
        versesList = newList
        notifyDataSetChanged()
    }
}
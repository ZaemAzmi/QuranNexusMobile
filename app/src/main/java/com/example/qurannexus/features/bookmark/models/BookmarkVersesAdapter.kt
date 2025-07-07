package com.example.qurannexus.features.bookmark.models

import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.database.entities.VerseLocationInfo
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.features.recitation.RecitationPageFragment
import com.example.qurannexus.features.recitation.data.RecitationDao
import com.example.qurannexus.features.recitation.models.SurahModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookmarkVersesAdapter(
    private var versesList: List<BookmarkVerse>,
    private val dao: RecitationDao,
    private val lifecycleOwner: LifecycleOwner
) :
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

        val surahDetails = QuranMetadata.getInstance().getSurahDetails(verse.itemProperties.surahId.toInt())

        surahDetails?.let { details ->
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
            holder.verseChapterAndVerseNumber.text = "Chapter ${verse.itemProperties.surahId}, Verse ${verse.itemProperties.ayahIndex}"

            // Setup menu button
            holder.menuButton.setOnClickListener { view ->
                showPopupMenu(view, verse, context)
            }

            holder.cardView.setOnClickListener {
                val globalIndex = verse.itemProperties.ayahIndex.toInt()
                val surahId = verse.itemProperties.surahId.toInt()
                navigateToVerse(context, surahId, globalIndex)
            }
        }
    }

    private fun navigateToVerse(context: Context, chapterId: Int, globalAyahIndex: Int) {
        Log.d("BookmarkAdapter", "Navigate request: chapterId=$chapterId, globalAyahIndex=$globalAyahIndex")

        // Get the correct Activity context, even from a nested fragment
        var activityContext = context
        while (activityContext !is FragmentActivity && activityContext is ContextWrapper) {
            activityContext = activityContext.baseContext
        }

        val activity = activityContext as? FragmentActivity
        if (activity == null) {
            Log.e("BookmarkAdapter", "Could not find FragmentActivity context")
            Toast.makeText(context, "Unable to navigate to verse", Toast.LENGTH_SHORT).show()
            return
        }
        // Now that we have the activity, proceed with the navigation logic
        // Use the safe lifecycleScope to perform the background DB query
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val locationInfo = dao.getVerseLocationByGlobalIndex(globalAyahIndex)

            // Switch back to the main thread to perform UI actions
            withContext(Dispatchers.Main) {
                if (locationInfo == null) {
                    Toast.makeText(context, "Could not find verse location.", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val targetPage = locationInfo.pageId
                val perSurahIndex = locationInfo.perSurahAyahIndex

                Log.d("BookmarkAdapter", "DB Result: TargetPage=$targetPage, PerSurahIndex=$perSurahIndex")

                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
                val isByPage = sharedPreferences.getBoolean("recitation_layout_by_page", false)

                val fragment: RecitationPageFragment

                if (isByPage) {
                    // Logic for Page-by-Page navigation
                    Log.d("BookmarkAdapter", "Navigating in Page Mode")
                    fragment = RecitationPageFragment.newInstanceForNavigation(
                        true,
                        chapterId.toString(),
                        perSurahIndex.toString(),
                        targetPage,
                        perSurahIndex,
                        null
                    )
                } else {
                    // Logic for Verse-by-Verse navigation
                    Log.d("BookmarkAdapter", "Navigating in Verse Mode")
                    // --- FIX: The issue with your Page adapter is here. It uses an older newInstance.
                    // We will use the correct newInstanceForNavigation.
                    val verseByVerseFragment = RecitationPageFragment.newInstanceForNavigation(
                        false,
                        chapterId.toString(),
                        perSurahIndex.toString(),
                        targetPage,
                        null,
                        chapterId - 1
                        // Pass the page number for initial load
                    )
                    fragment = verseByVerseFragment
                }

                // Perform the fragment transaction on the main container
                try {
                    activity.supportFragmentManager.beginTransaction()
                        .replace(R.id.mainFragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                    Log.d("BookmarkAdapter", "Fragment transaction committed successfully.")
                } catch (e: Exception) {
                    Log.e("BookmarkAdapter", "Error during fragment transaction", e)
                }
            }
        }
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
        quranApi.removeBookmark("Bearer $token", "verse", verse.itemProperties.ayahIndex)
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
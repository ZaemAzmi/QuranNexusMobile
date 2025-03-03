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
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.customViews.AutoFitTextView
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.features.recitation.RecitationPageFragment
import com.example.qurannexus.features.recitation.models.SurahModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class BookmarkPagesAdapter(private var pagesList: List<BookmarkPage>) :
    RecyclerView.Adapter<BookmarkPagesAdapter.BookmarkPageViewHolder>() {

    inner class BookmarkPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pageNumber: AutoFitTextView = itemView.findViewById(R.id.bookmarkPageNumber)
        val pageTitle: TextView = itemView.findViewById(R.id.bookmarkPageTitle)
        val pageSurahInfo: TextView = itemView.findViewById(R.id.bookmarkPageSurahInfo)
        val pageNotes: TextView = itemView.findViewById(R.id.bookmarkPageNotes)
        val pageJuzInfo: TextView = itemView.findViewById(R.id.bookmarkPageJuzInfo)
        val pageDateAdded: TextView = itemView.findViewById(R.id.bookmarkPageDateAdded)
        val menuButton: ImageView = itemView.findViewById(R.id.bookmarkPageMenu)
        val cardView: CardView = itemView.findViewById(R.id.bookmarkPageCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkPageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_bookmark_page, parent, false)
        return BookmarkPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkPageViewHolder, position: Int) {
        val page = pagesList[position]
        val context = holder.itemView.context

        // Get the surah that starts on this page
        val surahNumber = QuranMetadata.getInstance().getSurahNumberForPage(page.itemProperties.pageNumber)
        val surahDetails = QuranMetadata.getInstance().getSurahDetails(surahNumber)

        holder.pageNumber.text = surahNumber.toString()
        holder.pageTitle.text = "Page ${page.itemProperties.pageNumber.toString()}"
        holder.pageSurahInfo.text = "From Surah ${surahDetails?.englishName}"
        holder.pageNotes.text = if (page.notes.isNotBlank()) page.notes else "No notes"
        holder.pageJuzInfo.text = "Juz ${calculateJuzForPage(page.itemProperties.pageNumber)}"

        // Format date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val date = inputFormat.parse(page.createdAt)
            holder.pageDateAdded.text = "Added on ${outputFormat.format(date)}"
        } catch (e: ParseException) {
            // Fallback in case of parsing error
            holder.pageDateAdded.text = "Date not available"
        }
        // Setup menu button
        holder.menuButton.setOnClickListener { view ->
            showPopupMenu(view, page, context)
        }

        // Navigate to page on click
        holder.cardView.setOnClickListener {
            Log.d("BookmarkPagesAdapter", "Item clicked: Page ${page.itemProperties.pageNumber}")
            navigateToPage(context, page.itemProperties.pageNumber)
        }
    }

    private fun calculateJuzForPage(pageNumber: Int): Int {
        // Use the QuranMetadata utility to get the juz number for this page
        return QuranMetadata.getInstance().getJuzForPage(pageNumber)
    }
    private fun navigateToPage(context: Context, pageNumber: Int) {
        Log.d("BookmarkPagesAdapter", "Navigating to page: $pageNumber")

        // Get the correct Activity context
        var activityContext = context
        while (activityContext !is FragmentActivity && activityContext is ContextWrapper) {
            activityContext = activityContext.baseContext
        }

        val activity = activityContext as? FragmentActivity
        if (activity == null) {
            Log.e("BookmarkPagesAdapter", "Could not find FragmentActivity context")

            // Try to get activity through alternative method
            if (context is View) {
                val foundActivity = context.context as? FragmentActivity
                if (foundActivity != null) {
                    navigateWithActivity(foundActivity, pageNumber)
                    return
                }
            }

            Toast.makeText(context, "Unable to navigate to page", Toast.LENGTH_SHORT).show()
            return
        }

        navigateWithActivity(activity, pageNumber)
    }

    private fun navigateWithActivity(activity: FragmentActivity, pageNumber: Int) {
        try {
            // Get the surah that starts on this page
            val surahNumber = QuranMetadata.getInstance().getSurahNumberForPage(pageNumber)
            val surahDetails = QuranMetadata.getInstance().getSurahDetails(surahNumber)

            // Create a SurahModel for the fragment
            val surahModel = SurahModel(
                surahDetails?.englishName ?: "",
                surahDetails?.arabicName ?: "",
                surahNumber.toString(),
                surahDetails?.translationName ?: "",
                surahDetails?.numberOfVerses.toString(),
                false
            )

            // Create RecitationPageFragment with pageByPage layout
            val fragment = RecitationPageFragment.newInstance(
                surahModel,
                "pageByPage",
                surahNumber - 1
            )

            // Add the page number as an argument
            val args = fragment.arguments ?: Bundle()
            args.putInt("initial_page", pageNumber)
            fragment.arguments = args

            // Perform the fragment transaction
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .addToBackStack(null)
                .commit()

            Log.d("BookmarkPagesAdapter", "Fragment transaction committed")
        } catch (e: Exception) {
            Log.e("BookmarkPagesAdapter", "Error during navigation", e)
        }
    }

    private fun showPopupMenu(view: View, page: BookmarkPage, context: Context) {
        val popup = PopupMenu(context, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.bookmark_card_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    showDeleteConfirmationDialog(page, context)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun showDeleteConfirmationDialog(page: BookmarkPage, context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Delete Bookmark")
            .setMessage("Are you sure you want to delete this page bookmark?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteBookmark(page, context)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteBookmark(page: BookmarkPage, context: Context) {
        val token = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)

        if (token == null) {
            Toast.makeText(context, "Please login to delete bookmarks", Toast.LENGTH_SHORT).show()
            return
        }

        val quranApi = ApiService.getQuranClient().create(QuranApi::class.java)
        quranApi.removeBookmark("Bearer $token", "page", page.itemProperties.pageId)
            .enqueue(object : Callback<RemoveBookmarkResponse> {
                override fun onResponse(
                    call: Call<RemoveBookmarkResponse>,
                    response: Response<RemoveBookmarkResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val updatedList = pagesList.toMutableList()
                        updatedList.remove(page)
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

    override fun getItemCount() = pagesList.size

    fun updateData(newList: List<BookmarkPage>) {
        pagesList = newList
        notifyDataSetChanged()
    }
}
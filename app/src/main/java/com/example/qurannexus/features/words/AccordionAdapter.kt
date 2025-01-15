package com.example.qurannexus.features.words

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.databinding.ItemWordAccordionSectionBinding
import com.example.qurannexus.features.bookmark.models.BookmarkWord
import com.example.qurannexus.features.home.WordDetailsActivity

class AccordionAdapter(
    private val onWordClick: (BookmarkWord) -> Unit
) : RecyclerView.Adapter<AccordionAdapter.AccordionViewHolder>() {

    private val sections = mutableListOf<AccordionSection>()

    fun submitList(newSections: List<AccordionSection>) {
        sections.clear()
        sections.addAll(newSections)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccordionViewHolder {
        val binding = ItemWordAccordionSectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AccordionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccordionViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount(): Int = sections.size

    inner class AccordionViewHolder(private val binding: ItemWordAccordionSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false

        fun bind(section: AccordionSection) {
            binding.textViewSectionTitle.text = section.title
            binding.textViewItemCount.text = "(${section.words.size})"

            // Setup child RecyclerView with BookmarkWord items
            val childAdapter = BookmarkWordChildAdapter(
                onItemClick = { bookmarkWord ->
                    // Navigate to word details
                    val intent = Intent(binding.root.context, WordDetailsActivity::class.java).apply {
                        putExtra("WORD_ID", bookmarkWord.word_id)
                        putExtra("WORD_TEXT", bookmarkWord.word_text)
                        putExtra("TRANSLATION", bookmarkWord.translation)
                        putExtra("TRANSLITERATION", bookmarkWord.transliteration)
                        putExtra("SURAH_NAME", bookmarkWord.surah_name)
                        putExtra("AYAH_KEY", bookmarkWord.ayah_key)
                    }
                    binding.root.context.startActivity(intent)
                }
            )
            childAdapter.submitList(section.words)

            binding.recyclerViewWordAccordionChildItems.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = childAdapter
            }

            // Expand/collapse functionality
            binding.accordionHeader.setOnClickListener {
                isExpanded = !isExpanded
                binding.recyclerViewWordAccordionChildItems.visibility =
                    if (isExpanded) View.VISIBLE else View.GONE

                // Update expand/collapse icon
                binding.imageViewExpandCollapse.setImageResource(
                    if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                )
            }
        }
    }

    // New child adapter for bookmark words
    inner class BookmarkWordChildAdapter(
        private val onItemClick: (BookmarkWord) -> Unit
    ) : RecyclerView.Adapter<BookmarkWordChildAdapter.WordViewHolder>() {

        private val words = mutableListOf<BookmarkWord>()

        fun submitList(newWords: List<BookmarkWord>) {
            words.clear()
            words.addAll(newWords)
            notifyDataSetChanged()
        }

        inner class WordViewHolder(private val itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val wordText by lazy { itemView.findViewById<TextView>(R.id.bookmarkWordArabicText) }
            private val translationText by lazy { itemView.findViewById<TextView>(R.id.bookmarkWordEnglishText) }
//            private val surahNameText by lazy { itemView.findViewById<TextView>(R.id.surahNameText) }

            fun bind(bookmarkWord: BookmarkWord) {
                wordText.text = bookmarkWord.word_text
                translationText.text = bookmarkWord.translation
//                surahNameText.text = bookmarkWord.surah_name

                itemView.setOnClickListener { onItemClick(bookmarkWord) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_item_bookmark_word, parent, false)
            return WordViewHolder(view)
        }

        override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
            holder.bind(words[position])
        }

        override fun getItemCount() = words.size
    }
}
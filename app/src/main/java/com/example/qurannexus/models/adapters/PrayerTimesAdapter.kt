package com.example.qurannexus.models.adapters
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.models.PrayerTime
class PrayerTimesAdapter(private val prayerTimes: List<PrayerTime>) :
    RecyclerView.Adapter<PrayerTimesAdapter.PrayerTimeViewHolder>() {

    class PrayerTimeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.prayer_name)
        val time: TextView = view.findViewById(R.id.prayer_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrayerTimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.prayer_time_card_view, parent, false)
        return PrayerTimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrayerTimeViewHolder, position: Int) {
        val prayerTime = prayerTimes[position]
        holder.name.text = prayerTime.name
        holder.time.text = prayerTime.time
    }

    override fun getItemCount() = prayerTimes.size
}

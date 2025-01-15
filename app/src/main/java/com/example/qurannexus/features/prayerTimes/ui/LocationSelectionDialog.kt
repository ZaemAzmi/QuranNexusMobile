package com.example.qurannexus.features.prayerTimes.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R

// Create a new file: LocationSelectionDialog.kt
class LocationSelectionDialog(
    context: Context,
    private val onLocationSelected: (city: String, country: String) -> Unit
) : Dialog(context) {

    private val malaysianCities = listOf(
        "Kuala Lumpur" to "MY",
        "Johor Bahru" to "MY",
        "George Town" to "MY",
        "Ipoh" to "MY",
        "Shah Alam" to "MY",
        "Kota Kinabalu" to "MY",
        "Kuching" to "MY",
        "Malacca City" to "MY",
        "Alor Setar" to "MY",
        "Seremban" to "MY",
        "Miri" to "MY",
        "Kuala Terengganu" to "MY",
        "Kota Bharu" to "MY",
        "Sandakan" to "MY",
        "Sibu" to "MY",
        "Iskandar Puteri" to "MY",
        "Putrajaya" to "MY",
        "Labuan" to "MY",
        "Taiping" to "MY",
        "Sungai Petani" to "MY",
        "Kulim" to "MY",
        "Langkawi" to "MY",
        "Sepang" to "MY",
        "Cyberjaya" to "MY",
        "Batu Pahat" to "MY",
        "Kluang" to "MY",
        "Tawau" to "MY",
        "Bintulu" to "MY",
        "Port Dickson" to "MY",
        "Kuantan" to "MY",
        "Pasir Gudang" to "MY",
        "Pontian" to "MY",
        "Keningau" to "MY",
        "Marang" to "MY",
        "Jasin" to "MY",
        "Bentong" to "MY",
        "Raub" to "MY",
        "Temerloh" to "MY",
        "Manjung" to "MY",
        "Gua Musang" to "MY",
        "Lahad Datu" to "MY",
        "Ranau" to "MY",
        "Dungun" to "MY"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_location_selection)

        val recyclerView = findViewById<RecyclerView>(R.id.locationsRecyclerView)
        val searchEditText = findViewById<EditText>(R.id.searchEditText)

        val adapter = LocationsAdapter(malaysianCities) { city, country ->
            onLocationSelected(city, country)
            dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Add search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filteredCities = malaysianCities.filter {
                    it.first.lowercase().contains(query)
                }
                adapter.updateLocations(filteredCities)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}

// Create LocationsAdapter.kt
class LocationsAdapter(
    private var locations: List<Pair<String, String>>,
    private val onLocationSelected: (String, String) -> Unit
) : RecyclerView.Adapter<LocationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cityText: TextView = view.findViewById(R.id.cityText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (city, country) = locations[position]
        holder.cityText.text = city
        holder.itemView.setOnClickListener {
            onLocationSelected(city, country)
        }
    }

    override fun getItemCount() = locations.size

    fun updateLocations(newLocations: List<Pair<String, String>>) {
        locations = newLocations
        notifyDataSetChanged()
    }
}
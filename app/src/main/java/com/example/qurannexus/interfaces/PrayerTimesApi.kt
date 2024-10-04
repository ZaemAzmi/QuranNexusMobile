import com.example.qurannexus.models.PrayerTimesResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PrayerTimesApi {
    @GET("v1/timingsByCity")
    fun getPrayerTimes(
        @Query("date") date: String,
        @Query("city") city: String,
        @Query("country") country: String
    ): Call<PrayerTimesResponse>
}

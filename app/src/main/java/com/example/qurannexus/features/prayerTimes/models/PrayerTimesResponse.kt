package com.example.qurannexus.features.prayerTimes.models

class PrayerTimesResponse {
    var data: Data? = null

    inner class Data {
        var timings: Timings? = null
        var date: Date? = null
    }

    inner class Timings {
        var Fajr: String? = null
        var Sunrise: String? = null
        var Dhuhr: String? = null
        var Asr: String? = null
        var Maghrib: String? = null
        var Isha: String? = null
        var Imsak: String? = null
    }

    inner class Date {
        var readable : String? = null
        var gregorian: Gregorian? = null
        var hijri : Hijri? = null
    }
    inner class Gregorian {
        var weekday: Weekday? = null
    }

    inner class Weekday {
        var en: String? = null
    }

    inner class Hijri{
        var year : Int? = null
        var month : Month? = null
    }  inner class Month {
        var number: Int? = null
        var en: String? = null
    }
}

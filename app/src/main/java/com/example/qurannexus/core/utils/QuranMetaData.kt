package com.example.qurannexus.core.utils


class QuranMetadata private constructor() {
    private val surahMap: MutableMap<Int, SurahDetails> = HashMap()

    init {
        surahMap[1] = SurahDetails(1, "الفاتحة", "Al-Faatiha", "The Opening", 1, "Makkah", 7)
        surahMap[2] = SurahDetails(2, "البقرة", "Al-Baqara", "The Cow", 2, "Madinah", 286)
        surahMap[3] = SurahDetails(3, "آل عمران", "Aal-i-Imraan", "The Family of Imran", 50, "Madinah", 200)
        surahMap[4] = SurahDetails(4, "النساء", "An-Nisaa", "The Women", 77, "Madinah", 176)
        surahMap[5] = SurahDetails(5, "المائدة", "Al-Maaida", "The Table Spread", 107, "Madinah", 120)
        surahMap[6] = SurahDetails(6, "الأنعام", "Al-An'aam", "The Cattle", 128, "Makkah", 165)
        surahMap[7] = SurahDetails(7, "الأعراف", "Al-A'raaf", "The Heights", 151, "Makkah", 206)
        surahMap[8] = SurahDetails(8, "الأنفال", "Al-Anfaal", "The Spoils of War", 177, "Madinah", 75)
        surahMap[9] = SurahDetails(9, "التوبة", "At-Tawba", "The Repentance", 187, "Madinah", 129)
        surahMap[10] = SurahDetails(10, "يونس", "Yunus", "Prophet Jonah", 208, "Makkah", 109)
        surahMap[11] = SurahDetails(11, "هود", "Hud", "Prophet Hood", 222, "Makkah", 123)
        surahMap[12] = SurahDetails(12, "يوسف", "Yusuf", "Prophet Joseph", 236, "Makkah", 111)
        surahMap[13] = SurahDetails(13, "الرعد", "Ar-Ra'd", "The Thunder", 249, "Madinah", 43)
        surahMap[14] = SurahDetails(14, "ابراهيم", "Ibrahim", "Prophet Abraham", 256, "Makkah", 52)
        surahMap[15] = SurahDetails(15, "الحجر", "Al-Hijr", "The Rocky Tract", 262, "Makkah", 99)
        surahMap[16] = SurahDetails(16, "النحل", "An-Nahl", "The Bee", 268, "Makkah", 128)
        surahMap[17] = SurahDetails(17, "الإسراء", "Al-Israa", "The Night Journey", 282, "Makkah", 111)
        surahMap[18] = SurahDetails(18, "الكهف", "Al-Kahf", "The Cave", 294, "Makkah", 110)
        surahMap[19] = SurahDetails(19, "مريم", "Maryam", "Mary", 305, "Makkah", 98)
        surahMap[20] = SurahDetails(20, "طه", "Taa-Haa", "Ta-Ha", 313, "Makkah", 135)
        surahMap[21] = SurahDetails(21, "الأنبياء", "Al-Anbiyaa", "The Prophets", 322, "Makkah", 112)
        surahMap[22] = SurahDetails(22, "الحج", "Al-Hajj", "The Pilgrimage", 332, "Madinah", 78)
        surahMap[23] = SurahDetails(23, "المؤمنون", "Al-Muminoon", "The Believers", 342, "Makkah", 118)
        surahMap[24] = SurahDetails(24, "النور", "An-Noor", "The Light", 350, "Madinah", 64)
        surahMap[25] = SurahDetails(25, "الفرقان", "Al-Furqaan", "The Criterion", 360, "Makkah", 77)
        surahMap[26] = SurahDetails(26, "الشعراء", "Ash-Shu'araa", "The Poets", 367, "Makkah", 227)
        surahMap[27] = SurahDetails(27, "النمل", "An-Naml", "The Ant", 377, "Makkah", 93)
        surahMap[28] = SurahDetails(28, "القصص", "Al-Qasas", "The Stories", 386, "Makkah", 88)
        surahMap[29] = SurahDetails(29, "العنكبوت", "Al-Ankaboot", "The Spider", 397, "Makkah", 69)
        surahMap[30] = SurahDetails(30, "الروم", "Ar-Room", "The Romans", 405, "Makkah", 60)
        surahMap[31] = SurahDetails(31, "لقمان", "Luqman", "Luqman", 411, "Makkah", 34)
        surahMap[32] = SurahDetails(32, "السجدة", "As-Sajda", "The Prostration", 415, "Makkah", 30)
        surahMap[33] = SurahDetails(33, "الأحزاب", "Al-Ahzaab", "The Combined Forces", 418, "Madinah", 73)
        surahMap[34] = SurahDetails(34, "سبإ", "Saba", "Sheba", 428, "Makkah", 54)
        surahMap[35] = SurahDetails(35, "فاطر", "Faatir", "The Originator", 435, "Makkah", 45)
        surahMap[36] = SurahDetails(36, "يس", "Yaseen", "Ya Sin", 441, "Makkah", 83)
        surahMap[37] = SurahDetails(37, "الصافات", "As-Saaffaat", "Those Ranged in Ranks", 446, "Makkah", 182)
        surahMap[38] = SurahDetails(38, "ص", "Saad", "The Letter Sad", 453, "Makkah", 88)
        surahMap[39] = SurahDetails(39, "الزمر", "Az-Zumar", "The Groups", 459, "Makkah", 75)
        surahMap[40] = SurahDetails(40, "غافر", "Al-Ghaafir", "The Forgiver", 468, "Makkah", 85)
        surahMap[41] = SurahDetails(41, "فصلت", "Fussilat", "Explained in Detail", 477, "Makkah", 54)
        surahMap[42] = SurahDetails(42, "الشورى", "Ash-Shura", "The Consultation", 483, "Makkah", 53)
        surahMap[43] = SurahDetails(43, "الزخرف", "Az-Zukhruf", "The Gold Adornments", 490, "Makkah", 89)
        surahMap[44] = SurahDetails(44, "الدخان", "Ad-Dukhaan", "The Smoke", 496, "Makkah", 59)
        surahMap[45] = SurahDetails(45, "الجاثية", "Al-Jaathiya", "The Crouching", 499, "Makkah", 37)
        surahMap[46] = SurahDetails(46, "الأحقاف", "Al-Ahqaf", "The Wind-Curved Sandhills", 503, "Makkah", 35)
        surahMap[47] = SurahDetails(47, "محمد", "Muhammad", "Muhammad", 507, "Madinah", 38)
        surahMap[48] = SurahDetails(48, "الفتح", "Al-Fath", "The Victory", 511, "Madinah", 29)
        surahMap[49] = SurahDetails(49, "الحجرات", "Al-Hujuraat", "The Chambers", 516, "Madinah", 18)
        surahMap[50] = SurahDetails(50, "ق", "Qaaf", "The Letter Qaf", 518, "Makkah", 45)
        surahMap[51] = SurahDetails(51, "الذاريات", "Adh-Dhaariyat", "The Winnowing Winds", 521, "Makkah", 60)
        surahMap[52] = SurahDetails(52, "الطور", "At-Tur", "The Mount", 524, "Makkah", 49)
        surahMap[53] = SurahDetails(53, "النجم", "An-Najm", "The Star", 526, "Makkah", 62)
        surahMap[54] = SurahDetails(54, "القمر", "Al-Qamar", "The Moon", 529, "Makkah", 55)
        surahMap[55] = SurahDetails(55, "الرحمن", "Ar-Rahmaan", "The Beneficent", 532, "Madinah", 78)
        surahMap[56] = SurahDetails(56, "الواقعة", "Al-Waaqia", "The Inevitable", 535, "Makkah", 96)
        surahMap[57] = SurahDetails(57, "الحديد", "Al-Hadid", "The Iron", 538, "Madinah", 29)
        surahMap[58] = SurahDetails(58, "المجادلة", "Al-Mujaadila", "The Pleading Woman", 542, "Madinah", 22)
        surahMap[59] = SurahDetails(59, "الحشر", "Al-Hashr", "The Exile", 546, "Madinah", 24)
        surahMap[60] = SurahDetails(60, "الممتحنة", "Al-Mumtahana", "The Woman to be Examined", 549, "Madinah", 13)
        surahMap[61] = SurahDetails(61, "الصف", "As-Saff", "The Ranks", 552, "Madinah", 14)
        surahMap[62] = SurahDetails(62, "الجمعة", "Al-Jumu'a", "The Congregation", 553, "Madinah", 11)
        surahMap[63] = SurahDetails(63, "المنافقون", "Al-Munaafiqoon", "The Hypocrites", 555, "Madinah", 11)
        surahMap[64] = SurahDetails(64, "التغابن", "At-Taghaabun", "The Mutual Disillusion", 556, "Madinah", 18)
        surahMap[65] = SurahDetails(65, "الطلاق", "At-Talaaq", "The Divorce", 558, "Madinah", 12)
        surahMap[66] = SurahDetails(66, "التحريم", "At-Tahrim", "The Prohibition", 560, "Madinah", 12)
        surahMap[67] = SurahDetails(67, "الملك", "Al-Mulk", "The Dominion", 562, "Makkah", 30)
        surahMap[68] = SurahDetails(68, "القلم", "Al-Qalam", "The Pen", 565, "Makkah", 52)
        surahMap[69] = SurahDetails(69, "الحاقة", "Al-Haaqqa", "The Reality", 567, "Makkah", 52)
        surahMap[70] = SurahDetails(70, "المعارج", "Al-Ma'aarij", "The Ascending Stairways", 569, "Makkah", 44)
        surahMap[71] = SurahDetails(71, "نوح", "Nooh", "Noah", 571, "Makkah", 28)
        surahMap[72] = SurahDetails(72, "الجن", "Al-Jinn", "The Jinn", 572, "Makkah", 28)
        surahMap[73] = SurahDetails(73, "المزمل", "Al-Muzzammil", "The Enshrouded One", 574, "Makkah", 20)
        surahMap[74] = SurahDetails(74, "المدثر", "Al-Muddaththir", "The Cloaked One", 576, "Makkah", 56)
        surahMap[75] = SurahDetails(75, "القيامة", "Al-Qiyaama", "The Resurrection", 578, "Makkah", 40)
        surahMap[76] = SurahDetails(76, "الانسان", "Al-Insaan", "The Human", 579, "Madinah", 31)
        surahMap[77] = SurahDetails(77, "المرسلات", "Al-Mursalaat", "Those Sent Forth", 581, "Makkah", 50)
        surahMap[78] = SurahDetails(78, "النبإ", "An-Naba", "The Announcement", 582, "Makkah", 40)
        surahMap[79] = SurahDetails(79, "النازعات", "An-Naazi'aat", "Those Who Drag Forth", 584, "Makkah", 46)
        surahMap[80] = SurahDetails(80, "عبس", "Abasa", "He Frowned", 585, "Makkah", 42)
        surahMap[81] = SurahDetails(81, "التكوير", "At-Takwir", "The Overthrowing", 586, "Makkah", 29)
        surahMap[82] = SurahDetails(82, "الإنفطار", "Al-Infitaar", "The Cleaving", 587, "Makkah", 19)
        surahMap[83] = SurahDetails(83, "المطففين", "Al-Mutaffifin", "The Defrauding", 588, "Makkah", 36)
        surahMap[84] = SurahDetails(84, "الإنشقاق", "Al-Inshiqaaq", "The Splitting Open", 589, "Makkah", 25)
        surahMap[85] = SurahDetails(85, "البروج", "Al-Burooj", "The Mansions of the Stars", 590, "Makkah", 22)
        surahMap[86] = SurahDetails(86, "الطارق", "At-Taariq", "The Nightcomer", 591, "Makkah", 17)
        surahMap[87] = SurahDetails(87, "الأعلى", "Al-A'laa", "The Most High", 592, "Makkah", 19)
        surahMap[88] = SurahDetails(88, "الغاشية", "Al-Ghaashiya", "The Overwhelming", 592, "Makkah", 26)
        surahMap[89] = SurahDetails(89, "الفجر", "Al-Fajr", "The Dawn", 593, "Makkah", 30)
        surahMap[90] = SurahDetails(90, "البلد", "Al-Balad", "The City", 594, "Makkah", 20)
        surahMap[91] = SurahDetails(91, "الشمس", "Ash-Shams", "The Sun", 595, "Makkah", 15)
        surahMap[92] = SurahDetails(92, "الليل", "Al-Lail", "The Night", 596, "Makkah", 21)
        surahMap[93] = SurahDetails(93, "الضحى", "Ad-Dhuhaa", "The Morning Hours", 596, "Makkah", 11)
        surahMap[94] = SurahDetails(94, "الشرح", "Ash-Sharh", "The Relief", 596, "Makkah", 8)
        surahMap[95] = SurahDetails(95, "التين", "At-Tin", "The Fig", 597, "Makkah", 8)
        surahMap[96] = SurahDetails(96, "العلق", "Al-Alaq", "The Clot", 597, "Makkah", 19)
        surahMap[97] = SurahDetails(97, "القدر", "Al-Qadr", "The Power", 598, "Makkah", 5)
        surahMap[98] = SurahDetails(98, "البينة", "Al-Bayyina", "The Clear Proof", 599, "Madinah", 8)
        surahMap[99] = SurahDetails(99, "الزلزلة", "Az-Zalzala", "The Earthquake", 599, "Madinah", 8)
        surahMap[100] = SurahDetails(100, "العاديات", "Al-Aadiyaat", "The Courser", 600, "Makkah", 11)
        surahMap[101] = SurahDetails(101, "القارعة", "Al-Qaari'a", "The Calamity", 600, "Makkah", 11)
        surahMap[102] = SurahDetails(102, "التكاثر", "At-Takaathur", "The Rivalry in World Increase", 600, "Makkah", 8)
        surahMap[103] = SurahDetails(103, "العصر", "Al-Asr", "The Declining Day", 601, "Makkah", 3)
        surahMap[104] = SurahDetails(104, "الهمزة", "Al-Humaza", "The Traducer", 601, "Makkah", 9)
        surahMap[105] = SurahDetails(105, "الفيل", "Al-Fil", "The Elephant", 601, "Makkah", 5)
        surahMap[106] = SurahDetails(106, "قريش", "Quraish", "Quraish", 602, "Makkah", 4)
        surahMap[107] = SurahDetails(107, "الماعون", "Al-Maa'un", "The Small Kindnesses", 602, "Makkah", 7)
        surahMap[108] = SurahDetails(108, "الكوثر", "Al-Kawthar", "The Abundance", 602, "Makkah", 3)
        surahMap[109] = SurahDetails(109, "الكافرون", "Al-Kaafiroon", "The Disbelievers", 603, "Makkah", 6)
        surahMap[110] = SurahDetails(110, "النصر", "An-Nasr", "The Divine Support", 603, "Madinah", 3)
        surahMap[111] = SurahDetails(111, "المسد", "Al-Masad", "The Palm Fiber", 603, "Makkah", 5)
        surahMap[112] = SurahDetails(112, "الإخلاص", "Al-Ikhlaas", "The Sincerity", 604, "Makkah", 4)
        surahMap[113] = SurahDetails(113, "الفلق", "Al-Falaq", "The Daybreak", 604, "Makkah", 5)
        surahMap[114] = SurahDetails(114, "الناس", "An-Naas", "Mankind", 604, "Makkah", 6)
    }

    companion object {
        @Volatile
        private var instance: QuranMetadata? = null

        fun getInstance(): QuranMetadata {
            return instance ?: synchronized(this) {
                instance ?: QuranMetadata().also { instance = it }
            }
        }
    }
    // Method to get starting page
    fun getStartingPage(surahIndex: Int): Int {
        return surahMap[surahIndex]?.startingPage ?: -1
    }

    // Additional methods for fetching names, etc.
    fun getSurahDetails(surahIndex: Int): SurahDetails? {
        return surahMap[surahIndex]
    }

    fun getSurahNumberForPage(pageNumber: Int): Int {
        // Get all surahs sorted by starting page
        val sortedSurahs = surahMap.values.sortedBy { it.startingPage }

        // Find the last surah that starts on or before this page
        var targetSurah = sortedSurahs[0] // Default to first surah

        for (surah in sortedSurahs) {
            if (surah.startingPage <= pageNumber) {
                targetSurah = surah
            } else {
                break // Stop when we find a surah that starts after our page
            }
        }

        return targetSurah.surahIndex
    }
}
data class SurahDetails(
    val surahIndex: Int,
    val arabicName: String,
    val englishName: String,
    val translationName: String,
    val startingPage: Int,
    val revelationPlace: String,
    val numberOfVerses: Int
)

package com.example.qurannexus.utils


class QuranMetadata private constructor() {
    private val surahMap: MutableMap<Int, SurahDetails> = HashMap()

    init {
        surahMap[1] = SurahDetails(1, "الفاتحة", "Al-Faatiha", 1)
        surahMap[2] = SurahDetails(2, "البقرة", "Al-Baqara", 2)
        surahMap[3] = SurahDetails(3, "آل عمران", "Aal-i-Imraan", 50)
        surahMap[4] = SurahDetails(4, "النساء", "An-Nisaa", 77)
        surahMap[5] = SurahDetails(5, "المائدة", "Al-Maaida", 107)
        surahMap[6] = SurahDetails(6, "الأنعام", "Al-An'aam", 128)
        surahMap[7] = SurahDetails(7, "الأعراف", "Al-A'raaf", 151)
        surahMap[8] = SurahDetails(8, "الأنفال", "Al-Anfaal", 177)
        surahMap[9] = SurahDetails(9, "التوبة", "At-Tawba", 187)
        surahMap[10] = SurahDetails(10, "يونس", "Yunus", 208)
        surahMap[11] = SurahDetails(11, "هود", "Hud", 222)
        surahMap[12] = SurahDetails(12, "يوسف", "Yusuf", 236)
        surahMap[13] = SurahDetails(13, "الرعد", "Ar-Ra'd", 249)
        surahMap[14] = SurahDetails(14, "ابراهيم", "Ibrahim", 256)
        surahMap[15] = SurahDetails(15, "الحجر", "Al-Hijr", 262)
        surahMap[16] = SurahDetails(16, "النحل", "An-Nahl", 268)
        surahMap[17] = SurahDetails(17, "الإسراء", "Al-Israa", 282)
        surahMap[18] = SurahDetails(18, "الكهف", "Al-Kahf", 294)
        surahMap[19] = SurahDetails(19, "مريم", "Maryam", 305)
        surahMap[20] = SurahDetails(20, "طه", "Taa-Haa", 313)
        surahMap[21] = SurahDetails(21, "الأنبياء", "Al-Anbiyaa", 322)
        surahMap[22] = SurahDetails(22, "الحج", "Al-Hajj", 332)
        surahMap[23] = SurahDetails(23, "المؤمنون", "Al-Muminoon", 342)
        surahMap[24] = SurahDetails(24, "النور", "An-Noor", 350)
        surahMap[25] = SurahDetails(25, "الفرقان", "Al-Furqaan", 360)
        surahMap[26] = SurahDetails(26, "الشعراء", "Ash-Shu'araa", 367)
        surahMap[27] = SurahDetails(27, "النمل", "An-Naml", 377)
        surahMap[28] = SurahDetails(28, "القصص", "Al-Qasas", 386)
        surahMap[29] = SurahDetails(29, "العنكبوت", "Al-Ankaboot", 397)
        surahMap[30] = SurahDetails(30, "الروم", "Ar-Room", 405)
        surahMap[31] = SurahDetails(31, "لقمان", "Luqman", 411)
        surahMap[32] = SurahDetails(32, "السجدة", "As-Sajda", 415)
        surahMap[33] = SurahDetails(33, "الأحزاب", "Al-Ahzaab", 418)
        surahMap[34] = SurahDetails(34, "سبإ", "Saba", 428)
        surahMap[35] = SurahDetails(35, "فاطر", "Faatir", 435)
        surahMap[36] = SurahDetails(36, "يس", "Yaseen", 441)
        surahMap[37] = SurahDetails(37, "الصافات", "As-Saaffaat", 446)
        surahMap[38] = SurahDetails(38, "ص", "Saad", 453)
        surahMap[39] = SurahDetails(39, "الزمر", "Az-Zumar", 459)
        surahMap[40] = SurahDetails(40, "غافر", "Al-Ghaafir", 468)
        surahMap[41] = SurahDetails(41, "فصلت", "Fussilat", 477)
        surahMap[42] = SurahDetails(42, "الشورى", "Ash-Shura", 483)
        surahMap[43] = SurahDetails(43, "الزخرف", "Az-Zukhruf", 490)
        surahMap[44] = SurahDetails(44, "الدخان", "Ad-Dukhaan", 496)
        surahMap[45] = SurahDetails(45, "الجاثية", "Al-Jaathiya", 499)
        surahMap[46] = SurahDetails(46, "الأحقاف", "Al-Ahqaf", 503)
        surahMap[47] = SurahDetails(47, "محمد", "Muhammad", 507)
        surahMap[48] = SurahDetails(48, "الفتح", "Al-Fath", 511)
        surahMap[49] = SurahDetails(49, "الحجرات", "Al-Hujuraat", 516)
        surahMap[50] = SurahDetails(50, "ق", "Qaaf", 518)
        surahMap[51] = SurahDetails(51, "الذاريات", "Adh-Dhaariyat", 521)
        surahMap[52] = SurahDetails(52, "الطور", "At-Tur", 524)
        surahMap[53] = SurahDetails(53, "النجم", "An-Najm", 526)
        surahMap[54] = SurahDetails(54, "القمر", "Al-Qamar", 529)
        surahMap[55] = SurahDetails(55, "الرحمن", "Ar-Rahmaan", 532)
        surahMap[56] = SurahDetails(56, "الواقعة", "Al-Waaqia", 535)
        surahMap[57] = SurahDetails(57, "الحديد", "Al-Hadid", 538)
        surahMap[58] = SurahDetails(58, "المجادلة", "Al-Mujaadila", 542)
        surahMap[59] = SurahDetails(59, "الحشر", "Al-Hashr", 546)
        surahMap[60] = SurahDetails(60, "الممتحنة", "Al-Mumtahana", 549)
        surahMap[61] = SurahDetails(61, "الصف", "As-Saff", 552)
        surahMap[62] = SurahDetails(62, "الجمعة", "Al-Jumu'a", 553)
        surahMap[63] = SurahDetails(63, "المنافقون", "Al-Munaafiqoon", 555)
        surahMap[64] = SurahDetails(64, "التغابن", "At-Taghaabun", 556)
        surahMap[65] = SurahDetails(65, "الطلاق", "At-Talaaq", 558)
        surahMap[66] = SurahDetails(66, "التحريم", "At-Tahrim", 560)
        surahMap[67] = SurahDetails(67, "الملك", "Al-Mulk", 562)
        surahMap[68] = SurahDetails(68, "القلم", "Al-Qalam", 565)
        surahMap[69] = SurahDetails(69, "الحاقة", "Al-Haaqqa", 567)
        surahMap[70] = SurahDetails(70, "المعارج", "Al-Ma'aarij", 569)
        surahMap[71] = SurahDetails(71, "نوح", "Nooh", 571)
        surahMap[72] = SurahDetails(72, "الجن", "Al-Jinn", 572)
        surahMap[73] = SurahDetails(73, "المزمل", "Al-Muzzammil", 574)
        surahMap[74] = SurahDetails(74, "المدثر", "Al-Muddaththir", 576)
        surahMap[75] = SurahDetails(75, "القيامة", "Al-Qiyaama", 578)
        surahMap[76] = SurahDetails(76, "الانسان", "Al-Insaan", 579)
        surahMap[77] = SurahDetails(77, "المرسلات", "Al-Mursalaat", 581)
        surahMap[78] = SurahDetails(78, "النبإ", "An-Naba", 582)
        surahMap[79] = SurahDetails(79, "النازعات", "An-Naazi'aat", 584)
        surahMap[80] = SurahDetails(80, "عبس", "Abasa", 585)
        surahMap[81] = SurahDetails(81, "التكوير", "At-Takwir", 586)
        surahMap[82] = SurahDetails(82, "الإنفطار", "Al-Infitaar", 587)
        surahMap[83] = SurahDetails(83, "المطففين", "Al-Mutaffifin", 588)
        surahMap[84] = SurahDetails(84, "الإنشقاق", "Al-Inshiqaaq", 588)
        surahMap[85] = SurahDetails(85, "البروج", "Al-Burooj", 590)
        surahMap[86] = SurahDetails(86, "الطارق", "At-Taariq", 591)
        surahMap[87] = SurahDetails(87, "الأعلى", "Al-A'laa", 592)
        surahMap[88] = SurahDetails(88, "الغاشية", "Al-Ghaashiya", 592)
        surahMap[89] = SurahDetails(89, "الفجر", "Al-Fajr", 593)
        surahMap[90] = SurahDetails(90, "البلد", "Al-Balad", 593)
        surahMap[91] = SurahDetails(91, "الشمس", "Ash-Shams", 595)
        surahMap[92] = SurahDetails(92, "الليل", "Al-Lail", 596)
        surahMap[93] = SurahDetails(93, "الضحى", "Ad-Dhuhaa", 596)
        surahMap[94] = SurahDetails(94, "الشرح", "Ash-Sharh", 596)
        surahMap[95] = SurahDetails(95, "التين", "At-Tin", 597)
        surahMap[96] = SurahDetails(96, "العلق", "Al-Alaq", 597)
        surahMap[97] = SurahDetails(97, "القدر", "Al-Qadr", 598)
        surahMap[98] = SurahDetails(98, "البينة", "Al-Bayyina", 599)
        surahMap[99] = SurahDetails(99, "الزلزلة", "Az-Zalzala", 599)
        surahMap[100] = SurahDetails(100, "العاديات", "Al-Aadiyaat", 600)
        surahMap[101] = SurahDetails(101, "القارعة", "Al-Qaari'a", 600)
        surahMap[102] = SurahDetails(102, "التكاثر", "At-Takaathur", 600)
        surahMap[103] = SurahDetails(103, "العصر", "Al-Asr", 601)
        surahMap[104] = SurahDetails(104, "الهمزة", "Al-Humaza", 601)
        surahMap[105] = SurahDetails(105, "الفيل", "Al-Fil", 601)
        surahMap[106] = SurahDetails(106, "قريش", "Quraish", 602)
        surahMap[107] = SurahDetails(107, "الماعون", "Al-Maa'un", 602)
        surahMap[108] = SurahDetails(108, "الكوثر", "Al-Kawthar", 602)
        surahMap[109] = SurahDetails(109, "الكافرون", "Al-Kaafiroon", 603)
        surahMap[110] = SurahDetails(110, "النصر", "An-Nasr", 603)
        surahMap[111] = SurahDetails(111, "المسد", "Al-Masad", 603)
        surahMap[112] = SurahDetails(112, "الإخلاص", "Al-Ikhlaas", 604)
        surahMap[113] = SurahDetails(113, "الفلق", "Al-Falaq", 604)
        surahMap[114] = SurahDetails(114, "الناس", "An-Naas", 604)
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
}
data class SurahDetails(
    val surahIndex: Int,
    val arabicName: String,
    val englishName: String,
    val startingPage: Int
)

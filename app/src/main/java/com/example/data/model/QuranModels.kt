package com.example.data.model

data class QuranAyah(
    val numberInSurah: Int,
    val page: Int,
    val juz: Int,
    val textUthmani: String,
    val tafsirMuyassar: String,
    val translationEn: String,
)

data class QuranSurah(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val revelationType: String,
    val ayahs: List<QuranAyah>,
) {
    val revelationArabic: String
        get() = if (revelationType.equals("Meccan", ignoreCase = true)) "مكية" else "مدنية"
}

data class QuranReciter(
    val id: String,
    val arabicName: String,
    val country: String,
    val style: String,
    val narration: String,
    val baseUrl: String,
    val ayahBaseUrl: String,
    val sampleSurahUrl: String,
) {
    fun surahAudioUrl(surahNumber: Int): String {
        val padded = surahNumber.toString().padStart(3, '0')
        return "$baseUrl$padded.mp3"
    }
}

data class DailyWisdom(
    val title: String,
    val text: String,
    val source: String,
    val isHadith: Boolean,
)

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val requirementLabel: String,
    val isUnlocked: Boolean,
    val progress: Float,
)

enum class WirdMode(val arabicLabel: String) {
    PAGES("بالصفحات"),
    JUZ("بالأجزاء"),
    MINUTES("بالدقائق");
}

enum class QuranReadingUnit(val arabicLabel: String) {
    SURAH("سورة"),
    JUZ("جزء");
}

data class QuranChapterProgress(
    val dateIso: String,
    val unit: QuranReadingUnit,
    val unitNumber: Int,
    val title: String,
    val isRead: Boolean,
    val pagesEstimate: Int = 0,
    val updatedAt: Long = 0L,
)

data class QuranJuzInfo(
    val juzNumber: Int,
    val arabicTitle: String,
    val startSurahArabic: String,
    val startAyah: Int,
)

object QuranJuzCatalog {
    val allJuz: List<QuranJuzInfo> = listOf(
        QuranJuzInfo(1, "الجزء الأول (الم)", "الفاتحة", 1),
        QuranJuzInfo(2, "الجزء الثاني (سيقول)", "البقرة", 142),
        QuranJuzInfo(3, "الجزء الثالث (تلك الرسل)", "البقرة", 253),
        QuranJuzInfo(4, "الجزء الرابع (لن تنالوا)", "آل عمران", 93),
        QuranJuzInfo(5, "الجزء الخامس (والمحصنات)", "النساء", 24),
        QuranJuzInfo(6, "الجزء السادس (لا يحب الله)", "النساء", 148),
        QuranJuzInfo(7, "الجزء السابع (وإذا سمعوا)", "المائدة", 82),
        QuranJuzInfo(8, "الجزء الثامن (ولو أننا)", "الأنعام", 111),
        QuranJuzInfo(9, "الجزء التاسع (قال الملأ)", "الأعراف", 88),
        QuranJuzInfo(10, "الجزء العاشر (واعلموا)", "الأنفال", 41),
        QuranJuzInfo(11, "الجزء الحادي عشر (يعتذرون)", "التوبة", 93),
        QuranJuzInfo(12, "الجزء الثاني عشر (وما من دابة)", "هود", 6),
        QuranJuzInfo(13, "الجزء الثالث عشر (وما أبرئ)", "يوسف", 53),
        QuranJuzInfo(14, "الجزء الرابع عشر (ربما)", "الحجر", 1),
        QuranJuzInfo(15, "الجزء الخامس عشر (سبحان الذي)", "الإسراء", 1),
        QuranJuzInfo(16, "الجزء السادس عشر (قال ألم)", "الكهف", 75),
        QuranJuzInfo(17, "الجزء السابع عشر (اقترب للناس)", "الأنبياء", 1),
        QuranJuzInfo(18, "الجزء الثامن عشر (قد أفلح)", "المؤمنون", 1),
        QuranJuzInfo(19, "الجزء التاسع عشر (وقال الذين)", "الفرقان", 21),
        QuranJuzInfo(20, "الجزء العشرون (أمن خلق)", "النمل", 56),
        QuranJuzInfo(21, "الجزء الحادي والعشرون (اتل ما أوحي)", "العنكبوت", 46),
        QuranJuzInfo(22, "الجزء الثاني والعشرون (ومن يقنت)", "الأحزاب", 31),
        QuranJuzInfo(23, "الجزء الثالث والعشرون (وما لي)", "يس", 28),
        QuranJuzInfo(24, "الجزء الرابع والعشرون (فمن أظلم)", "الزمر", 32),
        QuranJuzInfo(25, "الجزء الخامس والعشرون (إليه يرد)", "فصلت", 47),
        QuranJuzInfo(26, "الجزء السادس والعشرون (حم)", "الأحقاف", 1),
        QuranJuzInfo(27, "الجزء السابع والعشرون (قال فما خطبكم)", "الذاريات", 31),
        QuranJuzInfo(28, "الجزء الثامن والعشرون (قد سمع الله)", "المجادلة", 1),
        QuranJuzInfo(29, "الجزء التاسع والعشرون (تبارك الذي)", "الملك", 1),
        QuranJuzInfo(30, "الجزء الثلاثون (عمّ يتساءلون)", "النبأ", 1),
    )
}

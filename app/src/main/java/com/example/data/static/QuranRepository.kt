package com.example.data.static

import android.content.Context
import com.example.data.model.QuranAyah
import com.example.data.model.QuranSurah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.zip.GZIPInputStream

class QuranRepository(private val context: Context) {
    @Volatile
    private var cachedSurahs: List<QuranSurah>? = null

    suspend fun getAllSurahs(): List<QuranSurah> {
        cachedSurahs?.let { return it }
        return withContext(Dispatchers.IO) {
            cachedSurahs?.let { return@withContext it }
            val jsonText = context.assets.open("quran_full.dat").use { raw ->
                GZIPInputStream(raw).bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            val array = JSONArray(jsonText)
            val result = ArrayList<QuranSurah>(array.length())
            for (i in 0 until array.length()) {
                val sObj = array.getJSONObject(i)
                val aArr = sObj.getJSONArray("a")
                val ayahs = ArrayList<QuranAyah>(aArr.length())
                for (j in 0 until aArr.length()) {
                    val aObj = aArr.getJSONObject(j)
                    ayahs.add(
                        QuranAyah(
                            numberInSurah = aObj.getInt("n"),
                            page = aObj.getInt("p"),
                            juz = aObj.getInt("j"),
                            textUthmani = aObj.getString("t"),
                            tafsirMuyassar = aObj.getString("m"),
                            translationEn = aObj.getString("e"),
                        )
                    )
                }
                result.add(
                    QuranSurah(
                        number = sObj.getInt("n"),
                        arabicName = sObj.getString("ar"),
                        englishName = sObj.getString("en"),
                        revelationType = sObj.getString("rev"),
                        ayahs = ayahs,
                    )
                )
            }
            cachedSurahs = result
            result
        }
    }
}

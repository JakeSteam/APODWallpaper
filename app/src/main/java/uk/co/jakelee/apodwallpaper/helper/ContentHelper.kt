package uk.co.jakelee.apodwallpaper.helper

import android.content.Context
import android.preference.PreferenceManager
import uk.co.jakelee.apodwallpaper.api.ContentItem

class ContentHelper(val context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    enum class ContentItemPrefs { Title, Desc, Image, ImageHd, Copyright, IsImage }

    fun saveContentData(response: ContentItem) = prefs.edit()
        .putString("${response.date}_${ContentItemPrefs.Title.name}", response.title)
        .putString("${response.date}_${ContentItemPrefs.Desc.name}", response.desc)
        .putString("${response.date}_${ContentItemPrefs.Image.name}", response.imageUrl)
        .putString("${response.date}_${ContentItemPrefs.ImageHd.name}", response.imageUrlHd)
        .putString("${response.date}_${ContentItemPrefs.Copyright.name}", response.copyright)
        .putBoolean("${response.date}_${ContentItemPrefs.IsImage.name}", response.isImage)
        .apply()

    fun getContentData(date: String) = ContentItem(
        date,
        prefs.getString("${date}_${ContentItemPrefs.Title.name}", "")!!,
        prefs.getString("${date}_${ContentItemPrefs.Desc.name}", "")!!,
        prefs.getString("${date}_${ContentItemPrefs.Image.name}", "")!!,
        prefs.getString("${date}_${ContentItemPrefs.ImageHd.name}", "")!!,
        prefs.getString("${date}_${ContentItemPrefs.Copyright.name}", "")!!,
        prefs.getBoolean("${date}_${ContentItemPrefs.IsImage.name}", true)
    )
}
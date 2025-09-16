package com.example.gpt011024

import android.net.Uri
import android.util.Log
import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class Converters {

    private val gson = GsonProvider.gson

    @TypeConverter
    fun fromPhotoList(photos: List<Photo>?): String? {
        if (photos == null) {
            return null
        }
        return gson.toJson(photos)
    }

    @TypeConverter
    fun toPhotoList(photosJson: String?): List<Photo> {
        if (photosJson.isNullOrEmpty()) {
            return emptyList()
        }
        return try {
            val listType: Type = object : TypeToken<List<Photo>>() {}.type
            gson.fromJson(photosJson, listType) ?: emptyList()
        } catch (e: Exception) {
            Log.e("Converters", "Error deserializing photos: $e")
            emptyList()
        }
    }

    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun toUri(uriString: String?): Uri? {
        return uriString?.let { Uri.parse(it) }
    }
}
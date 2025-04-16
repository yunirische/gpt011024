package com.example.gpt011024

import android.net.Uri
import android.util.Log
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URI
import java.lang.reflect.Type
class Converters {

    @TypeConverter
    fun fromPhotoList(photos: List<Photo>?): String {
        return Gson().toJson(photos)
    }

    @TypeConverter
    fun toPhotoList(photosJson: String?): List<Photo> {
        return if (photosJson.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                val listType: Type = object : TypeToken<List<Photo>>() {}.type
                Gson().fromJson(photosJson, listType) ?: emptyList()
            } catch (e: Exception) {
                Log.e("Converters", "Error deserializing photos: $e")
                emptyList()
            }
        }
    }

    @TypeConverter
    fun fromUri(uri: Uri?): String?{
        return uri?.toString()
    }

    @TypeConverter
    fun toUri(uriString: String?): Uri?{
     return uriString?.let { Uri.parse(it) }
    }
}
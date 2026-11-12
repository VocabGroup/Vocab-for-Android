package com.example.data.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Personal appearance only. Images are copied into private storage, never shared. */
class CourseCoverStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("course_covers", Context.MODE_PRIVATE)
    private val directory get() = File(context.filesDir, "course-covers").apply { mkdirs() }
    private fun key(course: String) = MessageDigest.getInstance("SHA-256").digest(course.toByteArray()).joinToString("") { "%02x".format(it) }
    fun value(course: String): String = preferences.getString(key(course), "preset:sky") ?: "preset:sky"
    fun file(value: String): File? = value.removePrefix("file:").takeIf { value.startsWith("file:") && it.matches(Regex("[a-f0-9]{64}-[0-9]+\\.jpg")) }?.let { File(directory, it).takeIf(File::exists) }
    fun set(course: String, value: String) {
        require(value in listOf("preset:sky", "preset:ice", "preset:cornflower", "preset:sea") || file(value) != null)
        val previous = this.value(course)
        check(preferences.edit().putString(key(course), value).commit()) { "封面無法儲存" }
        if (previous != value) file(previous)?.delete()
    }
    fun rename(old: String, new: String) {
        if (old == new) return
        val value = value(old)
        check(preferences.edit().remove(key(old)).putString(key(new), value).commit())
    }
    fun clear() { preferences.edit().clear().apply(); directory.listFiles()?.forEach { it.delete() } }
    suspend fun preparePhoto(course: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "無法讀取圖片，請選擇 JPEG 或 PNG" }
        val options = BitmapFactory.Options().apply { inSampleSize = 1 }
        while (bounds.outWidth / options.inSampleSize > 1800 || bounds.outHeight / options.inSampleSize > 1800) options.inSampleSize *= 2
        val decoded = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) } ?: error("無法讀取圖片")
        val orientation = runCatching { context.contentResolver.openInputStream(uri).use { android.media.ExifInterface(it!!).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) } }.getOrDefault(1)
        val matrix = android.graphics.Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f)
                3 -> setRotate(180f)
                4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }
                6 -> setRotate(90f)
                7 -> { setRotate(-90f); postScale(-1f, 1f) }
                8 -> setRotate(-90f)
            }
        }
        val source = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val output = Bitmap.createBitmap(600, 760, Bitmap.Config.ARGB_8888)
        val scale = maxOf(600f / source.width, 760f / source.height)
        val left = (600 - source.width * scale) / 2; val top = (760 - source.height * scale) / 2
        Canvas(output).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(source, null, RectF(left, top, left + source.width * scale, top + source.height * scale), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)) }
        val file = File(directory, "${key(course)}-${System.nanoTime()}.jpg")
        try { file.outputStream().use { check(output.compress(Bitmap.CompressFormat.JPEG, 82, it)) }; "file:${file.name}" }
        catch (error: Exception) { file.delete(); throw error }
        finally { output.recycle(); if (source !== decoded) source.recycle(); decoded.recycle() }
    }
    fun discard(value: String) { file(value)?.delete() }
}

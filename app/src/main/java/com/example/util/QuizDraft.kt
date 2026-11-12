package com.example.util

import org.json.JSONArray
import org.json.JSONObject

/** Content-only JSON checkpoints stay in private app storage, never in share exports. */
fun JSONObject.longList(key:String):List<Long> = optJSONArray(key)?.let{array->(0 until array.length()).map{array.getLong(it)}}?:emptyList()
fun JSONObject.intList(key:String):List<Int> = optJSONArray(key)?.let{array->(0 until array.length()).map{array.getInt(it)}}?:emptyList()
fun JSONObject.stringList(key:String):List<String> = optJSONArray(key)?.let{array->(0 until array.length()).map{array.getString(it)}}?:emptyList()
fun JSONObject.list(key:String,values:Collection<*>):JSONObject=put(key,JSONArray(values))

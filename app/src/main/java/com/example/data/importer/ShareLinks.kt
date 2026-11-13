package com.example.data.importer

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PublishedShare(val url:String,val id:String,val deleteToken:String,val expiresAt:Long)
data class ReceivedShare(val folders:List<SharedFolder>,val expiresAt:Long)
object ShareLinks {
    const val ORIGIN = "https://vocab-adam.pages.dev"
    private val client=OkHttpClient.Builder().callTimeout(25,TimeUnit.SECONDS).followRedirects(false).build()
    fun canonicalUrl(value:String):String? = runCatching {
        val uri=java.net.URI(value.trim())
        val key=if(uri.scheme=="vocab"&&uri.host=="share"&&uri.rawUserInfo==null&&uri.port==-1&&uri.query==null) uri.path.removePrefix("/") else id(value)
        require(key.matches(Regex("[a-f0-9]{48}")))
        "$ORIGIN/share#$key"
    }.getOrNull()
    private fun id(url:String):String {
        val uri=java.net.URI(url.trim())
        require(uri.scheme=="https"&&uri.host=="vocab-adam.pages.dev"&&uri.rawUserInfo==null&&uri.port==-1&&uri.path=="/share") {"請貼上 Vocab 的分享網址"}
        return uri.fragment.orEmpty().also{require(it.matches(Regex("[a-f0-9]{48}"))){"分享網址不完整"}}
    }
    private suspend fun json(request:Request):JSONObject=withContext(Dispatchers.IO){
        client.newCall(request).execute().use { response ->
            val body=response.body?:error("分享服務沒有回傳資料")
            val bytes=body.byteStream().use{stream->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=stream.read(buffer);if(n<0)break;require(out.size()+n<=600*1024){"分享內容過大"};out.write(buffer,0,n)};out.toByteArray()}
            require(bytes.size<=600*1024){"分享內容過大"}
            val value=runCatching{JSONObject(String(bytes,Charsets.UTF_8))}.getOrElse{error("分享服務暫時無法使用")}
            require(response.isSuccessful){value.optString("error","分享失敗（HTTP ${response.code}）")}
            value
        }
    }
    suspend fun publish(raw:String):PublishedShare {
        require(raw.toByteArray().size<=512*1024){"網址分享最多 512 KB，請改用 JSON 檔案分享"}
        val value=json(Request.Builder().url("$ORIGIN/api/shares").post(raw.toRequestBody("application/json".toMediaType())).build())
        val url=value.getString("url");val shareId=id(url)
        return PublishedShare(url,shareId,value.getString("deleteToken"),value.getLong("expiresAt"))
    }
    suspend fun loadDetails(url:String):ReceivedShare {
        val value=json(Request.Builder().url("$ORIGIN/api/shares/${id(url)}").build())
        val expiry=value.getLong("expiresAt")
        require(expiry>System.currentTimeMillis()) {"分享已到期，請向分享者索取新連結"}
        return ReceivedShare(FolderShare.parse(value.getJSONObject("share").toString()),expiry)
    }
    suspend fun load(url:String):List<SharedFolder> = loadDetails(url).folders
    suspend fun revoke(share:PublishedShare){json(Request.Builder().url("$ORIGIN/api/shares/${id(share.url)}").header("Authorization","Bearer ${share.deleteToken}").delete().build())}
    fun qr(url:String):Bitmap {
        id(url)
        val matrix=QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,600,600)
        val pixels=IntArray(600*600){i->if(matrix[i%600,i/600])android.graphics.Color.BLACK else android.graphics.Color.WHITE}
        return Bitmap.createBitmap(pixels,600,600,Bitmap.Config.ARGB_8888)
    }
}

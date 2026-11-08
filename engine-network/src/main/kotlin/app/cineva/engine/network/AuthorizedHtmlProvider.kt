package app.cineva.engine.network
import app.cineva.engine.core.model.*
import app.cineva.engine.core.provider.SourceProvider
import app.cineva.engine.core.security.SourceAccessPolicy
import org.jsoup.Jsoup
import java.net.URI
import java.security.MessageDigest
import java.util.Locale
class AuthorizedHtmlProvider(override val descriptor:ProviderDescriptor, private val http:SafeHttpClient,
    private val searchUrl:(MediaRequest)->String, private val rules:List<HtmlSelectorRule>,
    private val policy:SourceAccessPolicy=SourceAccessPolicy(), private val requestHeaders:Map<String,String> = emptyMap()):SourceProvider {
    init { require(rules.isNotEmpty()) }
    override suspend fun search(request:MediaRequest):List<StreamCandidate> {
        val page=searchUrl(request); policy.requireAllowed(page,descriptor)
        val doc=Jsoup.parse(http.getText(page,descriptor,requestHeaders),page)
        return rules.flatMap { rule -> doc.select(rule.cssSelector).mapNotNull { node ->
            val raw=if(rule.attribute.equals("text",true)) node.text() else node.attr(rule.attribute)
            if(raw.isBlank()) return@mapNotNull null
            val url=runCatching { URI(page).resolve(raw.trim()).toString() }.getOrNull() ?: return@mapNotNull null
            if(!policy.isAllowed(url,descriptor)) return@mapNotNull null
            val mime=rule.mimeType ?: inferMime(url) ?: return@mapNotNull null
            if(!(mime.startsWith("video/") || url.substringBefore('?').endsWith(".m3u8") || url.substringBefore('?').endsWith(".mpd"))) return@mapNotNull null
            StreamCandidate("${descriptor.id}:${hash(url)}",descriptor.id,descriptor.displayName,url,rule.quality ?: quality(url),mime,requestHeaders,page)
        }}.distinctBy { it.url }.take(descriptor.maxResults)
    }
    private fun inferMime(url:String):String?=when(URI(url).path.lowercase(Locale.ROOT).substringAfterLast('.')) {
        "m3u8"->"application/vnd.apple.mpegurl"; "mpd"->"application/dash+xml"; "mp4","m4v"->"video/mp4"; "webm"->"video/webm"; else->null
    }
    private fun quality(url:String):VideoQuality { val s=url.lowercase(Locale.ROOT); return when {
        Regex("(2160|4k|uhd)").containsMatchIn(s)->VideoQuality.UHD_2160
        Regex("(1080|full.?hd)").containsMatchIn(s)->VideoQuality.FULL_HD_1080
        Regex("(720|hd)").containsMatchIn(s)->VideoQuality.HD_720
        Regex("(480|sd)").containsMatchIn(s)->VideoQuality.SD_480
        else->VideoQuality.UNKNOWN
    }}
    private fun hash(v:String)=MessageDigest.getInstance("SHA-256").digest(v.toByteArray()).take(8).joinToString(""){"%02x".format(it)}
}
data class HtmlSelectorRule(val cssSelector:String,val attribute:String="href",val mimeType:String?=null,val quality:VideoQuality?=null) {
    init { require(cssSelector.isNotBlank() && attribute.isNotBlank()) }
}

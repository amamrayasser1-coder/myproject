package app.cineva.engine.network
import app.cineva.engine.core.model.ProviderDescriptor
import app.cineva.engine.core.security.SourceAccessPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.Buffer
import java.io.IOException
import java.net.*
import java.util.concurrent.TimeUnit
class SafeHttpClient(private val policy: SourceAccessPolicy = SourceAccessPolicy(), connectTimeoutSeconds: Long = 8,
    readTimeoutSeconds: Long = 12, private val maxRedirects: Int = 3, private val maxTextBytes: Long = 2L*1024*1024) {
    private val client = OkHttpClient.Builder().dns(PublicDns).followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).connectTimeout(connectTimeoutSeconds,TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds,TimeUnit.SECONDS).callTimeout(connectTimeoutSeconds+readTimeoutSeconds,TimeUnit.SECONDS).build()
    init { require(connectTimeoutSeconds>0 && readTimeoutSeconds>0); require(maxRedirects in 0..5 && maxTextBytes in 1024..10L*1024*1024) }
    suspend fun getText(url:String, descriptor:ProviderDescriptor, headers:Map<String,String> = emptyMap()):String = withContext(Dispatchers.IO) {
        var current=url
        for (hop in 0..maxRedirects) {
            policy.requireAllowed(current,descriptor)
            val request=Request.Builder().url(current).get()
            headers.forEach { (name,value) ->
                require(name.isNotBlank() && value.none { it=='\r' || it=='\n' })
                if (name.lowercase() !in BLOCKED) request.header(name,value)
            }
            var redirect:String?=null
            var text:String?=null
            client.newCall(request.build()).execute().use { response ->
                if (response.code in REDIRECTS) {
                    if (hop==maxRedirects) throw IOException("Redirect limit exceeded")
                    redirect=URI(current).resolve(response.header("Location") ?: throw IOException("Missing Location")).toString()
                } else {
                    if (!response.isSuccessful) throw IOException("HTTP status ${response.code}")
                    val body=response.body ?: throw IOException("Empty body")
                    if (body.contentLength()>maxTextBytes) throw IOException("Response too large")
                    val sink=Buffer(); val source=body.source()
                    while (true) { val n=source.read(sink,8192); if(n==-1L) break; if(sink.size>maxTextBytes) throw IOException("Response too large") }
                    text=sink.readUtf8()
                }
            }
            if(redirect!=null) { current=redirect!!; policy.requireAllowed(current,descriptor) }
            else return@withContext text ?: ""
        }
        throw IOException("Redirect resolution failed")
    }
    private object PublicDns:Dns {
        override fun lookup(hostname:String):List<InetAddress> {
            val found=Dns.SYSTEM.lookup(hostname)
            if(found.isEmpty() || found.any { a ->
                a.isAnyLocalAddress || a.isLoopbackAddress || a.isLinkLocalAddress || a.isSiteLocalAddress || a.isMulticastAddress ||
                (a is Inet4Address && (a.address[0].toInt() and 255==100) && (a.address[1].toInt() and 255 in 64..127))
            }) throw UnknownHostException("Non-public address")
            return found
        }
    }
    private companion object {
        val REDIRECTS=setOf(300,301,302,303,307,308)
        val BLOCKED=setOf("connection","content-length","host","keep-alive","proxy-authenticate","proxy-authorization","te","trailer","transfer-encoding","upgrade")
    }
}

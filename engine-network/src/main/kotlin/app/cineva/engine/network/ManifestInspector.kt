package app.cineva.engine.network

import app.cineva.engine.core.model.ProviderDescriptor
import app.cineva.engine.core.security.SourceAccessPolicy
import org.w3c.dom.Element
import java.io.StringReader
import java.net.URI
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

data class ManifestResource(val uri: String, val kind: Kind) {
    enum class Kind { PLAYLIST, SEGMENT, INIT, SUBTITLE, KEY, OTHER }
}
data class ManifestInspection(val resources: List<ManifestResource>, val isMasterPlaylist: Boolean)

class ManifestInspector(private val policy: SourceAccessPolicy = SourceAccessPolicy(), private val maxResources: Int = 20_000) {
    init { require(maxResources in 1..100_000) }

    fun inspectHls(content: String, manifestUrl: String, descriptor: ProviderDescriptor): ManifestInspection {
        policy.requireAllowed(manifestUrl, descriptor)
        require(content.length <= 4 * 1024 * 1024) { "HLS manifest too large." }
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        require(lines.firstOrNull() == "#EXTM3U") { "Invalid HLS manifest." }
        val resources = mutableListOf<ManifestResource>()
        var nextVariant = false
        var master = false
        fun add(raw: String, kind: ManifestResource.Kind) {
            if (raw.isBlank() || raw.startsWith("data:", true)) return
            val uri = URI(manifestUrl).resolve(raw.trim().trim('"')).toString()
            policy.requireAllowed(uri, descriptor)
            resources += ManifestResource(uri, kind)
            require(resources.size <= maxResources) { "Manifest resource limit exceeded." }
        }
        for (line in lines.drop(1)) {
            if (line.startsWith("#EXT-X-STREAM-INF")) { nextVariant = true; master = true; continue }
            if (line.startsWith("#EXT-X-MEDIA:")) {
                val attrs = attributes(line.substringAfter(':'))
                attrs["URI"]?.let { add(it, if (attrs["TYPE"] == "SUBTITLES") ManifestResource.Kind.SUBTITLE else ManifestResource.Kind.PLAYLIST) }
            } else if (line.startsWith("#EXT-X-KEY:") || line.startsWith("#EXT-X-SESSION-KEY:")) {
                val attrs = attributes(line.substringAfter(':'))
                attrs["URI"]?.let { add(it, ManifestResource.Kind.KEY) }
            } else if (line.startsWith("#EXT-X-MAP:")) {
                attributes(line.substringAfter(':'))["URI"]?.let { add(it, ManifestResource.Kind.INIT) }
            } else if (!line.startsWith("#")) {
                add(line, if (nextVariant) ManifestResource.Kind.PLAYLIST else ManifestResource.Kind.SEGMENT)
                nextVariant = false
            }
        }
        return ManifestInspection(resources.distinctBy { it.uri }, master)
    }

    fun inspectDash(content: String, manifestUrl: String, descriptor: ProviderDescriptor): ManifestInspection {
        policy.requireAllowed(manifestUrl, descriptor)
        require(content.length <= 4 * 1024 * 1024) { "DASH manifest too large." }
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(content)))
        val root = doc.documentElement
        require(root.localName == "MPD" || root.tagName.substringAfter(':') == "MPD") { "Invalid DASH MPD." }
        val base = root.getElementsByTagName("BaseURL")
        val resources = mutableListOf<ManifestResource>()
        for (i in 0 until base.length) {
            val raw = base.item(i).textContent.trim()
            if (raw.isNotEmpty()) {
                val uri = URI(manifestUrl).resolve(raw).toString()
                policy.requireAllowed(uri, descriptor)
                resources += ManifestResource(uri, ManifestResource.Kind.OTHER)
            }
        }
        val elements = root.getElementsByTagName("*")
        for (i in 0 until elements.length) {
            val node = elements.item(i) as? Element ?: continue
            for (attr in listOf("media", "initialization", "sourceURL")) {
                if (node.hasAttribute(attr)) {
                    val raw = node.getAttribute(attr)
                    if (raw.isNotBlank() && !raw.contains('$')) {
                        val uri = URI(manifestUrl).resolve(raw).toString()
                        policy.requireAllowed(uri, descriptor)
                        resources += ManifestResource(uri, if (attr == "initialization") ManifestResource.Kind.INIT else ManifestResource.Kind.SEGMENT)
                    }
                }
            }
            require(resources.size <= maxResources) { "Manifest resource limit exceeded." }
        }
        return ManifestInspection(resources.distinctBy { it.uri }, false)
    }

    private fun attributes(raw: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val regex = Regex("""([A-Z0-9-]+)=("[^"]*"|[^,]*)""")
        regex.findAll(raw).forEach { result[it.groupValues[1]] = it.groupValues[2].trim('"') }
        return result
    }
}

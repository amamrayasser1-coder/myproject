package app.cineva.engine.network

import app.cineva.engine.core.model.ProviderDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ManifestInspectorTest {
    private val descriptor = ProviderDescriptor("hls-test", "HLS Test", setOf("media.example.org"))
    private val inspector = ManifestInspector()

    @Test fun parsesHlsVariantsSegmentsAndSubtitles() {
        val text = """#EXTM3U
#EXT-X-STREAM-INF:BANDWIDTH=2000000
720/index.m3u8
#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="English",URI="subs/en.vtt"
"""
        val result = inspector.inspectHls(text, "https://media.example.org/master.m3u8", descriptor)
        assertTrue(result.isMasterPlaylist)
        assertEquals(2, result.resources.size)
        assertTrue(result.resources.any { it.uri.endsWith("subs/en.vtt") })
    }

    @Test fun rejectsForeignHlsSegmentHost() {
        assertFailsWith<IllegalArgumentException> {
            inspector.inspectHls("#EXTM3U\nhttps://attacker.test/segment.ts", "https://media.example.org/a.m3u8", descriptor)
        }
    }

    @Test fun parsesDashAndRejectsDoctype() {
        val xml = """<MPD xmlns="urn:mpeg:dash:schema:mpd:2011"><Period><SegmentTemplate media="chunk-${'$'}Number${'$'}.m4s" initialization="init.mp4"/></Period></MPD>"""
        val result = inspector.inspectDash(xml, "https://media.example.org/manifest.mpd", descriptor)
        assertTrue(result.resources.any { it.uri.endsWith("init.mp4") })
        assertFailsWith<Exception> {
            inspector.inspectDash("""<!DOCTYPE x [<!ENTITY e SYSTEM "file:///etc/passwd">]><MPD>&e;</MPD>""",
                "https://media.example.org/manifest.mpd", descriptor)
        }
    }
}

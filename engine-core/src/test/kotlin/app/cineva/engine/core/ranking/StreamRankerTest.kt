package app.cineva.engine.core.ranking
import app.cineva.engine.core.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
class StreamRankerTest {
    @Test fun prefersLanguageThenQualityAndDropsExpired() {
        fun item(id:String,q:VideoQuality,lang:String?=null,expiry:Long?=null)=StreamCandidate(
            id,id,id,"https://$id.example.org/video.m3u8",q,
            subtitles=lang?.let { listOf(SubtitleTrack("https://$id.example.org/$it.vtt",language=it)) } ?: emptyList(),
            expiresAtEpochMillis=expiry)
        val result=StreamRanker().rank(listOf(
            item("fr",VideoQuality.FULL_HD_1080,"fr"),
            item("en",VideoQuality.FULL_HD_1080,"en"),
            item("old",VideoQuality.UHD_2160,expiry=10),
            item("hd",VideoQuality.HD_720)
        ),MediaRequest("Example",preferredLanguages=listOf("en")),100)
        assertEquals(listOf("en","fr","hd"),result.map { it.id })
    }
}

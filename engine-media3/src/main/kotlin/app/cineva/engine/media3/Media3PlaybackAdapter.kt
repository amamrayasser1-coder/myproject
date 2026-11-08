package app.cineva.engine.media3
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import app.cineva.engine.core.model.*
class Media3PlaybackAdapter(private val context:Context,private val userAgent:String="Cineva/1.0") {
    fun toMediaItem(candidate:StreamCandidate):MediaItem {
        val subtitles=candidate.subtitles.mapNotNull(::subtitleConfiguration)
        val builder=MediaItem.Builder().setMediaId(candidate.id).setUri(candidate.url).setSubtitleConfigurations(subtitles)
        candidate.mimeType?.let { builder.setMimeType(it) }
        return builder.build()
    }
    fun toMediaSourceFactory(candidate:StreamCandidate):DefaultMediaSourceFactory {
        val headers=LinkedHashMap(candidate.headers); candidate.referer?.let { headers.putIfAbsent("Referer",it) }
        val http=DefaultHttpDataSource.Factory().setUserAgent(userAgent).setAllowCrossProtocolRedirects(false).setDefaultRequestProperties(headers)
        return DefaultMediaSourceFactory(context).setDataSourceFactory(http)
    }
    private fun subtitleConfiguration(track:SubtitleTrack):MediaItem.SubtitleConfiguration? {
        val mime=track.mimeType ?: when(track.format) {
            SubtitleFormat.WEBVTT->MimeTypes.TEXT_VTT; SubtitleFormat.SUBRIP->MimeTypes.APPLICATION_SUBRIP
            SubtitleFormat.SSA->MimeTypes.TEXT_SSA; SubtitleFormat.TTML->MimeTypes.APPLICATION_TTML
            SubtitleFormat.UNKNOWN->return null
        }
        return MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url)).setMimeType(mime)
            .setLanguage(track.language).setLabel(track.label)
            .setSelectionFlags(if(track.isDefault) C.SELECTION_FLAG_DEFAULT else 0).build()
    }
}

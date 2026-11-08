package app.cineva.engine.core.provider
import app.cineva.engine.core.model.MediaRequest
import app.cineva.engine.core.model.ProviderDescriptor
import app.cineva.engine.core.model.StreamCandidate
interface SourceProvider {
    val descriptor: ProviderDescriptor
    suspend fun search(request: MediaRequest): List<StreamCandidate>
}

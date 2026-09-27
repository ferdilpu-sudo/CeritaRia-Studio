package com.flyonz.ceritaria.studio.core.upload

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import javax.inject.Inject

class VideoTransferSourceResolver @Inject constructor() {
    fun resolve(job: VideoJob): VideoTransferSource {
        if (!job.needsEncoding) {
            require(job.sourceMetadata.sizeBytes > 0L) {
                "Source video size is unknown."
            }
            return VideoTransferSource(
                uri = job.sourceUri,
                sizeBytes = job.sourceMetadata.sizeBytes,
            )
        }

        require(job.encodingStatus == VideoEncodingStatus.READY) {
            "Video encoding is not ready."
        }
        val encodedUri = requireNotNull(job.encodedLocalUri) {
            "Encoded output URI is missing."
        }
        require(job.totalBytes > 0L) {
            "Encoded output size is unknown."
        }
        return VideoTransferSource(
            uri = encodedUri,
            sizeBytes = job.totalBytes,
        )
    }
}

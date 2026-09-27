package com.flyonz.ceritaria.studio.core.upload.execution

object VideoUploadExecutionIds {
    fun systemJobId(jobId: String): Int =
        UIDT_JOB_ID_BASE + (jobId.hashCode() and ID_MASK)

    fun notificationId(jobId: String): Int =
        NOTIFICATION_ID_BASE + (jobId.hashCode() and ID_MASK)

    fun workName(jobId: String): String = "video-upload-" + jobId

    private const val ID_MASK = 0x000fffff
    private const val UIDT_JOB_ID_BASE = 0x12000000
    private const val NOTIFICATION_ID_BASE = 0x23000000
}

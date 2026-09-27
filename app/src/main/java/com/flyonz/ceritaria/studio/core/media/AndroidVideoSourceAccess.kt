package com.flyonz.ceritaria.studio.core.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidVideoSourceAccess @Inject constructor(
    @ApplicationContext private val context: Context,
) : VideoSourceAccess {
    override fun persistReadAccess(sourceUri: String) {
        val uri = Uri.parse(sourceUri)
        val alreadyPersisted = context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission
        }
        if (alreadyPersisted) return

        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (error: SecurityException) {
            throw VideoSourceAccessException(error)
        }
    }
}

class VideoSourceAccessException(
    cause: Throwable,
) : IllegalStateException("Video source access could not be persisted.", cause)

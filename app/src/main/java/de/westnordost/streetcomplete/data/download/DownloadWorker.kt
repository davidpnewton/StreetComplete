package de.westnordost.streetcomplete.data.download

import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.data.download.tiles.TilesRect
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.data.sync.createSyncNotification
import de.westnordost.streetcomplete.util.logs.Log
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Downloads all quests and tiles in a given area asynchronously.
 *
 * Generally, starting a new download cancels the old one. This is a feature; Consideration:
 * If the user requests a new area to be downloaded, he'll generally be more interested in his last
 * request than any request he made earlier and he wants that as fast as possible.
 */
class DownloadWorker(
    private val downloader: Downloader,
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationId = ApplicationConstants.NOTIFICATIONS_ID_SYNC
        val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(id)
        val notification = createSyncNotification(context, cancelIntent)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    override suspend fun doWork(): Result {
        val bbox: BoundingBox = inputData.getString(ARG_BBOX)?.let { Json.decodeFromString(it) }
            ?: return Result.failure()

        try {
            val isPriorityDownload = inputData.getBoolean(ARG_IS_PRIORITY, false)
            downloader.download(bbox, isPriorityDownload)
        } catch (e: CancellationException) {
            Log.i(TAG, "Download cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Unable to download", e)
            return Result.failure()
        }

        return Result.success()
    }

    companion object {
        const val TAG = "Download"

        private const val ARG_BBOX = "bbox"
        private const val ARG_IS_PRIORITY = "isPriority"

        fun createWorkRequest(bbox: BoundingBox, isPriority: Boolean): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<DownloadWorker>()
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .setInputData(workDataOf(
                    ARG_BBOX to Json.encodeToString(bbox),
                    ARG_IS_PRIORITY to isPriority,
                ))
                .build()
    }
}

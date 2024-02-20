package de.westnordost.streetcomplete.data.upload

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.westnordost.streetcomplete.util.Listeners

/** Controls uploading */
class UploadController(private val context: Context) : UploadProgressSource {

    // TODO listener

    private val listeners = Listeners<UploadProgressSource.Listener>()
    private val workInfos: LiveData<List<WorkInfo>> get() =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(UploadWorker.TAG)

    /** @return true if an upload is running */
    override val isUploadInProgress: Boolean get() =
        workInfos.value?.any { !it.state.isFinished } == true

    init {
        workInfos.observeForever { workInfos ->
            TODO()
        }
    }

    /** Collect and upload all changes made by the user  */
    fun upload() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            UploadWorker.TAG,
            ExistingWorkPolicy.KEEP,
            UploadWorker.createWorkRequest()
        )
    }

    override fun addListener(listener: UploadProgressSource.Listener) {
        listeners.add(listener)
    }
    override fun removeListener(listener: UploadProgressSource.Listener) {
        listeners.remove(listener)
    }
}

package com.zhenxiang.nyaasi.releasetracker

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zhenxiang.nyaasi.BuildConfig
import com.zhenxiang.nyaasi.NyaaApplication.Companion.RELEASE_TRACKER_CHANNEL_ID
import com.zhenxiang.nyaasi.R
import com.zhenxiang.nyaasi.api.NyaaPageProvider
import com.zhenxiang.nyaasi.db.NyaaDb
import com.zhenxiang.nyaasi.db.NyaaReleasePreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReleaseTrackerBgWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    private val TAG = javaClass.name
    private val RELEASE_TRACKER_FOR_USERS_NOTIF_ID = 1069
    private val RELEASE_TRACKER_NOTIF_ID = 1072

    private val subscribedUsersDao = NyaaDb(appContext).subscribedTrackersDao()

    override suspend fun doWork(): Result {
        val usersWithNewReleases = mutableListOf<SubscribedUser>()
        val newReleasesForSubscribedRelease = mutableListOf<SubscribedRelease>()
        withContext(Dispatchers.IO) {
            subscribedUsersDao.getAllTrackedUsers().forEach {
                val newReleasesOfUser = getNewReleasesFromTracker(SubscribedTracker(it.id, username = it.username, lastReleaseTimestamp = it.lastReleaseTimestamp))
                if (newReleasesOfUser.isNotEmpty()) {
                    usersWithNewReleases.add(it)
                    subscribedUsersDao.updateLatestTimestamp(it.id, newReleasesOfUser[0].timestamp)
                }
            }

            subscribedUsersDao.getAllTrackedReleases().forEach {
                val newReleases = getNewReleasesFromTracker(SubscribedTracker(it.id, username = it.username, searchQuery = it.searchQuery, lastReleaseTimestamp = it.lastReleaseTimestamp))
                if (newReleases.isNotEmpty()) {
                    newReleasesForSubscribedRelease.add(it)
                    subscribedUsersDao.updateLatestTimestamp(it.id, newReleases[0].timestamp)
                }
            }
        }
        withContext(Dispatchers.Main) {
            var notifcationContent: String? = if (usersWithNewReleases.isEmpty()) {
                if (BuildConfig.DEBUG) "[DEBUG] No new releases" else null
            } else {
                var usersListString = ""
                usersWithNewReleases.forEachIndexed { index, subscribedUser ->
                    if (index == 0) {
                        usersListString += subscribedUser.username
                    } else if (index == usersWithNewReleases.size - 1) {
                        usersListString += applicationContext.getString(R.string.and_conjunction_word, subscribedUser.username)
                    } else {
                        usersListString += applicationContext.getString(R.string.comma_word, subscribedUser.username)
                    }
                }
                applicationContext.getString(R.string.release_tracker_new_releases_from_user, usersListString)
            }
            notifcationContent?.let {
                generateNotif(RELEASE_TRACKER_FOR_USERS_NOTIF_ID, it)
            }

            if (newReleasesForSubscribedRelease.isNotEmpty()) {
                var expandedText = applicationContext.getString(R.string.release_tracker_new_releases_expanded) + "\n"
                newReleasesForSubscribedRelease.forEach {
                    expandedText += "- " + it.searchQuery + "\n"
                }
                generateNotif(RELEASE_TRACKER_NOTIF_ID, applicationContext.getString(R.string.release_tracker_new_releases), expandedText)
            }
        }
        return Result.success()
    }

    private fun generateNotif(id: Int, content: String, expandedText: String? = null) {
        val notificationBuilder = NotificationCompat.Builder(applicationContext, RELEASE_TRACKER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_magnet)
            .setContentTitle("New releases")
            .setContentText(content)
            .setAutoCancel(true)

        expandedText?.let {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(it))
        }

        with(NotificationManagerCompat.from(applicationContext)) {
            // notificationId is a unique int for each notification that you must define
            notify(id, notificationBuilder.build())
        }
    }

    private suspend fun getNewReleasesFromTracker(tracker: SubscribedTracker): MutableList<NyaaReleasePreview> {
        val newReleases = mutableListOf<NyaaReleasePreview>()
        var pageIndex = 0
        while(true) {
            // Parse pages until we hit null or empty page
            val releases = NyaaPageProvider.getPageItems(pageIndex, user = tracker.username, searchQuery = tracker.searchQuery)
            if (releases == null || releases.isEmpty()) {
                return newReleases
            } else {
                releases.forEach {
                    // If release timestamp is smaller or equal than lastReleaseTimestamp
                    // we've hit a release than the last one saved in tracker,
                    // so let's exit and call it a day
                    if (tracker.lastReleaseTimestamp >= it.timestamp) {
                        return newReleases
                    } else {
                        newReleases.add(it)
                    }
                }
            }
            pageIndex ++
        }
    }

    companion object {
        const val WORK_NAME = "releaseTrackerWork"
    }
}
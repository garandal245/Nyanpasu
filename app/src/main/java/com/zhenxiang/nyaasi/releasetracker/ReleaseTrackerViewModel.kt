package com.zhenxiang.nyaasi.releasetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

class ReleaseTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ReleaseTrackerRepo(application)

    val subscribedTrackerSearch = MutableLiveData<String>()
    private val preFilterSubscribedTrackers = repo.subscribedUsersDao.getAllLive()
    val subscribedTrackers = Transformations.switchMap(subscribedTrackerSearch) { query ->
        if (query.isNullOrEmpty()) {
            preFilterSubscribedTrackers
        } else {
            Transformations.map(preFilterSubscribedTrackers) {
                it.filter { item -> item.username?.contains(query, true) == true || item.searchQuery?.contains(query, true) == true }
            }
        }
    }

    init {
        subscribedTrackerSearch.value = null
    }

    fun addReleaseTracker(tracker: SubscribedTracker) {
        repo.subscribedUsersDao.insert(tracker)
    }

    fun getTrackedByUsername(username: String): SubscribedUser? {
        return repo.subscribedUsersDao.getByUsername(username)
    }

    fun getTrackedByUsernameAndQuery(username: String?, query: String): SubscribedRelease? {
        return if (username.isNullOrBlank()) {
            repo.subscribedUsersDao.getByQueryWithNullUsername(query)
        } else {
            repo.subscribedUsersDao.getByUsernameAndQuery(username, query)
        }
    }

    fun deleteTrackedUser(username: String) {
        repo.subscribedUsersDao.deleteByUsername(username)
    }
}
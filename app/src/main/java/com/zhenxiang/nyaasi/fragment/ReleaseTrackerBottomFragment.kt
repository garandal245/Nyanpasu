package com.zhenxiang.nyaasi.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zhenxiang.nyaasi.R
import com.zhenxiang.nyaasi.releasetracker.ReleaseTrackerBgWorker
import com.zhenxiang.nyaasi.releasetracker.ReleaseTrackerViewModel
import com.zhenxiang.nyaasi.releasetracker.SubscribedTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

private const val ARG_USERNAME = "username"
private const val ARG_LATEST_TIMESTAMP = "latestTimestamp"

class ReleaseTrackerBottomFragment : BottomSheetDialogFragment() {
    private lateinit var username: String
    private var latestTimestamp by Delegates.notNull<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().let {
            username = it.getString(ARG_USERNAME)!!
            latestTimestamp = it.getLong(ARG_LATEST_TIMESTAMP)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val releasesTrackerViewModel = ViewModelProvider(this).get(ReleaseTrackerViewModel::class.java)
        val fragmentView = inflater.inflate(R.layout.fragment_release_tracker_bottom, container, false)
        val trackAllFromUser = fragmentView.findViewById<View>(R.id.track_all_from_user)
        trackAllFromUser.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val newTracked = SubscribedTracker(username = username, lastReleaseTimestamp = latestTimestamp)
                releasesTrackerViewModel.addUserToTracker(newTracked)
                withContext(Dispatchers.Main) {
                    parentFragmentManager.setFragmentResult(NEW_TRACKED_USER, bundleOf(
                        NEW_TRACKED_USER to newTracked))
                    dismiss()
                }
            }
        }

        val trackAllFromUserForQuery = fragmentView.findViewById<View>(R.id.track_all_from_user_for_query)
        trackAllFromUserForQuery.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val newTracked = SubscribedTracker(searchQuery = "zettai makenai", lastReleaseTimestamp = 1619010092)
                releasesTrackerViewModel.addUserToTracker(newTracked)
                withContext(Dispatchers.Main) {
                    parentFragmentManager.setFragmentResult(NEW_TRACKED_USER, bundleOf(
                        NEW_TRACKED_USER to newTracked))
                    val releaseTracker =
                        OneTimeWorkRequestBuilder<ReleaseTrackerBgWorker>()
                            .build()
                    WorkManager.getInstance(fragmentView.context).enqueue(releaseTracker)
                    dismiss()
                }
            }
        }
        return fragmentView
    }

    companion object {

        const val NEW_TRACKED_USER = "newTrackerUser"

        @JvmStatic
        fun newInstance(username: String, latestTimestamp: Long) =
            ReleaseTrackerBottomFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USERNAME, username)
                    putLong(ARG_LATEST_TIMESTAMP, latestTimestamp)
                }
            }
    }
}
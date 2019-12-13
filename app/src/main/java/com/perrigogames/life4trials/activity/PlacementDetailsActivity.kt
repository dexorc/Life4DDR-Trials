package com.perrigogames.life4trials.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.perrigogames.life4trials.R
import com.perrigogames.life4trials.SettingsKeys.KEY_DETAILS_PHOTO_SELECT
import com.perrigogames.life4trials.data.Song
import com.perrigogames.life4trials.data.Trial
import com.perrigogames.life4trials.life4app
import com.perrigogames.life4trials.manager.PlacementManager
import com.perrigogames.life4trials.ui.songlist.SongListFragment
import com.perrigogames.life4trials.util.SharedPrefsUtil
import com.perrigogames.life4trials.view.RankHeaderView
import kotlinx.android.synthetic.main.content_placement_details.*


class PlacementDetailsActivity: PhotoCaptureActivity(), SongListFragment.Listener {

    private val placementManager: PlacementManager get() = life4app.placementManager
    private val placementId: String by lazy { intent.extras!!.getString(ARG_PLACEMENT_ID) }
    private val trial: Trial get() = placementManager.findPlacement(placementId)!!

    override val snackbarContainer: ViewGroup get() = container

    private lateinit var songListFragment: SongListFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_placement_details)

        (layout_rank_header as RankHeaderView).apply {
            rank = trial.placement_rank!!.parent
            genericTitles = true
//            navigationListener = object : RankHeaderView.NavigationListener {
//                override fun onPreviousClicked() = navigationButtonClicked(placementManager.previousPlacement(placementId))
//                override fun onNextClicked() = navigationButtonClicked(placementManager.nextPlacement(placementId))
//            }
        }

        switch_acquire_mode.isChecked = SharedPrefsUtil.getUserFlag(this, KEY_DETAILS_PHOTO_SELECT, false)
        switch_acquire_mode.setOnCheckedChangeListener { _, isChecked ->
            SharedPrefsUtil.setUserFlag(this, KEY_DETAILS_PHOTO_SELECT, isChecked)
        }

        songListFragment = SongListFragment.newInstance(trial.id, tiled = false, useCurrentSession = false, useCamera = false)
        supportFragmentManager.beginTransaction()
            .add(R.id.container_song_list_fragment, songListFragment)
            .commitNow()
    }

    override fun onResume() {
        super.onResume()
        try {
            scroll_details.scrollTo(0, 0)
        } catch (_: NullPointerException) {}
    }

    fun navigationButtonClicked(placement: Trial?) {
        if (placement != null) {
            startActivity(intent(this, placement.id))
            finish()
        }
    }

    override fun onSongSelected(song: Song, position: Int) = Unit

    override fun onNewPhotoCreated(uri: Uri) = Unit

    override fun onPhotoCancelled() = Unit

    fun onFinalizeClick(v: View) = acquirePhoto()

    private fun doSubmit(uri: Uri) {
//        life4app.trialManager.saveRecord(session)
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_placement_title)
            .setMessage(R.string.trial_submit_dialog_prompt)
            .setNegativeButton(R.string.no) { _, _ -> finishWithResult() }
            .setPositiveButton(R.string.yes) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_standard_submission_form))))
                finishWithResult()
            }
            .show()
    }

    private fun finishWithResult() {
        setResult(RESULT_FINISHED)
        finish()
    }

    override fun onPhotoTaken(uri: Uri) {
        doSubmit(uri)
    }

    override fun onPhotoChosen(uri: Uri) {
        doSubmit(uri)
    }

    companion object {
        const val ARG_PLACEMENT_ID = "ARG_PLACEMENT_ID"
        const val RESULT_FINISHED = 1003

        fun intent(c: Context, trialId: String) =
            Intent(c, PlacementDetailsActivity::class.java).apply {
                putExtra(ARG_PLACEMENT_ID, trialId)
            }
    }
}
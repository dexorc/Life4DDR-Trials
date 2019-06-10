package com.perrigogames.life4trials.ui.songlist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.perrigogames.life4trials.R
import com.perrigogames.life4trials.data.Song
import com.perrigogames.life4trials.data.SongResult
import com.perrigogames.life4trials.life4app
import com.perrigogames.life4trials.view.SongView

/**
 * A [Fragment] designed to display a list of [SongView]s in a vertical arrangement.
 */
class SongListFragment : Fragment() {

    private val trialManager get() = context!!.life4app.trialManager
    private val trial get() = trialManager.findTrial(trialId!!)!!
    private val results get() = if (useCurrentSession) trialManager.currentSession!!.results else null

    private var trialId: String? = null
    private var tiled: Boolean = false
    private var useCurrentSession: Boolean = false
    private var listener: Listener? = null

    private lateinit var layout: LinearLayout
    private val songViews = mutableListOf<SongView>()

    var shouldShowAdvancedSongDetails: Boolean = false
        set(v) {
            field = v
            songViews.forEach {
                if (useCurrentSession) {
                    it.shouldShowAdvancedSongDetails = v
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            trialId = it.getString(ARG_TRIAL_ID)
            tiled = it.getBoolean(ARG_TILED, tiled)
            useCurrentSession = it.getBoolean(ARG_CURRENT_SESSION_RESULTS, false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        layout = inflater.inflate(R.layout.fragment_song_list, container, false) as LinearLayout
        trial.songs.forEachIndexed(this::addSongView)
        return layout
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement Listener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun addSongView(idx: Int, song: Song) {
        val newView = layoutInflater.inflate(R.layout.item_song_list_item, layout, false)
        songViews.add(idx, (newView as SongView).also {
            it.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            it.tag = idx
            it.song = song
            if (results != null) {
                it.result = results!![idx]
            }
            it.setOnClickListener { listener?.onSongSelected(song, idx) }
        })
        layout.addView(newView)
    }

    fun setSongResult(idx: Int, result: SongResult?) {
        songViews[idx].result = result
    }

    interface Listener {
        fun onSongSelected(song: Song, position: Int)
    }

    companion object {
        private const val ARG_TRIAL_ID = "ARG_TRIAL_ID"
        private const val ARG_TILED = "ARG_TILED"
        private const val ARG_CURRENT_SESSION_RESULTS = "ARG_CURRENT_SESSION_RESULTS"

        @JvmStatic
        fun newInstance(trialId: String, tiled: Boolean, useCurrentSession: Boolean) =
            SongListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TRIAL_ID, trialId)
                    putBoolean(ARG_TILED, tiled)
                    putBoolean(ARG_CURRENT_SESSION_RESULTS, useCurrentSession)
                }
            }
    }
}
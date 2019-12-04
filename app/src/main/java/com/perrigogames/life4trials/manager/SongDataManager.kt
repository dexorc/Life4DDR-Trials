package com.perrigogames.life4trials.manager

import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import com.crashlytics.android.Crashlytics
import com.perrigogames.life4trials.Life4Application
import com.perrigogames.life4trials.R
import com.perrigogames.life4trials.SettingsKeys.KEY_IMPORT_GAME_VERSION
import com.perrigogames.life4trials.api.GithubDataAPI
import com.perrigogames.life4trials.api.LocalRemoteData
import com.perrigogames.life4trials.api.MajorVersionedRemoteData
import com.perrigogames.life4trials.data.*
import com.perrigogames.life4trials.db.ChartDB
import com.perrigogames.life4trials.db.SongDB
import com.perrigogames.life4trials.event.MajorUpdateProcessEvent
import com.perrigogames.life4trials.repo.SongDataRepo
import com.perrigogames.life4trials.util.*
import com.perrigogames.life4trials.util.SharedPrefsUtil.KEY_SONG_LIST_VERSION
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import retrofit2.Response


/**
 * A Manager class that keeps track of the available songs
 */
class SongDataManager(private val context: Context,
                      private val songDataRepo: SongDataRepo,
                      private val githubDataAPI: GithubDataAPI): BaseManager() {

    private val songList = object: LocalRemoteData<String>(context, R.raw.songs, SONGS_FILE_NAME) {
        override fun createLocalDataFromText(text: String) = text
        override fun createTextToData(data: String) = data
        override fun getDataVersion(data: String) = data.substring(0, data.indexOfOrEnd('\n')).trim().toInt()
        override suspend fun getRemoteResponse(): Response<String> = githubDataAPI.getSongList()
        override fun onFetchUpdated(data: String) {
            super.onFetchUpdated(data)
            refreshSongDatabase(data)
        }
    }

    private val ignoreLists = object: MajorVersionedRemoteData<IgnoreLists>(context, R.raw.ignore_lists, IGNORES_FILE_NAME, 1) {
        override suspend fun getRemoteResponse() = githubDataAPI.getIgnoreLists()
        override fun createLocalDataFromText(text: String) = DataUtil.gson.fromJson(text, IgnoreLists::class.java)
    }

    init {
        Life4Application.eventBus.register(this)
        songList.start()
        ignoreLists.start()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMajorVersion(e: MajorUpdateProcessEvent) {
        if (e.version == MajorUpdate.SONG_DB) {
            initializeSongDatabase()
        } else if (e.version == MajorUpdate.DOUBLES_FIX) {
            songDataRepo.chartBox.remove(getChartsByPlayStyle(PlayStyle.DOUBLE))
            refreshSongDatabase(force = true)
        }
    }

    //
    // Song List Management
    //
    fun initializeSongDatabase() {
        songDataRepo.chartBox.removeAll()
        songBox.removeAll()
        refreshSongDatabase(force = true)
    }

    private fun refreshSongDatabase(input: String = songList.data, force: Boolean = false) {
        val lines = input.lines()
        if (force || SharedPrefsUtil.getUserInt(context, KEY_SONG_LIST_VERSION, -1) < lines[0].toInt()) {
            val songContents = songBox.all

            val dbSongs = mutableListOf<SongDB>()
            val dbCharts = mutableListOf<ChartDB>()
            lines.forEachIndexed { idx, line ->
                if (idx == 0 || line.isEmpty()) {
                    return@forEachIndexed
                }
                val data = line.split(";")
                val id = data[0].toLong()
                val title = data[1]
                var preview = false
                val mix = GameVersion.parse(data[2].let {
                    it.toLongOrNull() ?: it.substring(0, it.length - 1).let { seg ->
                        preview = true
                        seg.toLong()
                    }
                })
                val existingSong: SongDB? = songContents.firstOrNull { it.id == id }
                existingSong?.let {
                    if (it.title != title || it.artist != null || it.version != mix || it.preview != preview) {
                        it.title = title
                        it.artist = null
                        it.version = mix
                        it.preview = preview
                        songBox.put(it)
                    }
                }
                val song = existingSong ?: SongDB(title, null, mix, preview).also { song ->
                    songBox.attach(song)
                }
                PlayStyle.values().forEachIndexed { sIdx, style ->
                    DifficultyClass.values().forEachIndexed { dIdx, diff ->
                        val diffStr = data[3 + ((sIdx * DifficultyClass.values().size) + dIdx)]
                        if (diffStr.isNotEmpty()) {
                            updateOrCreateChartForSong(song, style, diff, diffStr.toInt(), false).also { chart ->
                                dbCharts.add(chart)
                                song.charts.add(chart)
                            }
                        }
                    }
                }
                dbSongs.add(song)
            }
            songDataRepo.chartBox.put(dbCharts)
            songBox.put(dbSongs)
            invalidateIgnoredIds()
            SharedPrefsUtil.setUserInt(context, KEY_SONG_LIST_VERSION, lines[0].toInt())
        }
    }

    //
    // Ignore List Data
    //
    val ignoreListIds get() = ignoreLists.data.lists.map { it.id }
    val ignoreListTitles get() = ignoreLists.data.lists.map { it.name }

    val selectedVersion: String
        get() = SharedPrefsUtil.getUserString(context, KEY_IMPORT_GAME_VERSION, DEFAULT_IGNORE_VERSION)!!
    val selectedIgnoreList: IgnoreList?
        get() = getIgnoreList(selectedVersion)

    fun getIgnoreList(id: String): IgnoreList =
        ignoreLists.data.lists.firstOrNull { it.id == id } ?: getIgnoreList(DEFAULT_IGNORE_VERSION)

    private var mSelectedIgnoreSongIds: LongArray? = null
    private var mSelectedIgnoreChartIds: LongArray? = null

    val selectedIgnoreSongIds: LongArray
        get() {
            if (mSelectedIgnoreSongIds == null) {
                mSelectedIgnoreSongIds = selectedIgnoreList?.songs?.map { it.title }?.toTypedArray()?.let { ignoreTitles ->
                    val versionId = selectedIgnoreList!!.baseVersion.stableId
                    blockedSongQuery(ignoreTitles, versionId, versionId + 1)
                        .find().map { it.id }.toLongArray()
                } ?: LongArray(0)
            }
            return mSelectedIgnoreSongIds!!
        }
    val selectedIgnoreChartIds: LongArray
        get() {
            if (mSelectedIgnoreChartIds == null) {
                mSelectedIgnoreChartIds = selectedIgnoreList?.charts?.mapNotNull { chart ->
                    val song = songTitleQuery.setParameter("title", chart.title).findFirst()
                    return@mapNotNull song?.charts?.firstOrNull { it.difficultyClass == chart.difficultyClass }?.id
                }?.toLongArray() ?: LongArray(0)
            }
            return mSelectedIgnoreChartIds!!
        }

    fun onA20RequiredUpdate(context: Context) {
        invalidateIgnoredIds()
        SharedPrefsUtil.setUserString(context, KEY_IMPORT_GAME_VERSION, DEFAULT_IGNORE_VERSION)
        Handler().postDelayed({
            AlertDialog.Builder(context)
                .setTitle(R.string.a20_update)
                .setMessage(R.string.a20_update_explanation)
                .setNegativeButton(R.string.more_info) { d, _ ->
                    context.openWebUrlFromRes(R.string.url_a20_update)
                    d.dismiss()
                }
                .setPositiveButton(R.string.okay) { d, _ -> d.dismiss() }
                .setCancelable(true)
                .create()
                .show()
        }, 10)
    }

    /**
     * Nulls out the list of invalid IDs, to regenerate them
     */
    fun invalidateIgnoredIds() {
        mSelectedIgnoreSongIds = null
        mSelectedIgnoreChartIds = null
    }

    fun updateOrCreateChartForSong(song: SongDB,
                                   playStyle: PlayStyle,
                                   difficultyClass: DifficultyClass,
                                   difficultyNumber: Int,
                                   commit: Boolean = true): ChartDB {
        val chart = song.charts.firstOrNull { it.playStyle == playStyle && it.difficultyClass == difficultyClass }
        chart?.let {
            if (it.difficultyNumber != difficultyNumber) {
                Crashlytics.logException(UnexpectedDifficultyNumberException(it, difficultyNumber))
                it.difficultyNumber = difficultyNumber
                if (commit) {
                    songDataRepo.chartBox.put(it)
                }
            }
        }
        return chart ?: ChartDB(difficultyClass, difficultyNumber, playStyle).also {
            song.charts.add(it)
            if (commit) {
                songDataRepo.chartBox.put(it)
                songBox.put(song)
            }
        }
    }

    companion object {
        const val SONGS_FILE_NAME = "songs.csv"
        const val IGNORES_FILE_NAME = "ignore_lists.json"
        const val DEFAULT_IGNORE_VERSION = "A20_US"
    }
}

class UnexpectedDifficultyNumberException(chart: ChartDB, newDiff: Int): Exception(
    "Chart ${chart.song.target.title} ${chart.playStyle} ${chart.difficultyClass} changed: ${chart.difficultyNumber} -> $newDiff")

class SongNotFoundException(name: String): Exception("$name does not exist in the song database")
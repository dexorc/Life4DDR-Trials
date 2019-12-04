package com.perrigogames.life4trials.repo

import android.util.Log
import com.perrigogames.life4trials.data.DifficultyClass
import com.perrigogames.life4trials.data.PlayStyle
import com.perrigogames.life4trials.db.ChartDB
import com.perrigogames.life4trials.db.ChartDB_
import com.perrigogames.life4trials.db.SongDB
import com.perrigogames.life4trials.db.SongDB_
import com.perrigogames.life4trials.manager.BaseManager

class SongDataRepo: BaseManager() {

    //
    // ObjectBoxes
    //
    val songBox get() = objectBox.boxFor(SongDB::class.java)
    val chartBox get() = objectBox.boxFor(ChartDB::class.java)

    //
    // Queries
    //
    private val songTitleQuery = songBox.query()
        .equal(SongDB_.title, "").parameterAlias("title")
        .build()
    private fun blockedSongQuery(titles: Array<String>, version: Long, previewVersion: Long) = songBox.query()
        .greater(SongDB_.version, previewVersion) // block everything higher than preview version
        .or().greater(SongDB_.version, version).and().equal(SongDB_.preview, false) // block non-preview songs in preview versions
        .or().`in`(SongDB_.title, titles) // block songs in the supplied list
        .build()
    private val gameVersionQuery = songBox.query()
        .equal(SongDB_.version, -1).parameterAlias("version")
        .build()
    private val multipleGameVersionQuery = songBox.query()
        .`in`(SongDB_.version, LongArray(0)).parameterAlias("versions")
        .build()
    private val chartPlayStyleQuery = chartBox.query()
        .equal(ChartDB_.playStyle, 0).parameterAlias("play_style")
        .build()
    private val chartDifficultyQuery = chartBox.query().apply {
        equal(ChartDB_.difficultyNumber, 0).parameterAlias("difficulty")
        equal(ChartDB_.playStyle, 0).parameterAlias("play_style")
        link(ChartDB_.song).notIn(SongDB_.id, selectedIgnoreSongIds)
        notIn(ChartDB_.id, selectedIgnoreChartIds)
    }.build()

    fun getSongs(): List<SongDB> = songBox.all

    fun getSongById(id: Long): SongDB? = songBox.get(id)

    fun getSongsById(ids: LongArray): MutableList<SongDB> = songBox.get(ids)

    fun getSongByName(name: String): SongDB? =
        songTitleQuery.setParameter("title", name).findFirst()

    fun getChartById(id: Long): ChartDB? = chartBox.get(id)

    fun getChartsById(ids: LongArray): MutableList<ChartDB> = chartBox.get(ids)

    fun getChartsByPlayStyle(playStyle: PlayStyle): MutableList<ChartDB> =
        chartPlayStyleQuery.setParameter("play_style", playStyle.stableId)
            .find()

    fun getFilteredChartsByDifficulty(difficulty: Int, playStyle: PlayStyle): MutableList<ChartDB> =
        chartDifficultyQuery.setParameter("difficulty", difficulty.toLong())
            .setParameter("play_style", playStyle.stableId)
            .find()

    fun getFilteredChartsByDifficulty(difficultyList: IntArray, playStyle: PlayStyle): MutableList<ChartDB> = mutableListOf<ChartDB>().apply {
        difficultyList.forEach {
            addAll(chartDifficultyQuery
                .setParameter("difficulty", it.toLong())
                .setParameter("play_style", playStyle.stableId)
                .find())
        }
    }

    fun updateChart(chart: ChartDB) = chartBox.put(chart)

    //
    // Other Methods
    //

    fun dumpData() {
        val songStrings = songBox.all.map { song ->
            val builder = StringBuilder("${song.title};")
            val chartsCopy = song.charts.toMutableList()
            PlayStyle.values().forEach { style ->
                DifficultyClass.values().forEach { diff ->
                    val chart = chartsCopy.firstOrNull { it.playStyle == style && it.difficultyClass == diff }
                    if (chart != null) {
                        chartsCopy.remove(chart)
                        builder.append("${chart.difficultyNumber};")
                    } else {
                        builder.append(";")
                    }
                }
            }
            builder.toString()
        }.toMutableList()
        with(StringBuilder()) {
            while (songStrings.isNotEmpty()) {
                repeat((0..10).count()) {
                    if (songStrings.isNotEmpty()) {
                        append("${songStrings.removeAt(0)}[][]")
                    }
                }
                Log.v("SongDataManager", this.toString())
                setLength(0)
            }
        }
    }
}
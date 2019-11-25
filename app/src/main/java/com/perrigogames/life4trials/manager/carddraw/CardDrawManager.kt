package com.perrigogames.life4trials.manager.carddraw

import com.perrigogames.life4trials.data.DifficultyClass
import com.perrigogames.life4trials.data.PlayStyle
import com.perrigogames.life4trials.db.ChartDB
import com.perrigogames.life4trials.manager.BaseManager
import kotlin.math.min

class CardDrawManager: BaseManager() {

    fun performCardDraw(items: List<ChartDB>, options: ICardDrawOptions): List<ChartDB> {
        return items.filter { options.acceptChart(it) }.shuffled().let { it.subList(0, min(options.initialCount, it.size)) }
    }

    /**
     * Describes the functionalities that should be present on any structure designated for card draws.
     */
    interface ICardDrawOptions {

        val initialCount: Int

        /** the final number of songs expected to be played */
        val finalCount: Int

        fun acceptChart(chart: ChartDB): Boolean = true
    }

    /**
     * A data class that holds information about a card draw.
     * @param protects the number of songs that can be protected by each player
     * @param vetoes the number of songs that will be vetoed by each player
     */
    open class CardDrawOptions(
        val players: Int = 2,
        override val finalCount: Int = 3,
        val protects: Int = 1,
        val vetoes: Int = 1,
        val playStyle: PlayStyle = PlayStyle.SINGLE,
        val allowedClasses: List<DifficultyClass>? = null,
        open val allowedDiffs: List<Int>? = null): ICardDrawOptions {

        init {
            require(protects * players <= finalCount) { "$protects protects/$players players exceeds final count $finalCount" }
        }

        /** Field describing the number of songs that should be in the initial card draw before any vetoes occur */
        override val initialCount = finalCount + (vetoes * players)

        override fun acceptChart(chart: ChartDB): Boolean {
            return playStyle == chart.playStyle
                    && (allowedClasses?.contains(chart.difficultyClass) ?: true)
                    && (allowedDiffs?.contains(chart.difficultyNumber) ?: true)
        }
    }

    open class WeightedCardDrawOptions(
        players: Int = 2,
        finalCount: Int = 3,
        protects: Int = 1,
        vetoes: Int = 1,
        playStyle: PlayStyle = PlayStyle.SINGLE,
        allowedClasses: List<DifficultyClass>? = null,
        val weightedDiffs: Map<Int, Int>): CardDrawOptions(players, finalCount, protects, vetoes, playStyle, allowedClasses, null) {

        override val allowedDiffs get() = weightedDiffs.keys.toList()
    }
}
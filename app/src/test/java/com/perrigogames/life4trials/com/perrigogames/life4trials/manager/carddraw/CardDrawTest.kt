package com.perrigogames.life4trials.com.perrigogames.life4trials.manager.carddraw

import com.perrigogames.life4trials.data.DifficultyClass
import com.perrigogames.life4trials.db.ChartDB
import com.perrigogames.life4trials.manager.carddraw.CardDrawManager
import com.perrigogames.life4trials.manager.carddraw.CardDrawManager.CardDrawOptions
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test


class CardDrawTest {

    private val cardDrawSongs: List<ChartDB> = (1..DIFF_CAP).flatMap {  diff ->
        (1..CHARTS_PER_DIFF).map { ChartDB(diffIndexClass(it), diff, id = chartId) }
    }

    private val cardDrawManager = CardDrawManager()

    @Test
    fun testCardDraw_DifficultyNumberLimit() {
        (1..DIFF_CAP).forEach { diff ->
            println("Difficulty $diff:")
            val draw = cardDrawManager.performCardDraw(cardDrawSongs, CardDrawOptions(allowedDiffs = listOf(diff)))
            verifyDraw(draw, true) { assertEquals(diff, it.difficultyNumber) }
            println()
        }
    }

    @Test
    fun testCardDraw_MultiDifficulty() {
        (1..DIFF_CAP - 3).forEach { diff ->
            val range = (diff..diff+2)
            println("Difficulty ${range.joinToString()}:")
            val draw = cardDrawManager.performCardDraw(cardDrawSongs, CardDrawOptions(allowedDiffs = range.toList()))
            verifyDraw(draw, true) { assertTrue(range.contains(it.difficultyNumber)) }
            println()
        }
    }

    @Test
    fun testCardDraw_DifficultyClassLimit() {
        DifficultyClass.values().forEach { clazz ->
            println("Difficulty Class $clazz:")
            val draw = cardDrawManager.performCardDraw(cardDrawSongs, CardDrawOptions(allowedClasses = listOf(clazz)))
            verifyDraw(draw, true) { assertEquals(clazz, it.difficultyClass) }
            println()
        }
    }

    companion object {
        const val DIFF_CAP = 19
        const val CHARTS_PER_DIFF = 10

        var chartId: Long = 0
            get() {
                field++
                return field
            }

        fun diffIndexClass(idx: Int): DifficultyClass = when(idx) {
            1, 2 -> DifficultyClass.BEGINNER
            3, 4 -> DifficultyClass.BASIC
            5, 6 -> DifficultyClass.DIFFICULT
            7, 8 -> DifficultyClass.EXPERT
            else -> DifficultyClass.CHALLENGE
        }

        private inline fun verifyDraw(draw: List<ChartDB>, print: Boolean = false, validation: (ChartDB) -> Unit) {
            draw.forEach {
                validation(it)
                if (print) {
                    println("${it.id} - ${it.difficultyClass} (${it.difficultyNumber})")
                }
            }
        }
    }
}
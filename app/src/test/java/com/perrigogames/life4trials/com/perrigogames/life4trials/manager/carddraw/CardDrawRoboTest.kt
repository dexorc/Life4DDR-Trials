package com.perrigogames.life4trials.com.perrigogames.life4trials.manager.carddraw

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.perrigogames.life4trials.Life4Application
import com.perrigogames.life4trials.data.PlayStyle
import com.perrigogames.life4trials.db.ChartDB
import com.perrigogames.life4trials.manager.carddraw.CardDrawManager
import com.perrigogames.life4trials.manager.carddraw.CardDrawManager.WeightedCardDrawOptions
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardDrawRoboTest {

    private val cardDrawManager = CardDrawManager()
    private lateinit var chartDatabase: List<ChartDB>

    @Before
    fun setupResources() {
        chartDatabase = getApplicationContext<Life4Application>().songDataManager.getCharts()
    }

    @Test
    fun testCardDraw_RealData_Single() {
        (1..CardDrawTest.DIFF_CAP).forEach { diff ->
            println("Difficulty $diff:")
            val draw = cardDrawManager.performCardDraw(chartDatabase, WeightedCardDrawOptions(playStyle = PlayStyle.SINGLE, weightedDiffs = mapOf(Pair(diff, 1))))
            verifyDraw(draw, true) { TestCase.assertEquals(diff, it.difficultyNumber) }
            println()
        }
    }

    @Test
    fun testCardDraw_RealData_Double() {
        (1..CardDrawTest.DIFF_CAP).forEach { diff ->
            println("Difficulty $diff:")
            val draw = cardDrawManager.performCardDraw(chartDatabase, WeightedCardDrawOptions(playStyle = PlayStyle.DOUBLE, weightedDiffs = mapOf(Pair(diff, 1))))
            verifyDraw(draw, true) { TestCase.assertEquals(diff, it.difficultyNumber) }
            println()
        }
    }

    companion object {
        inline fun verifyDraw(draw: List<ChartDB>, print: Boolean = false, validation: (ChartDB) -> Unit) {
            draw.forEach {
                validation(it)
                if (print) {
                    println("(${it.id}) ${it.song.target.title} - ${it.difficultyClass} (${it.difficultyNumber})")
                }
            }
        }
    }
}
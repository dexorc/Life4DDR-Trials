package com.perrigogames.life4trials.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.perrigogames.life4trials.R
import com.perrigogames.life4trials.data.DifficultyClass

class DifficultyTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.textViewStyle) :
    AppCompatTextView(context, attrs, defStyleAttr) {

    var difficultyNumber: Int = 0
        set(value) {
            field = value
            update()
        }
    var difficultyClass: DifficultyClass =
        DifficultyClass.BEGINNER
        set(value) {
            field = value
            update()
        }

    var customTitle: String? = null
        set(value) {
            field = value
            update()
        }

    fun setDifficulty(clazz: DifficultyClass, number: Int) {
        difficultyClass = clazz
        difficultyNumber = number
    }

    private fun update() {
        setTextColor(ContextCompat.getColor(context, difficultyClass.colorRes))
        val title = customTitle ?: difficultyClass.toString()
        text = resources.getString(R.string.difficulty_string_format, title, difficultyNumber)
    }
}
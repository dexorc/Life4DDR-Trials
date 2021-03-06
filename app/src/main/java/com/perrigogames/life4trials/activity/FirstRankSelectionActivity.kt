package com.perrigogames.life4trials.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.perrigogames.life4trials.R
import com.perrigogames.life4trials.activity.RankDetailsActivity.Companion.RESULT_RANK_SELECTED
import com.perrigogames.life4trials.data.LadderRank
import com.perrigogames.life4trials.data.RankEntry
import com.perrigogames.life4trials.life4app
import com.perrigogames.life4trials.ui.ranklist.RankListFragment
import com.perrigogames.life4trials.ui.ranklist.RankListFragment.OnRankListInteractionListener
import kotlinx.android.synthetic.main.content_rank_list.*

/**
 * Activity displaying the list of ladder ranks that can be obtained.
 */
class FirstRankSelectionActivity : AppCompatActivity(), OnRankListInteractionListener {

    private val firstRunManager get() = life4app.firstRunManager
    private val ladderManager get() = life4app.ladderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_rank_list)

        layout_container.removeAllViews()
        supportFragmentManager.beginTransaction()
            .add(R.id.layout_container,
                RankListFragment.newInstance(3))
            .commit()
    }

    override fun onListFragmentInteraction(item: RankEntry?) =
        startActivityForResult(RankDetailsActivity.intent(this, item?.rank), REQUEST_CODE_DETAIL_SELECTION)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DETAIL_SELECTION) {
            if (resultCode == RESULT_RANK_SELECTED && data != null) {
                ladderManager.setUserRank(LadderRank.parse(data.getLongExtra(RankDetailsActivity.EXTRA_RANK, 0)))
                startActivity(firstRunManager.finishProcessIntent)
                finish()
            }
        }
    }

    fun onPlacementsClick(v: View) {
        startActivity(firstRunManager.placementIntent)
        finish()
    }

    fun onNoRankClick(v: View) {
        startActivity(firstRunManager.finishProcessIntent)
        finish()
    }

    companion object {
        const val REQUEST_CODE_DETAIL_SELECTION = 1022
    }
}

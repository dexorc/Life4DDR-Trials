package com.perrigogames.life4trials.event

import com.perrigogames.life4trials.data.Trial

/**
 * Event published when a saved rank is updated.
 * @param trial the [Trial] that was updated. If null, indicate all Trials' ranks may have been updated
 */
class SavedRankUpdatedEvent(val trial: Trial? = null)

/**
 * Event published when something changes with the Trial List, requiring it to be refreshed.
 */
class TrialListUpdatedEvent

/**
 * Event published when something substantial changes with the Trial List, requiring the list to recreate the
 */
class TrialListReplacedEvent

/**
 * Event published when a ladder data import finishes, including information about the number of items updated
 * and how many items gave errors.
 */
class LadderImportCompletedEvent(val updated: Int, val errors: Int)
package com.nuvio.app.features.player

import com.nuvio.app.core.network.NetworkCondition

/** The account/sync backend is independent of an installed addon's subtitle host. */
internal fun deferAddonSubtitleRequest(condition: NetworkCondition): Boolean =
    condition == NetworkCondition.NoInternet

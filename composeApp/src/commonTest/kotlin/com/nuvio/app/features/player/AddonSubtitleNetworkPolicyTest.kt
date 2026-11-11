package com.nuvio.app.features.player

import com.nuvio.app.core.network.NetworkCondition
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddonSubtitleNetworkPolicyTest {
    @Test fun accountServerOutageDoesNotDisableIndependentSubtitleHosts() {
        assertFalse(deferAddonSubtitleRequest(NetworkCondition.ServersUnreachable))
    }
    @Test fun knownInternetOutageDefersTheRequest() {
        assertTrue(deferAddonSubtitleRequest(NetworkCondition.NoInternet))
    }
    @Test fun healthyOrUnconfirmedConnectivityStillAllowsTheRequest() {
        listOf(NetworkCondition.Online, NetworkCondition.Unknown, NetworkCondition.Checking).forEach {
            assertFalse(deferAddonSubtitleRequest(it))
        }
    }
}

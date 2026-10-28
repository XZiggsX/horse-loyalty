package com.xziggsx.horseloyalty.horseinfo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xziggsx.horseloyalty.horseinfo.api.InfoTopic;
import org.junit.jupiter.api.Test;

class RevealPoliciesTest {
	@Test
	void everythingPolicyNeverLocks() {
		for (InfoTopic topic : InfoTopic.values()) {
			assertTrue(RevealPolicies.everything().lockReason(topic, null, null).isEmpty());
		}
	}
}

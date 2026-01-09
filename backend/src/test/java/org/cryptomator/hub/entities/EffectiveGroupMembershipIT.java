package org.cryptomator.hub.entities;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@QuarkusTest
class EffectiveGroupMembershipIT {

	@Inject
	User.Repository userRepo;

	@Inject
	Group.Repository groupRepo;

	@Inject
	Authority.Repository authorityRepo;

	@Inject
	EffectiveGroupMembership.Repository effectiveGroupMembershipRepo;

	@BeforeEach
	@Transactional
	void setup() {
		for (int i = 1; i <= 5; i++) {
			User u = new User();
			u.setId("u" + i);
			u.setName("User " + i);
			userRepo.persist(u);
		}

		for (int i = 1; i <= 5; i++) {
			Group g = new Group();
			g.setId("g" + i);
			g.setName("Group " + i);
			groupRepo.persist(g);
		}

		/**
		 *
		 * g1/
		 * ├─ u1/
		 * ├─ g2/
		 *    ├─ u2
		 *    ├─ u4
		 *    ├─ g3/
		 *       ├─ u3
		 *       ├─ u4
		 */
		addMembers("g1", "u1", "g2");
		addMembers("g2", "u2", "u4", "g3");
		addMembers("g3", "u3", "u4");
		effectiveGroupMembershipRepo.fullUpdate();
	}

	@AfterEach
	@Transactional
	void teardown() {
		userRepo.deleteByIds(List.of("u1", "u2", "u3", "u4", "u5"));
		groupRepo.deleteByIds(List.of( "g1", "g2", "g3", "g4", "g5"));
	}

	@Test
	@DisplayName("validate data after full update")
	@Transactional
	void testDataAfterFullUpdate() {
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g1", "u1"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g1", "u2"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g1", "u3"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g2", "u2"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g2", "u3"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g2", "u4"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g3", "u3"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g3", "u4"));

		Assertions.assertFalse(effectiveGroupMembershipRepo.isMember("g2", "u1"));
		Assertions.assertFalse(effectiveGroupMembershipRepo.isMember("g3", "u1"));
		Assertions.assertFalse(effectiveGroupMembershipRepo.isMember("g1", "u5"));
		Assertions.assertFalse(effectiveGroupMembershipRepo.isMember("g2", "u5"));
		Assertions.assertFalse(effectiveGroupMembershipRepo.isMember("g3", "u5"));

	}

	/* update groups */

	@Test
	@DisplayName("updateGroups after adding g5/u5")
	@Transactional
	void updateGroupAfterAddingG5U5() {
		addMembers("g5", "u5");
		effectiveGroupMembershipRepo.updateGroups(List.of("g5"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g5", "u5"));
	}

	@Test
	@DisplayName("updateGroups after removing g2/g3")
	@Transactional
	void updateGroupAfterRemovingG2G3() {
		removeMembers("g2", "g3");
		effectiveGroupMembershipRepo.updateGroups(List.of("g2"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g2", "u2"));
		Assertions.assertFalse(effectiveGroupMembershipRepo.isMember("g2", "g3"));
		Assertions.assertFalse(effectiveGroupMembershipRepo.isMember("g2", "u3"));
		Assertions.assertFalse(effectiveGroupMembershipRepo.isMember("g1", "g3"));
		Assertions.assertFalse(effectiveGroupMembershipRepo.isMember("g1", "u3"));
	}

	/* update users */

	@Test
	@DisplayName("updateUsers after adding g5/u5")
	@Transactional
	void updateUsersAfterAddingG5U5() {
		addMembers("g5", "u5");
		effectiveGroupMembershipRepo.updateUsers(List.of("u5"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g5", "u5"));
	}

	@Test
	@DisplayName("updateUsers after adding g3/u5")
	@Transactional
	void updateUsersAfterAddingG3U5() {
		addMembers("g3", "u5");
		effectiveGroupMembershipRepo.updateUsers(List.of("u5"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g3", "u5"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g2", "u5"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g1", "u5"));
	}

	@Test
	@DisplayName("updateUsers after removing g2/u2")
	@Transactional
	void updateUsersAfterRemovingG2U2() {
		removeMembers("g2", "u2");
		effectiveGroupMembershipRepo.updateUsers(List.of("u2"));
		Assertions.assertFalse(effectiveGroupMembershipRepo.isMember("g2", "u2"));
		Assertions.assertFalse(effectiveGroupMembershipRepo.isMember("g1", "u2"));
	}

	@Test
	@DisplayName("updateUsers after removing g2/u4")
	@Transactional
	void updateUsersAfterRemovingG2U4() {
		removeMembers("g2", "u4");
		effectiveGroupMembershipRepo.updateUsers(List.of("u4"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g3", "u4"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g2", "u4"));
		Assertions.assertTrue(effectiveGroupMembershipRepo.isMember("g1", "u4"));
	}

	/* helpers */

	private void addMembers(String groupId, String... memberIds) {
		if (memberIds.length == 0) {
			throw new IllegalArgumentException("At least one memberId must be provided");
		}
		Group group = groupRepo.findById(groupId);
		if (group == null) {
			throw new IllegalArgumentException("Group " + groupId + " does not exist");
		}
		for (String memberId : memberIds) {
			Authority member = authorityRepo.findById(memberId);
			if (member == null) {
				throw new IllegalArgumentException("Member " + memberId + " does not exist");
			}
			group.getMembers().add(member);
		}
		groupRepo.persist(group);
	}

	private void removeMembers(String groupId, String... memberIds) {
		if (memberIds.length == 0) {
			throw new IllegalArgumentException("At least one memberId must be provided");
		}
		Group group = groupRepo.findById(groupId);
		if (group == null) {
			throw new IllegalArgumentException("Group " + groupId + " does not exist");
		}
		for (String memberId : memberIds) {
			Authority member = authorityRepo.findById(memberId);
			if (member == null) {
				throw new IllegalArgumentException("Member " + memberId + " does not exist");
			}
			group.getMembers().remove(member);
		}
		groupRepo.persist(group);
	}

}
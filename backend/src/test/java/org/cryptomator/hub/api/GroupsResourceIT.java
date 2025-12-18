package org.cryptomator.hub.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.SQLException;
import java.util.List;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;

@QuarkusTest
@DisplayName("Resource /groups")
public class GroupsResourceIT {

	@Inject
	Group.Repository groupRepo;

	@Inject
	User.Repository userRepo;

	@Inject
	EffectiveGroupMembership.Repository effectiveGroupMembershipRepo;

	@BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@BeforeEach
	@Transactional
	void setupTestData() {
		var user999 = new User();
		user999.setId("user999");
		user999.setName("User 999");
		userRepo.persist(user999);

		var group999 = new Group();
		group999.setId("group999");
		group999.setName("Group 999");
		group999.getMembers().add(user999);
		groupRepo.persist(group999);

		var group1 = groupRepo.findById("group1");
		group1.getMembers().add(group999);
		groupRepo.persist(group1);
		
		effectiveGroupMembershipRepo.updateGroups(List.of("group1", "group999"));
	}

	@AfterEach
	@Transactional
	void cleanupTestData() {
		groupRepo.deleteById("group999");
		userRepo.deleteById("user999");
	}

	@Nested
	@DisplayName("As user1")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class AsAuthorzedUser1 {

		@Test
		@DisplayName("GET /groups returns 200")
		void testGetAll() {
			when().get("/groups")
					.then().statusCode(200)
					.body("id", hasItems("group1", "group2"));
		}

		@Test
		@DisplayName("GET /groups/group1/effective-members contains direct and subgroup members")
		void testGetEffectiveUsers() throws SQLException {
			when().get("/groups/{groupId}/effective-members", "group1")
					.then().statusCode(200)
					.body("id", hasItems("user1", "user999"));
		}

	}

	@Nested
	@DisplayName("As unauthenticated user")
	public class AsAnonymous {

		@DisplayName("401 Unauthorized")
		@ParameterizedTest(name = "{0} {1}")
		@CsvSource(value = {
				"GET, /users"
		})
		void testGet(String method, String path) {
			when().request(method, path)
					.then().statusCode(401);
		}

	}

}
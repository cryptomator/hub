package org.cryptomator.hub.api;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.cryptomator.hub.entities.Device;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.SQLException;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@DisplayName("Resource /devices")
public class DeviceResourceTest {

	@Inject
	AgroalDataSource dataSource;

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Nested
	@DisplayName("As user1")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class AsAuthorzedUser1 {

		@Test
		@Order(1)
		@DisplayName("PUT /devices/device1 without DTO returns 400")
		public void testCreateNoDeviceDto() {
			given().contentType(ContentType.JSON).body("")
					.when().put("/devices/{deviceId}", "device1")
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("PUT /devices/  with DTO returns 400")
		public void testCreateNoDeviceId() {
			var deviceDto = new DeviceResource.DeviceDto("device1", "Computer 1", Device.Type.DESKTOP, "publickey1", "jwe.jwe.jwe.user1.device1", "user1", Instant.parse("2020-02-20T20:20:20Z"));
			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", " ") //a whitespace
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("GET /devices/device1 returns 200")
		public void testGet1() {
			given().when().get("/devices/{deviceId}", "device1")
					.then().statusCode(200)
					.body("id", is("device1"))
					.body("name", is("Computer 1"))
					.body("userPrivateKey", is("jwe.jwe.jwe.user1.device1"));
		}

		@Test
		@Order(1)
		@DisplayName("GET /devices/legacyDevice1/legacy-access-tokens returns 200")
		public void testGetLegacyAccessTokens1() {
			given().when().get("/devices/{deviceId}/legacy-access-tokens", "legacyDevice1")
					.then().statusCode(200)
					.body("7e57c0de-0000-4000-8000-000100001111", is("legacy.jwe.jwe.vault1.device1"));
		}

		@Test
		@Order(1)
		@DisplayName("GET /devices/legacyDevice2/legacy-access-tokens returns empty list (owned by different user)")
		public void testGetLegacyAccessTokens2() {
			given().when().get("/devices/{deviceId}/legacy-access-tokens", "legacyDevice2")
					.then().statusCode(200)
					.body(is("{}"));
		}

		@Test
		@Order(1)
		@DisplayName("GET /devices/legacyDevice3/legacy-access-tokens returns 200")
		public void testGetLegacyAccessTokens3() {
			given().when().get("/devices/{deviceId}/legacy-access-tokens", "legacyDevice3")
					.then().statusCode(200)
					.body("7e57c0de-0000-4000-8000-000100002222", is("legacy.jwe.jwe.vault2.device3"));
		}

		@Test
		@Order(1)
		@DisplayName("GET /devices/noSuchDevice/legacy-access-tokens returns empty list (no such device)")
		public void testGetLegacyAccessTokens4() {
			given().when().get("/devices/{deviceId}/legacy-access-tokens", "noSuchDevice")
					.then().statusCode(200)
					.body(is("{}"));
		}

		@Test
		@Order(1)
		@DisplayName("GET /devices/device2 returns 404 (owned by other user)")
		public void testGet2() {
			given().when().get("/devices/{deviceId}", "device2")
					.then().statusCode(404);
		}

		@Test
		@Order(1)
		@DisplayName("GET /devices/noSuchDevice returns 404 (no such device)")
		public void testGetNonExistingDeviceToken() {
			when().get("/devices/{deviceId}", "noSuchDevice")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("PUT /devices/device999 returns 201 (creating new device)")
		public void testCreate999() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "device_legacy" ("id", "owner_id", "name", "type", "publickey", "creation_time")
						VALUES
							('device999', 'user1', 'Computer 999', 'DESKTOP', 'publickey999', '2020-02-20 20:20:20')
						""");
			}

			var deviceDto = new DeviceResource.DeviceDto("device999", "Computer 999", Device.Type.DESKTOP, "publickey999", "jwe.jwe.jwe.user1.device999", "user1", Instant.parse("2020-02-20T20:20:20Z"));

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device999")
					.then().statusCode(201);

			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				var rs = s.executeQuery("""
						SELECT * FROM "device_legacy" WHERE "id" = 'device999';
						""");
				Assertions.assertFalse(rs.next());
			}
		}

		@Test
		@Order(2)
		@DisplayName("PUT /devices/deviceX returns 201 (creating new device with same name as device1)")
		public void testCreateX() {
			var deviceDto = new DeviceResource.DeviceDto("deviceX", "Computer 1", Device.Type.DESKTOP, "publickey1", "jwe.jwe.jwe.user1.deviceX", "user1", Instant.parse("2020-02-20T20:20:20Z"));

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "deviceX")
					.then().statusCode(201);
		}

		@Test
		@Order(3)
		@DisplayName("PUT /devices/deviceY returns 409 (creating new device with the key of deviceX conflicts)")
		public void testCreateYWithKeyOfDeviceX() {
			var deviceDto = new DeviceResource.DeviceDto("deviceY", "Computer 2", Device.Type.DESKTOP, "publickey1", "jwe.jwe.jwe.user1.deviceX", "user1", Instant.parse("2020-02-20T20:20:20Z"));

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "deviceY")
					.then().statusCode(409);
		}

		@Test
		@Order(4)
		@DisplayName("GET /devices/device999 returns 200")
		public void testGet999AfterCreate() {
			given().when().get("/devices/{deviceId}", "device999")
					.then().statusCode(200)
					.body("id", is("device999"))
					.body("name", is("Computer 999"));
		}

		@Test
		@Order(5)
		@DisplayName("PUT /devices/device999 returns 201 (updating existing device)")
		public void testUpdate1() {
			var deviceDto = new DeviceResource.DeviceDto("device999", "Computer 999 got a new name", Device.Type.DESKTOP, "publickey999", "jwe.jwe.jwe.user1.device999", "user1", Instant.parse("2020-02-20T20:20:20Z"));

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device999")
					.then().statusCode(201);
		}

		@Test
		@Order(6)
		@DisplayName("GET /devices/device999 returns 200 (with updated name)")
		public void testGet999AfterUpdate() {
			given().when().get("/devices/{deviceId}", "device999")
					.then().statusCode(200)
					.body("id", is("device999"))
					.body("name", is("Computer 999 got a new name"));
		}

		@Test
		@Order(7)
		@DisplayName("DELETE /devices/  returns 400")
		public void testDeleteNoDeviceId() {
			when().delete("/devices/{deviceId}", " ") //a whitespace
					.then().statusCode(400);
		}

		@Test
		@Order(7)
		@DisplayName("DELETE /devices/device0 returns 404")
		public void testDeleteNotExisting() {
			when().delete("/devices/{deviceId}", "device0") //
					.then().statusCode(404);
		}

		@Test
		@Order(7)
		@DisplayName("DELETE /devices/device2 returns 404")
		public void testDeleteNotOwner() {
			when().delete("/devices/{deviceId}", "device2") //
					.then().statusCode(404);
		}

		@Test
		@Order(7)
		@DisplayName("DELETE /devices/device999 returns 204")
		public void testDeleteValid() {
			when().delete("/devices/{deviceId}", "device999") //
					.then().statusCode(204);
		}


	}

	@Nested
	@DisplayName("As unauthenticated user")
	public class AsAnonymous {

		@Test
		@DisplayName("PUT /devices/device1 returns 401")
		public void testCreate1() {
			var deviceDto = new DeviceResource.DeviceDto("device1", "Device 1", Device.Type.BROWSER, "publickey1", "jwe.jwe.jwe.user1.device1", "user1", Instant.parse("2020-02-20T20:20:20Z"));

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device1")
					.then().statusCode(401);
		}

		@Test
		@DisplayName("DELETE /devices/device1 returns 401")
		public void testDelete() {
			when().delete("/devices/{deviceId}", "device1") //
					.then().statusCode(401);
		}

	}

	@Nested
	@DisplayName("GET /devices?ids=...")
	@TestSecurity(user = "User Name 1", roles = {"user", "admin"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class GetSome {

		@Test
		@DisplayName("GET /devices returns 200 with empty body")
		public void testGetSomeEmpty() {
			when().get("/devices")
					.then().statusCode(200)
					.body("", hasSize(0));
		}

		@Test
		@DisplayName("GET /devices?ids=iDoNotExist returns 200 with empty body")
		public void testGetSomeNotExisting() {
			given().param("ids", "iDoNotExist")
					.when().get("/devices")
					.then().statusCode(200)
					.body("", hasSize(0));
		}

		@Test
		@DisplayName("GET /devices?ids=device2&ids=device3 returns 200 with body containing device2 and device3")
		public void testGetSome() {
			given().param("ids", "device2", "device3")
					.when().get("/devices")
					.then().statusCode(200)
					.body("id", containsInAnyOrder("device2", "device3"));
		}

		@Test
		@DisplayName("GET /devices?ids=device2&ids=device3 as user returns 403")
		@TestSecurity(user = "User Name 1", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		public void testGetSomeAsUser() {
			given().param("ids", "device2", "device3")
					.when().get("/devices")
					.then().statusCode(403);
		}
	}
}
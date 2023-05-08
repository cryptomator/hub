package org.cryptomator.hub.api;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
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

@QuarkusTest
@DisplayName("Resource /devices")
public class DeviceResourceTest {

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
			var deviceDto = new DeviceResource.DeviceDto("device1", "Computer 1", "publickey1", "jwe.jwe.jwe.user1.device1","user1", Instant.parse("2020-02-20T20:20:20Z"));
			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", " ") //a whitespace
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("PUT /devices/deviceX returns 409 due to non-unique name")
		public void testCreateX() {
			var deviceDto = new DeviceResource.DeviceDto("deviceX", "Computer 1", "publickey1", "jwe.jwe.jwe.user1.deviceX","user1", Instant.parse("2020-02-20T20:20:20Z"));

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "deviceX")
					.then().statusCode(409);
		}

		@Test
		@Order(1)
		@DisplayName("GET /devices/device1 returns 200")
		public void testGet1() {
			given().when().get("/devices/{deviceId}", "device1")
					.then().statusCode(200)
					.body("id", is("device1"))
					.body("name", is("Computer 1"));
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
		@DisplayName("GET /devices/device1/device-token returns 200")
		public void testGetDeviceToken1() {
			when().get("/devices/{deviceId}/device-token", "device1")
					.then().statusCode(200)
					.body(is("jwe.jwe.jwe.user1.device1"));
		}

		@Test
		@Order(1)
		@DisplayName("GET /devices/device2/device-token returns 404 (belongs to other user)")
		public void testGetDeviceToken2() {
			when().get("/devices/{deviceId}/device-token", "device2")
					.then().statusCode(404);
		}

		@Test
		@Order(1)
		@DisplayName("GET /devices/device3/device-token returns 403 (device not yet verified)")
		public void testGetDeviceToken3() {
			when().get("/devices/{deviceId}/device-token", "device3")
					.then().statusCode(403);
		}

		@Test
		@Order(1)
		@DisplayName("GET /devices/noSuchDevice/device-token returns 404 (no such device)")
		public void testGetNonExistingDeviceToken() {
			when().get("/devices/{deviceId}/device-token", "noSuchDevice")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("PUT /devices/device999 returns 201 (creating new device)")
		public void testCreate999() {
			var deviceDto = new DeviceResource.DeviceDto("device999", "Computer 999", "publickey999", "jwe.jwe.jwe.user1.device999", "user1", Instant.parse("2020-02-20T20:20:20Z"));

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device999")
					.then().statusCode(201);
		}

		@Test
		@Order(3)
		@DisplayName("GET /devices/device999 returns 200")
		public void testGet999AfterCreate() {
			given().when().get("/devices/{deviceId}", "device999")
					.then().statusCode(200)
					.body("id", is("device999"))
					.body("name", is("Computer 999"));
		}

		@Test
		@Order(4)
		@DisplayName("PUT /devices/device999 returns 201 (updating existing device)")
		public void testUpdate1() {
			var deviceDto = new DeviceResource.DeviceDto("device999", "Computer 999 got a new name", "publickey999", "jwe.jwe.jwe.user1.device999", "user1", Instant.parse("2020-02-20T20:20:20Z"));

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device999")
					.then().statusCode(201);
		}

		@Test
		@Order(5)
		@DisplayName("GET /devices/device999 returns 200 (with updated name)")
		public void testGet999AfterUpdate() {
			given().when().get("/devices/{deviceId}", "device999")
					.then().statusCode(200)
					.body("id", is("device999"))
					.body("name", is("Computer 999 got a new name"));
		}

		@Test
		@Order(6)
		@DisplayName("DELETE /devices/  returns 400")
		public void testDeleteNoDeviceId() {
			when().delete("/devices/{deviceId}", " ") //a whitespace
					.then().statusCode(400);
		}

		@Test
		@Order(6)
		@DisplayName("DELETE /devices/device0 returns 404")
		public void testDeleteNotExisting() {
			when().delete("/devices/{deviceId}", "device0") //
					.then().statusCode(404);
		}

		@Test
		@Order(6)
		@DisplayName("DELETE /devices/device2 returns 404")
		public void testDeleteNotOwner() {
			when().delete("/devices/{deviceId}", "device2") //
					.then().statusCode(404);
		}

		@Test
		@Order(6)
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
			var deviceDto = new DeviceResource.DeviceDto("device1", "Computer 1", "publickey1", "jwe.jwe.jwe.user1.device1", "user1", Instant.parse("2020-02-20T20:20:20Z"));

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

}
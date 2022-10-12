package org.cryptomator.hub.api;

import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

@QuarkusTest
@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"})
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
	public class AsAuthorzedUser1 {

		@Test
		@DisplayName("PUT /devices/device1 without DTO returns 400 ")
		public void testCreateNoDeviceDto() {
			given().contentType(ContentType.JSON).body("")
					.when().put("/devices/{deviceId}", "device1")
					.then().statusCode(400);
		}

		@Test
		@DisplayName("PUT /devices/ with DTO returns 400 ")
		public void testCreateNoDeviceId() {
			var deviceDto = new DeviceResource.DeviceDto("device1", "Computer 1", "publickey1", "", Set.of(), ZonedDateTime.parse("2020-02-20T20:20:20+00:00"));
			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "\u0020") //a whitespace
					.then().statusCode(400);
		}

		@Test
		@DisplayName("PUT /devices/device1 returns 409")
		public void testCreate1() {
			var deviceDto = new DeviceResource.DeviceDto("device1", "Computer 1", "publickey1", "owner1", Set.of(), ZonedDateTime.parse("2020-02-20T20:20:20+00:00"));

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /devices/deviceX returns 409 due to non-unique name")
		public void testCreateX() {
			var deviceDto = new DeviceResource.DeviceDto("deviceX", "Computer 1", "publickey1", "owner1", Set.of(), ZonedDateTime.parse("2020-02-20T20:20:20+00:00"));

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "deviceX")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /devices/device999 returns 201")
		public void testCreate2() {
			var deviceDto = new DeviceResource.DeviceDto("device999", "Computer 999", "publickey999", "owner1", Set.of(), ZonedDateTime.parse("2020-02-20T20:20:20+00:00"));

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device999")
					.then().statusCode(201);
		}

		@Test
		@DisplayName("DELETE /devices/ returns 400")
		public void testDeleteNoDeviceId() {
			when().delete("/devices/{deviceId}", "\u0020") //a whitespace
					.then().statusCode(400);
		}

		@Test
		@DisplayName("DELETE /devices/device0 returns 404")
		public void testDeleteNotExisting() {
			when().delete("/devices/{deviceId}", "device0") //
					.then().statusCode(404);
		}

		@Test
		@DisplayName("DELETE /devices/device2 returns 404")
		public void testDeleteNotOwner() {
			when().delete("/devices/{deviceId}", "device2") //
					.then().statusCode(404);
		}

		@Test
		@DisplayName("DELETE /devices/device1 returns 204")
		public void testDeleteValid() {
			when().delete("/devices/{deviceId}", "device1") //
					.then().statusCode(204);
		}


	}

	@Nested
	@DisplayName("As unauthenticated user")
	public class AsAnonymous {

		@Test
		@DisplayName("PUT /devices/device1 returns 401")
		public void testCreate1() {
			var deviceDto = new DeviceResource.DeviceDto("device1", "Computer 1", "publickey1", "user1", Set.of(), ZonedDateTime.parse("2020-02-20T20:20:20+00:00"));

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
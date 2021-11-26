package org.cryptomator.hub.spi;

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

import java.util.Set;

import static io.restassured.RestAssured.given;

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
	public class AsAuthorzedUser1 {

		@Test
		@DisplayName("PUT /devices/device1 returns 409")
		public void testCreate1() {
			var deviceDto = new DeviceResource.DeviceDto("device1", "Computer 1", "publickey1", "", Set.of());

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /devices/device3 returns 201")
		public void testCreate2() {
			var deviceDto = new DeviceResource.DeviceDto("device3", "Computer 3", "publickey3", "", Set.of());

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device3")
					.then().statusCode(201);
		}

	}

	@Nested
	@DisplayName("As unauthenticated user")
	public class AsAnonymous {

		@Test
		@DisplayName("PUT /devices/device1 returns 401")
		public void testCreate1() {
			var deviceDto = new DeviceResource.DeviceDto("device1", "Computer 1", "publickey1", "user1", Set.of());

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device1")
					.then().statusCode(401);
		}

	}

}
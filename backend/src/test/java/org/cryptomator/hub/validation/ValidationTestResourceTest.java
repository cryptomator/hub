package org.cryptomator.hub.validation;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static io.restassured.RestAssured.when;

@QuarkusTest
public class ValidationTestResourceTest {

	private static final String[] MALICOUS_STRINGS = {"§$%&", "<bar>", "\"; DELETE * FROM USERS", "\" src=\"http://evil.corp\""};

	@Test
	public void testOk() {
		when().get("/test/nothing")
				.then().statusCode(200);
	}

	@Nested
	@DisplayName("Test @ValidUUID")
	public class UUIDTest {

		@DisplayName("Valid uuids are accepted")
		@ParameterizedTest
		@ValueSource(strings = {"2fa854c2-e289-4a4d-9cf5-8dd81e6ae710", "EF08020E-C584-4797-BF34-1FF432E99FC8", "2fa854c2-C584-4797-bf34-1FF432E99FC8"})
		public void testUUIDvalid(String toTest) {
			when().get("/test/validuuid/{uuid}", toTest)
					.then().statusCode(200);
		}

		@DisplayName("Invalid uuids are rejected")
		@ParameterizedTest
		@ValueSource(strings = {"2G$5If2-e28b-4aTd-9ff5-8d81e6ae710", "23af23-23bc-8231", "2fa854c2-e289-4a4d-9cf5-8dd81e6ae710-23008b"})
		@ArgumentsSource(MalicousStringsProvider.class)
		public void testUUIDinvalid(String toTest) {
			when().get("/test/validuuid/{uuid}", toTest)
					.then().statusCode(400);
		}
	}

	@Nested
	@DisplayName("Test @ValidId")
	public class IdTest {

		@DisplayName("Valid ids are accepted")
		@ParameterizedTest
		@ValueSource(strings = {"2fa854c2-e289-4a4d-9cf5-8dd81e6ae710", "myPersonalId", "_-.-_"})
		public void testIdvalid(String toTest) {
			when().get("/test/validid/{id}", toTest)
					.then().statusCode(200);
		}

		@DisplayName("Invalid ids are rejected")
		@ParameterizedTest
		@ValueSource(strings = {"WHITE SPACE", "\u5207ä="})
		@ArgumentsSource(MalicousStringsProvider.class)
		public void testIdInvalid(String toTest) {
			when().get("/test/validid/{id}", toTest)
					.then().statusCode(400);
		}
	}

	@Nested
	@DisplayName("Test @ValidPseudoBase64")
	public class PseudoBase64Test {

		@DisplayName("Valid pseudo-base64 strings are accepted")
		@ParameterizedTest
		@ValueSource(strings = {"abcdefghijklmnopqrstuvwxyz0123456789+/", "bGlnaHQgd29yaw==", "x======"})
		public void testPseudoBase64Valid(String toTest) {
			when().get("/test/validpseudobase64/{pb64String}", toTest)
					.then().statusCode(200);
		}

		@DisplayName("Invalid pseudo-base64 strings are rejected")
		@ParameterizedTest
		@ValueSource(strings = {"\u5207ä=", "foo_-", "abc==abc", "==="})
		@ArgumentsSource(MalicousStringsProvider.class)
		public void testPseudoBase64Invalid(String toTest) {
			when().get("/test/validpseudobase64/{pb64String}", toTest)
					.then().statusCode(400);
		}
	}

	@Nested
	@DisplayName("Test @ValidPseudoBase64Url")
	public class PseudoBase64UrlTest {

		@DisplayName("Valid pseudo-base64url strings are accepted")
		@ParameterizedTest
		@ValueSource(strings = {"abcdefghijklmnopqrstuvwxyz0123456789-_", "bGln-HQgd2_yaw==", "-======"})
		public void testPseudoBase64UrlValid(String toTest) {
			when().get("/test/validpseudobase64url/{pb64urlString}", toTest)
					.then().statusCode(200);
		}

		@DisplayName("Invalid pseudo-base64url strings are rejected")
		@ParameterizedTest
		@ValueSource(strings = {"foo+/", "\u5207ä=", "abc==abc", "==="})
		@ArgumentsSource(MalicousStringsProvider.class)
		public void testPseudoBase64UrlInvalid(String toTest) {
			when().get("/test/validpseudobase64url/{pb64urlString}", toTest)
					.then().statusCode(400);
		}
	}

	static class MalicousStringsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
			return Arrays.stream(MALICOUS_STRINGS).map(Arguments::of);
		}
	}
}

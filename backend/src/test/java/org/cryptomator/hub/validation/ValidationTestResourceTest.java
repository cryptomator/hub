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

	private static final String[] MALICOUS_STRINGS = {"§$%&", "<bar>", "\"; DELETE * FROM USERS;--", "\" src=\"http://evil.corp\""};

	@Test
	public void testOk() {
		when().get("/test/nothing")
				.then().statusCode(200);
	}

	@Nested
	@DisplayName("Test @ValidId")
	public class IdTest {

		@DisplayName("Valid ids are accepted")
		@ParameterizedTest
		@ValueSource(strings = {"2fa854c2-e289-4a4d-9cf5-8dd81e6ae710", "myPersonalId", "_-.-_"})
		public void testIdValid(String toTest) {
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
	@DisplayName("Test @ValidName")
	public class NameTest {

		@Test
		@DisplayName("Valid names are accepted")
		public void testNameValid() {
			when().get("/test/validname/{name}", "_foo-\u5207 BAR")
					.then().statusCode(200);
		}

		@DisplayName("Invalid names are rejected")
		@ParameterizedTest
		@ValueSource(strings = {"foo**r", ".;/()"})
		@ArgumentsSource(MalicousStringsProvider.class)
		public void testNameInvalid(String toTest) {
			when().get("/test/validname/{name}", toTest)
					.then().statusCode(400);
		}
	}

	@Nested
	@DisplayName("Test @OnlyBase64Chars")
	public class Base64CharsTest {

		@DisplayName("Strings only containing base64-chars are accepted")
		@ParameterizedTest
		@ValueSource(strings = {"abcdefghijklmnopqrstuvwxyz0123456789+/", "bGlnaHQgd29yaw==", "x======"})
		public void testOnlyBase64CharsValid(String toTest) {
			when().get("/test/onlybase64chars/{b64String}", toTest)
					.then().statusCode(200);
		}

		@DisplayName("Strings containing not-base64-chars (or wrong order) are rejected")
		@ParameterizedTest
		@ValueSource(strings = {"\u5207ä=", "foo_-", "abc==abc", "==="})
		@ArgumentsSource(MalicousStringsProvider.class)
		public void testOnlyBase64CharsInvalid(String toTest) {
			when().get("/test/onlybase64chars/{b64String}", toTest)
					.then().statusCode(400);
		}
	}

	@Nested
	@DisplayName("Test @OnlyBase64UrlChars")
	public class Base64UrlCharsTest {

		@DisplayName("Strings only containing base64url-chars are accepted")
		@ParameterizedTest
		@ValueSource(strings = {"abcdefghijklmnopqrstuvwxyz0123456789-_", "bGln-HQgd2_yaw==", "-======"})
		public void testOnlyBase64UrlCharsValid(String toTest) {
			when().get("/test/onlybase64urlchars/{b64String}", toTest)
					.then().statusCode(200);
		}

		@DisplayName("Strings containing not-base64url-chars (or wrong order) are rejected")
		@ParameterizedTest
		@ValueSource(strings = {"foo+/", "\u5207ä=", "abc==abc", "==="})
		@ArgumentsSource(MalicousStringsProvider.class)
		public void testOnlyBase64UrlCharsInvalid(String toTest) {
			when().get("/test/onlybase64urlchars/{b64String}", toTest)
					.then().statusCode(400);
		}
	}

	@Nested
	@DisplayName("Test @ValidJWE")
	public class JWETest {

		@DisplayName("Valid JWE compact serializations strings are accepted")
		@ParameterizedTest
		@ValueSource(strings = {"foo=.b4r.baz==.bas.asd=", "fo0=...bar.", "foo.=.=.bar.===="})
		public void testJWEValid(String toTest) {
			when().get("/test/validjwe/{jwe}", toTest)
					.then().statusCode(200);
		}

		@DisplayName("Invalid JWE compact serializations strings are rejected")
		@ParameterizedTest
		@ValueSource(strings = {"foo=.bar.baz.bas", ".bar=.baz.bas.asd=", "föö=.bar.baz.bas.asd", "foo=bar.baz.bas.asd.qwe"})
		@ArgumentsSource(MalicousStringsProvider.class)
		public void testJWEInvalid(String toTest) {
			when().get("/test/validjwe/{jwe}", toTest)
					.then().statusCode(400);
		}
	}

	@Nested
	@DisplayName("Test @ValidJWS")
	public class JWSTest {

		@DisplayName("Valid JWS compact serializations strings are accepted")
		@ParameterizedTest
		@ValueSource(strings = {"fo0.b4r.baz", "f0o..", "fo0.=.bar="})
		public void testJWSValid(String toTest) {
			when().get("/test/validjws/{jws}", toTest)
					.then().statusCode(200);
		}

		@DisplayName("Invalid JWS compact serializations strings are rejected")
		@ParameterizedTest
		@ValueSource(strings = {"foo=.bar", ".bar=.baz", "föö=.bar.baz", "foo=bar.baz.bas"})
		@ArgumentsSource(MalicousStringsProvider.class)
		public void testJWSInvalid(String toTest) {
			when().get("/test/validjws/{jws}", toTest)
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

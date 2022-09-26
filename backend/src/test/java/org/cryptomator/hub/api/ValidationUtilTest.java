package org.cryptomator.hub.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

public class ValidationUtilTest {

	@ParameterizedTest
	@DisplayName("Test id pattern with matches")
	@ValueSource(strings = {"asd-asd", "029foobar-b0z"})
	public void testIdPatternValid(String matches) {
		var result = Pattern.matches(ValidationUtil.ID_PATTERN, matches);
		Assertions.assertTrue(result);
	}

	@ParameterizedTest
	@DisplayName("Test id pattern with no-matches")
	@ValueSource(strings = {"foo\u5207", "asd§", "asd$", "%&asd", "as02nmf-laksdj.", "foo/\\bar", "+foobarbaz#-\"", "<bar>"})
	public void testIdPatternInvalid(String fails) {
		var result = Pattern.matches(ValidationUtil.ID_PATTERN, fails);
		Assertions.assertFalse(result);
	}

	@ParameterizedTest
	@DisplayName("Test name pattern with matches")
	@ValueSource(strings = {"foo", "\u5207\u03b1\u00f6", "foo-bar", "Baz Baz Baz", "1970"})
	public void testNamePatternValid(String matches) {
		var result = Pattern.matches(ValidationUtil.NAME_PATTERN, matches);
		Assertions.assertTrue(result);
	}

	@ParameterizedTest
	@DisplayName("Test name pattern with no-matches")
	@ValueSource(strings = {"<foo>", "foo.bar", "$$baz$$", "%&§asd", "foo/\\bar", "+foobarbaz#-\""})
	public void testNamePatternInvalid(String fails) {
		var result = Pattern.matches(ValidationUtil.NAME_PATTERN, fails);
		Assertions.assertFalse(result);
	}
}

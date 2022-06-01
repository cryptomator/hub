package org.cryptomator.hub.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ConfigResourceTest {

	ConfigResource configResource;

	@BeforeEach
	public void init() {
		this.configResource = new ConfigResource();
	}

	@ParameterizedTest
	@CsvSource({"foobar,foo,baz,bazbar",
			"foobar,baz,bar,foobar",
			"foo,foo,bar,bar",
			"foo,'',bar,barfoo",
			"'',baz,bar,''"})
	void testReplacePrefix(String str, String prefix, String replacement, String expected) {
		String out = configResource.replacePrefix(str, prefix, replacement);

		Assertions.assertEquals(expected, out);
	}

	@ParameterizedTest
	@CsvSource({"foo,foo",
			"foo/,foo",
			"foo//,foo/",
			"'',''",
			"/,''"})
	void testTrimTrailingSlash(String in, String expected) {
		String out = configResource.trimTrailingSlash(in);

		Assertions.assertEquals(expected, out);
	}
}
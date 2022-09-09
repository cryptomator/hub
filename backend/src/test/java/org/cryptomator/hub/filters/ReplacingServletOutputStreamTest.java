package org.cryptomator.hub.filters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ReplacingServletOutputStreamTest {

	@ParameterizedTest
	@CsvSource({
			"hello world!, foo, bar, hello world!",
			"hello foo!, foo, bar, hello bar!",
			"hello fffooo!, foo, bar, hello ffbaro!",
			"fofoo foo foo fo, foo, bar, fobar bar bar fo",
			"fofoo foo foo fo, foo foo, bar, fobar foo fo",
	})
	public void testReplaceByteSequence(String input, String search, String replace, String expected) throws IOException {
		var buf = new ByteArrayOutputStream();
		var delegate = new ServletOutputStream() {

			@Override
			public void write(int b) {
				buf.write(b);
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setWriteListener(WriteListener writeListener) {
				// no-op
			}
		};

		var replacingOutputStream = new ReplacingServletOutputStream(delegate, search.getBytes(), replace.getBytes());

		replacingOutputStream.write(input.getBytes());
		replacingOutputStream.flush();
		Assertions.assertEquals(expected, buf.toString());
	}

}
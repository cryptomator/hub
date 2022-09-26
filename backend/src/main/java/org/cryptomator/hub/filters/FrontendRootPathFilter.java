package org.cryptomator.hub.filters;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Replaces occurences of <code>&lt;base href="/"&gt;</code> with the corresponding configuration value from <code>hub.public-root-path</code>.
 */
public class FrontendRootPathFilter extends HttpFilter {

	@Inject
	@ConfigProperty(name = "hub.public-root-path", defaultValue = "")
	Provider<String> publicRootPath;

	@Override
	protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
		var capturedResponse = new CapturingResponseWrapper(res);
		chain.doFilter(req, capturedResponse);
		String content = capturedResponse.getCaptureAsString(); // This uses response character encoding.
		String replacedContent = content.replace("<base href=\"/\"/>", "<base href=\"%s\"/>".formatted(publicRootPath.get()));
		res.setContentLength(replacedContent.length());
		res.getWriter().write(replacedContent);
		res.getWriter().flush();
	}

	// Taken from https://stackoverflow.com/a/23381235/4014509
	private static class CapturingResponseWrapper extends HttpServletResponseWrapper {

		private final ByteArrayOutputStream capture;
		private ServletOutputStream output;
		private PrintWriter writer;

		public CapturingResponseWrapper(HttpServletResponse response) {
			super(response);
			capture = new ByteArrayOutputStream(response.getBufferSize());
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (output == null) {
				output = new ServletOutputStream() {
					@Override
					public boolean isReady() {
						return true;
					}

					@Override
					public void setWriteListener(WriteListener writeListener) {
						// no-op
					}

					@Override
					public void write(int b) {
						capture.write(b);
					}

					@Override
					public void flush() throws IOException {
						capture.flush();
					}

					@Override
					public void close() throws IOException {
						capture.close();
					}
				};
			}
			return output;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (writer == null) {
				writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
			}
			return writer;
		}

		@Override
		public void flushBuffer() throws IOException {
			if (writer != null) {
				writer.flush();
			} else if (output != null) {
				output.flush();
			}
		}

		public byte[] getCaptureAsBytes() throws IOException {
			if (writer != null) {
				writer.close();
			} else if (output != null) {
				output.close();
			}

			return capture.toByteArray();
		}

		public String getCaptureAsString() throws IOException {
			return new String(getCaptureAsBytes(), getCharacterEncoding());
		}

	}
}

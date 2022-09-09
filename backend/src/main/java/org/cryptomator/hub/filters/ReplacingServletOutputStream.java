package org.cryptomator.hub.filters;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;

class ReplacingServletOutputStream extends ServletOutputStream {

	private final ServletOutputStream delegate;
	private final byte[] search;
	private final byte[] replacement;
	private final byte[] ringBuffer;
	private int r = 0, w = -1; // ringBuffer read and write positions
	private int bytesWritten = 0;

	public ReplacingServletOutputStream(ServletOutputStream delegate, byte[] search, byte[] replacement) {
		this.delegate = delegate;
		this.search = search;
		this.replacement = replacement;
		this.ringBuffer = new byte[search.length];
	}

	@Override
	public boolean isReady() {
		return delegate.isReady();
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {
		delegate.setWriteListener(writeListener);
	}

	@Override
	public void write(int b) throws IOException {
		ringBuffer[++w % ringBuffer.length] = (byte) b;
		int n = w - r + 1;
		if (n >= search.length) {
			if (foundMatch()) {
				delegate.write(replacement);
				bytesWritten += replacement.length;
				r += search.length;
			} else {
				delegate.write(ringBuffer[r++ % ringBuffer.length]);
				bytesWritten++;
			}
		}
	}

	@Override
	public void flush() throws IOException {
		while (r <= w) {
			delegate.write(ringBuffer[r++ % ringBuffer.length]);
			bytesWritten++;
		}
		delegate.flush();
	}

	@Override
	public void close() throws IOException {
		flush();
		delegate.close();
	}

	public int getBytesWritten() {
		return bytesWritten;
	}

	private boolean foundMatch() {
		for (int i = 0; i < search.length; i++) {
			if (search[i] != ringBuffer[(r + i) % ringBuffer.length]) {
				return false;
			}
		}
		return true;
	}
}

package jdwproxy;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Endpoint {
	private InputStream m_input;
	private OutputStream m_output;

	public Endpoint(InputStream input, OutputStream output) throws NullPointerException {
		if (input == null || output == null) {
			throw new NullPointerException();
		}

		m_input = input;
		m_output = output;
	}

	public void close() throws IOException {
		m_input.close();
		m_output.close();
	}

	public byte[] read(int length) throws IOException {
		byte[] retval = new byte[length];
		int i = 0;
		while (i < length) {
			int cb = m_input.read(retval, i, length - i);
			if (cb <= 0) {
				throw new IOException(String.format("Underlying stream aborted, failed to read %d bytes, actual read %d", length, i));
			}
			i += cb;
		}
		return retval;
	}

	public String readString(int ccb) throws IOException {
		return new String(this.read(ccb), "US-ASCII");
	}

	public void write(byte[] buffer) throws IOException {
		m_output.write(buffer);
	}

	public void writeString(String str) throws IOException {
		this.write(str.getBytes("US-ASCII"));
	}
}

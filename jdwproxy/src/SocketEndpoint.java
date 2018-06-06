package jdwproxy;

import java.io.IOException;
import java.net.Socket;

public class SocketEndpoint extends Endpoint {
	private Socket m_socket;
	private String m_repr;

	public SocketEndpoint(Socket socket) throws IOException {
		super(socket.getInputStream(), socket.getOutputStream());
		m_socket = socket;
		m_repr = String.format("%s | %s", socket.getRemoteSocketAddress(), socket.getLocalSocketAddress());
	}

	public void close() throws IOException {
		super.close();
		m_socket.close();
	}

	public String toString() {
		return m_repr;
	}
}

package jdwproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class MainClass {
	static Socket connect(String hostport) throws IOException {
		int idx = hostport.lastIndexOf(":");
		String host = hostport.substring(0, idx);
		int port = Integer.parseInt(hostport.substring(idx + 1));
		return connect(host, port);
	}

	static Socket connect(String host, int port) throws IOException {
		InetSocketAddress addrinfo = new InetSocketAddress(host, port);
		return new Socket(addrinfo.getAddress(), addrinfo.getPort());
	}

	static ServerSocket listen(String hostport) throws IOException {
		return listen(hostport, 0);
	}

	static ServerSocket listen(String hostport, int backlog) throws IOException {
		int idx = hostport.lastIndexOf(":");
		String host = hostport.substring(0, idx);
		int port = Integer.parseInt(hostport.substring(idx + 1));
		return listen(host, port, backlog);
	}

	static ServerSocket listen(String host, int port, int backlog) throws IOException {
		InetSocketAddress addrinfo = new InetSocketAddress(host, port);
		return new ServerSocket(addrinfo.getPort(), backlog, addrinfo.getAddress());
	}

	public static void main(String args[]) throws Exception {
		ArrayList<String> argv = new ArrayList<>(Arrays.asList(args));
		DebugEngine.Mode engineMode = DebugEngine.Mode.RELAY;
		switch (argv.size()) {
		case 2:
			argv.add(0, "relay");
		case 3:
			String mode = argv.get(0);
			if (mode.equals("relay")) {
				break;
			}
			if (mode.equals("proxy")) {
				engineMode = DebugEngine.Mode.PROXY;
				break;
			}
			if (mode.equals("stub")) {
				engineMode = DebugEngine.Mode.STUB;
				break;
			}
		default:
			System.err.printf("usage: jdwproxy [relay | proxy | stub] <binding host>:<port> <address>:<port>\n");
			System.exit(-1);
		}
		ServerSocket server = listen(argv.get(1));
		SocketEndpoint incomingEndpoint = new SocketEndpoint(server.accept());
		System.out.printf("incoming connection established from %s\n", incomingEndpoint);

		SocketEndpoint outgoingEndpoint = new SocketEndpoint(connect(argv.get(2)));
		System.out.printf("outging connection established to %s\n", outgoingEndpoint);

		DebugEngine engine = new DebugEngine(incomingEndpoint, outgoingEndpoint, engineMode);
		engine.loop();
	}
}

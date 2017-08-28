package jdbg;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

public class InteractiveConsole implements Runnable {
	private static final int CONSOLE_STARTED = 0x0;
	private static final int CONSOLE_REQUEST_FOR_INPUT = 0x1;
	private static final int CONSOLE_REQUEST_FOR_EXIT = 0x2;
	private static final int CONSOLE_LINE_RECEIVED = 0x4;
	private static final int CONSOLE_EOF_RECEIVED = 0x8;
	private static final int CONSOLE_UNKNOWN_ERROR = 0x1000;

	private String m_input = null;
	private String m_prompt = null;
	private int m_status = CONSOLE_STARTED;

	@Override
	public synchronized void run() {
		// TODO: do we need to intercept SIGTERM (Ctrl+C)?
		try (
			Reader reader = new InputStreamReader(System.in);
			BufferedReader bufferedReader = new BufferedReader(reader)
		) {
			while (true) {
				while ((m_status & (CONSOLE_REQUEST_FOR_INPUT | CONSOLE_REQUEST_FOR_EXIT)) == 0) {
					this.wait();
				}
				if (CONSOLE_REQUEST_FOR_EXIT != m_status) {
					System.out.print(m_prompt);
					m_input = bufferedReader.readLine();
					m_status = null == m_input ? CONSOLE_EOF_RECEIVED : CONSOLE_LINE_RECEIVED;
				}
				this.notify();
				if ((m_status & (CONSOLE_REQUEST_FOR_EXIT | CONSOLE_EOF_RECEIVED)) != 0) {
					break;
				}
			}
		} catch (InterruptedException | IOException ex) {
			ex.printStackTrace();
			m_status = CONSOLE_UNKNOWN_ERROR;
			this.notify();
		}
	}

	public synchronized String getInput(String prompt) throws InterruptedException {
		m_prompt = prompt;
		m_status = CONSOLE_REQUEST_FOR_INPUT;
		this.notify();
		while (CONSOLE_REQUEST_FOR_INPUT == m_status) {
			this.wait();
		}
		return m_input;
	}

	public synchronized void close() {
		m_status = CONSOLE_REQUEST_FOR_EXIT;
		this.notify();
	}
}

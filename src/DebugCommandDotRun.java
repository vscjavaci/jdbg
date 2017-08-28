package jdbg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class DebugCommandDotRun extends DebugCommand {
	@Override
	public int execute(DebugEngine engine) {
		String fileName = this.getArguments().substring(1);
		File file = new File(fileName);
		try (
			Reader reader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(reader)
		) {
			return engine.runScript(bufferedReader, fileName, this);
		} catch (IOException ex) {
			// TODO: print command stack
			ex.printStackTrace();
		} finally {
		}
		return DebugEngine.JDBG_COMMAND_FAILED;
	}
}

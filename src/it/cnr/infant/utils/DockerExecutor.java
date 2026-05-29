package it.cnr.infant.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

public class DockerExecutor {

	/**
	 * Executes a docker command and returns the console output.
	 *
	 * @param command Full docker command as a String
	 * @return Console output (stdout + stderr)
	 * @throws IOException
	 * @throws InterruptedException
	 */

	public static String runDockerCommand(String command) throws Exception {

		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

		ProcessBuilder pb = isWindows ? new ProcessBuilder("cmd.exe", "/c", command)
				: new ProcessBuilder("bash", "-c", command);

		pb.redirectErrorStream(true);

		Process process = pb.start();

		StringBuilder output = new StringBuilder();
		AtomicBoolean completed = new AtomicBoolean(false);

		Thread readerThread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

				String line;

				while ((line = reader.readLine()) != null) {
					output.append(line).append(System.lineSeparator());
					System.out.println("[DOCKER] " + line);

					if (line.contains("'▶ Confidence'") && line.contains("'avg_logprob'")
							&& line.contains("'compression_ratio'")) {
						completed.set(true);
						process.destroy();
						break;
					}
				}

			} catch (Exception e) {
				output.append(e.getMessage()).append(System.lineSeparator());
			}
		});

		readerThread.start();

		int maxSeconds = 600;

		for (int i = 0; i < maxSeconds; i++) {

			if (completed.get()) {
				break;
			}

			if (!process.isAlive()) {
				break;
			}

			Thread.sleep(1000);
		}

		if (process.isAlive()) {
			process.destroyForcibly();
		}

		readerThread.join(3000);

		output.append("Exit Code: ");

		if (process.isAlive()) {
			output.append("KILLED");
		} else {
			output.append(process.exitValue());
		}

		return output.toString();
	}
}

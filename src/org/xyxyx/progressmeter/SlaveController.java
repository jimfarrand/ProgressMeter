package org.xyxyx.progressmeter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class SlaveController {
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException {
		if(args.length < 1) {
			throw new IllegalArgumentException("<cmd> [arg1 [arg2 [arg3 ...]]]");
		}
		
		List<String> command = new ArrayList<String>();
		for(int i = 0; i < args.length; i++) {
			command.add(args[i]);
		}
		
		String id = generateId(command);
		
		final ProcessBuilder processBuilder = new ProcessBuilder(command);
		final Process process = processBuilder.start();
		final List<BufferedReader> readers = new ArrayList<BufferedReader>();
		readers.add(new BufferedReader(new InputStreamReader(process.getInputStream())));
		readers.add(new BufferedReader(new InputStreamReader(process.getErrorStream())));

		final StreamConnector streamConnector = new StreamConnector(System.in, process.getOutputStream());
		streamConnector.start();
		
		final ProgressMeter progressMeter = new ProgressMeter(readers, new OutputStreamWriter(System.out), id);
		progressMeter.start();
		final int status = process.waitFor();
		progressMeter.cleanup(status == 0);
		System.exit(status);
	}

	private static String generateId(final List<String> command) throws NoSuchAlgorithmException {
		MessageDigest instance = MessageDigest.getInstance("MD5");
		
		String property = System.getProperty("pm.id");
		if(property != null) {
			return property;
		}
		
		if(currentDirSignificant()) {
			instance.update(System.getProperty("user.dir").getBytes());
			instance.update((byte)0);
		}
		
		if(argsSignificant()) {
			for(final String commandPart : command) {
				instance.update(commandPart.getBytes());
				instance.update((byte)0);
			}
		} else {
			instance.update(command.get(0).getBytes());
			instance.update((byte)0);
		}
		
		return new BigInteger(1, instance.digest()).toString(16);
	}

	private static boolean argsSignificant() {
		String property = System.getProperty("pm.args.significant");
		if(property == null) {
			return true;
		} else {
			return Boolean.parseBoolean(property);
		}
	}

	private static boolean currentDirSignificant() {
		String property = System.getProperty("pm.cwd.significant");
		if(property == null) {
			return false;
		} else {
			return Boolean.parseBoolean(property);
		}
	}
}

package org.xyxyx.progressmeter;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class PipeController {
	
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		if(args.length != 1) {
			throw new IllegalArgumentException("<id>");
		} 
		
		final ProgressMeter meter = new ProgressMeter(args[0]);
		meter.start();
		meter.cleanup(true);
		
	}
}

package org.xyxyx.progressmeter;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProgressMonitor {
	private final ProgressSerialiser progressSerialiser;
	private final MessageDigest digester;
	
	/** Time we started monitoring. */
	private final double startTime;
	
	/** The amount of time this process took on it's last run. */
	private final double lastTotalTime;
	
	private final Map<BigInteger, Double> lastLineTimes = new HashMap<BigInteger, Double>();
	
	/** The "current" time, being used for calculation.  Recorded so that we can ensure that
	 * several calculations all use the same current time, for consistency. */
	private double currentTotalTime;
	
	//private double lastLineTime;
	
	private final Map<BigInteger, Double> currentLineTimes = new HashMap<BigInteger, Double>();
	private final Set<BigInteger> ignoreLines = new HashSet<BigInteger>();
	private double fractionDone = -1;
	
	
	public ProgressMonitor(ProgressSerialiser progressSerialiser) throws IOException, NoSuchAlgorithmException {
		this.progressSerialiser = progressSerialiser;
		this.digester = MessageDigest.getInstance("MD5");
		lastTotalTime = progressSerialiser.readProgressData(lastLineTimes);
		this.startTime = System.currentTimeMillis();
	}

	public void registerLine(String line) {
		updateTotalTime();
//		lastLineTime = currentTotalTime;
		
		final BigInteger hash = hash(line);
		if(!ignoreLines.contains(hash)) {
			if(currentLineTimes.containsKey(hash)) {
				currentLineTimes.remove(hash);
				ignoreLines.add(hash);
			} else {
				currentLineTimes.put(hash, currentTotalTime);
	
				if(lastTotalTime > 0) {
					final Double lastLineTime = lastLineTimes.get(hash);
					if(lastLineTime != null) {
						fractionDone = lastLineTime / lastTotalTime;
					}
				}
			}
		}
	}
	
	public void updateTotalTime() {
		currentTotalTime = System.currentTimeMillis() - startTime;
	}

	public void registerEnd() {
		fractionDone = 1;
	}
	
	private BigInteger hash(String line) {
		final byte[] bytes = line.getBytes();
		if(bytes.length == 0) {
			return BigInteger.ZERO;
		} else if(bytes.length < digester.getDigestLength()) {
			return new BigInteger(-1, bytes);
		} else {
			final byte[] digest = digester.digest(bytes);
			digester.reset();
			return new BigInteger(1, digest);
		}
	}

	public void save() throws IOException {
		progressSerialiser.writeProgressData(currentTotalTime, currentLineTimes);
	}

	public double getFractionDone() {
		return fractionDone;
	}

	public double getElapsedTime() {
		return currentTotalTime;
	}

	public double getEstimatedTime() {
		if(fractionDone < 0) {
			return -1;
		} else {
			return currentTotalTime / fractionDone;
		}
	}

	public double getRemainingTime() {
		if(fractionDone < 0) {
			return -1;
		} else {
			//return getEstimatedTime() - getElapsedTime() - getTimeSinceLastLine();
			return getEstimatedTime() - getElapsedTime();
		}
	}

//	public double getTimeSinceLastLine() {
//		return lastLineTime - currentTotalTime;
//	}
	
}

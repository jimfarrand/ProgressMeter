package org.xyxyx.progressmeter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

public interface ProgressSerialiser {

	public abstract double readProgressData(
			final Map<BigInteger, Double> lineTimes) throws IOException;

	public abstract void writeProgressData(double totalTime,
			Map<BigInteger, Double> lineTimes) throws IOException;

}
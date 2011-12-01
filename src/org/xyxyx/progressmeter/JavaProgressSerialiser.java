package org.xyxyx.progressmeter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class JavaProgressSerialiser implements ProgressSerialiser {

	private final String serialiserId;

	private static class SerialisedProgress implements Serializable {
		
		private static final long serialVersionUID = -398590478722677400L;
		private final double totalTime;
		private final Map<BigInteger, Double> lineTimes;
		
		public SerialisedProgress(Double totalTime,
				Map<BigInteger, Double> lineTimes) {
			this.totalTime = totalTime;
			this.lineTimes = lineTimes;
		}
		

	}
	
	public JavaProgressSerialiser(String serialiserId) {
		this.serialiserId = serialiserId;
	}

	/* (non-Javadoc)
	 * @see org.xyxyx.progressmeter.ProgressSerialiser#readProgressData(java.util.Map)
	 */
	@Override
	public double readProgressData(
			final Map<BigInteger, Double> lineTimes) throws IOException {
		
		try {
			final ObjectInputStream objectInputStream = new ObjectInputStream(new GZIPInputStream(new FileInputStream(getDataFile())));
			final SerialisedProgress progressData = (SerialisedProgress) objectInputStream.readObject();
			lineTimes.putAll(progressData.lineTimes);
			objectInputStream.close();
			return progressData.totalTime;
		} catch(FileNotFoundException e) {
			return -1;
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
		
	}

	private File getDataFile() {
		return new File(System.getProperty("user.home") + File.separator + ".progressmeter" + File.separator + serialiserId);
	}

	/* (non-Javadoc)
	 * @see org.xyxyx.progressmeter.ProgressSerialiser#writeProgressData(double, java.util.Map)
	 */
	@Override
	public void writeProgressData(double totalTime,
			Map<BigInteger, Double> lineTimes) throws IOException {

		final ObjectOutputStream objectOutputStream = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(getDataFile())));
		final SerialisedProgress progressData = new SerialisedProgress(totalTime, lineTimes);
		objectOutputStream.writeObject(progressData);
		objectOutputStream.close();
	}
}

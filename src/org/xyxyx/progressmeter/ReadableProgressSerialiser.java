package org.xyxyx.progressmeter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ReadableProgressSerialiser implements ProgressSerialiser{
	private final String serialiserId;
	
	public ReadableProgressSerialiser(String serialiserId) {
		this.serialiserId = serialiserId;
	}
	
	@Override
	public double readProgressData(Map<BigInteger, Double> lineTimes)
			throws IOException {
		
		try {
			final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(getDataFile()))));
			try {
			final double total = Double.parseDouble(bufferedReader.readLine());
				String line;
				while(null != (line = bufferedReader.readLine())) {
					String[] split = line.split(":");
					lineTimes.put(new BigInteger(split[1], 16), Double.parseDouble(split[0]));
				}
				return total;
			} finally {
				bufferedReader.close();
			}
		} catch(FileNotFoundException e) {
			return -1;
		}
	}

	@Override
	public void writeProgressData(double totalTime,
			Map<BigInteger, Double> lineTimes) throws IOException {
		final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(getDataFile()))));
		try {
			bufferedWriter.append(Double.toString(totalTime));
			bufferedWriter.append('\n');
			for(Map.Entry<BigInteger, Double> entry : lineTimes.entrySet()) {
				bufferedWriter.append(Double.toString(entry.getValue()));
				bufferedWriter.append(':');
				bufferedWriter.append(entry.getKey().toString(16));
				bufferedWriter.append('\n');
			}
		} finally {
			bufferedWriter.close();
		}
	}

	private File getDataFile() {
		return new File(System.getProperty("user.home") + File.separator + ".progressmeter" + File.separator + serialiserId);
	}
	
}

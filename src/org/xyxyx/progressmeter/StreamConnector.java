package org.xyxyx.progressmeter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamConnector {

	private final InputStream in;
	private final OutputStream out;
	private IOException exception;
	private Thread thread;
	
	public StreamConnector(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}

	public void start() {
		thread = new Thread(new Runnable() {
			

			@Override
			public void run() {
				final byte[] buffer = new byte[4096];
				int len;
				try {
					try {
						while(-1 != (len = in.read(buffer))) {
							out.write(buffer, 0, len);
						}
					} finally {
						out.flush();
					}
					
					out.close();
					in.close();
					
				} catch (IOException e) {
					StreamConnector.this.exception = e;
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}
}

package org.xyxyx.progressmeter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProgressMeter {
	

	private final ProgressMonitor progressMonitor;
	private final ProgressView view;
	private final OutputStreamWriter out;
	private final BufferedReader[] in;
	private final Thread[] handlers;
	
	
	public ProgressMeter(String serialiserId) throws IOException, NoSuchAlgorithmException {
		this(Collections.singleton(new BufferedReader(
				new InputStreamReader(System.in))),
				new OutputStreamWriter(System.out),
				serialiserId);
	}
	
	public ProgressMeter(
			Collection<BufferedReader> in,
			OutputStreamWriter out,
			String serialiserId) throws IOException, NoSuchAlgorithmException {
		
		this.in = in.toArray(new BufferedReader[in.size()]);
		this.out = out;
		this.progressMonitor = new ProgressMonitor(new JavaProgressSerialiser(serialiserId));
		this.handlers = new Thread[this.in.length];
		for(int i = 0; i < this.in.length; i++) {
			final BufferedReader myIn = this.in[i];
			handlers[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					handleHandleIO(myIn);
				}
			});
		}
		
		final List<ProgressView.ProgressFormatPart> lineFormat = new ArrayList<ProgressView.ProgressFormatPart>();
		lineFormat.add(ProgressView.LINE_FORMAT_PART);
		
		final List<ProgressView.ProgressFormatPart> progressFormat = new ArrayList<ProgressView.ProgressFormatPart>();
		
		progressFormat.add(ProgressView.PERCENT_DONE_FORMAT_PART);
		progressFormat.add(new ProgressView.StringFormatPart("% "));
		progressFormat.add(new ProgressView.ProgressBar(40));
		progressFormat.add(new ProgressView.StringFormatPart(" "));
		progressFormat.add(ProgressView.ELAPSED_TIME_FORMAT_PART);
		progressFormat.add(new ProgressView.StringFormatPart(" / "));
		progressFormat.add(ProgressView.REMAINING_FORMAT_PART);
		progressFormat.add(new ProgressView.StringFormatPart(" / "));
		progressFormat.add(ProgressView.TOTAL_ESTIMATED_TIME_FORMAT_PART);
		
		this.view = new ProgressView(progressMonitor, out, lineFormat, progressFormat);
	}

	private void handleHandleIO(BufferedReader in) {
		String line;
		try {
			try {
				while(null != (line = in.readLine())) {
					synchronized (progressMonitor) {
						progressMonitor.registerLine(line);
					}
					
					synchronized (view) {
						view.outputLine(line);
						view.outputProgress();
					}
				}
			} finally {
				
				in.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		for(Thread handler : handlers) {
			handler.start();
		}
	}
	
	public void cleanup(boolean complete) throws IOException {
		for(Thread handler : handlers) {
			try {
				handler.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if(complete) {
			synchronized (progressMonitor) {
				progressMonitor.registerEnd();
			}
			
			synchronized (view) {
				view.outputProgress();
			}
		}
		
		out.append('\n');
		out.flush();
		
		if(complete || forceStore()) {
			synchronized (progressMonitor) {
				progressMonitor.save();
			}
		}
		
			
		out.close();
		
		
	}

	private boolean forceStore() {
		final String property = System.getProperty("pm.forcestore");
		if(property == null) {
			return false;
		} else {
			return Boolean.parseBoolean(property);
		}
	}



}

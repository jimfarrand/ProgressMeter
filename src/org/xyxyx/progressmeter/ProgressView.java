package org.xyxyx.progressmeter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class ProgressView {
	public interface ProgressFormatPart {
		void output(ProgressView view) throws IOException;
	}
	
	public static class StringFormatPart implements ProgressFormatPart {

		private final String data;

		public StringFormatPart(String data) {
			this.data = data;
		}

		@Override
		public void output(ProgressView view) throws IOException {
			view.write(data);
		}
		
	}

	public static ProgressFormatPart LINE_FORMAT_PART = new ProgressFormatPart() {
		@Override
		public void output(ProgressView view) throws IOException {
			view.write(view.line);
		}
	};
	
	public static ProgressFormatPart PERCENT_DONE_FORMAT_PART = new ProgressFormatPart() {
		@Override
		public void output(ProgressView view) throws IOException {
			final double fractionDone;
			synchronized(view.progressMeter) {
				fractionDone = view.progressMeter.getFractionDone();
			}
			if(fractionDone < 0) {
				view.write("?");
			} else {
				view.write(String.format("%.1f", fractionDone*100.0));
			}
		}
	};
	
	public static ProgressFormatPart ELAPSED_TIME_FORMAT_PART = new ProgressFormatPart() {
		@Override
		public void output(ProgressView view) throws IOException {
			final double elapsedTime;
			synchronized(view.progressMeter) {
				elapsedTime = view.progressMeter.getElapsedTime();
			}
			view.write(formatTime(elapsedTime / 1000.0));
		}
	};

	public static ProgressFormatPart REMAINING_FORMAT_PART = new ProgressFormatPart() {
		@Override
		public void output(ProgressView view) throws IOException {
			final double remainingTime;
			synchronized (view.progressMeter) {
				remainingTime = view.progressMeter.getRemainingTime();
			}
			if(remainingTime < 0) {
				view.write("?");
			} else {
				view.write(formatTime(remainingTime / 1000.0));
			}
		}
	};
	
	public static class ProgressBar implements ProgressFormatPart {
		
		private final int length;
		private int iteration;
		private double lastTime;
		
		public ProgressBar(int length) {
			this.length = length;
		}
		
		@Override
		public void output(ProgressView view) throws IOException {
			final double fractionDone;
			synchronized(view.progressMeter) {
				fractionDone = view.progressMeter.getFractionDone();
				
				final double newTime = view.progressMeter.getElapsedTime();
				if(newTime - lastTime > 100) {
					lastTime = newTime;
					iteration++;
				}
			}
			
			view.write("|");
			
			if(fractionDone >= 0) {
				final int filled = (int)Math.floor(length * fractionDone);
				
				for(int i = 0; i < length; i++) {
					if(i < filled) {
						view.write("#");
					} else {
						view.write(" ");
					}
				}
			} else {
				// We don't have an elapsed time, so we just do a Kit style left-right
				// thing
				
				
				final int period = (length * 2) - 2;
				int pos = iteration % period;
				if(pos >= length) {
					pos = period - pos;
				}
				
				for(int i = 0; i < length; i++) {
					if(i == pos) {
						view.write("#");
					} else {
						view.write(" ");
					}
				}
				pos = (pos+1) % length;
			}

			view.write("|");			
		}
		
	};
	
	private static final double MINUTE=60;
	private static final double HOUR=60*MINUTE;
	private static final double DAY=24*HOUR;
	
	public static String formatTime(double seconds) {
		final int days = (int)(seconds/DAY);
		seconds -= days*DAY;
		final int hours = (int)(seconds/HOUR);
		seconds -= hours*HOUR;
		final int minutes = (int)(seconds/MINUTE);
		seconds -= minutes*MINUTE;
		
		if(days > 0) {
			return String.format("%dd%02dh%02dm%02ds", days, hours, minutes, (int)seconds);
		} else if(hours > 0) {
			return String.format("%dh%02dm%02ds", hours, minutes, (int)seconds);
		} else if(minutes > 0) {
			return String.format("%dm%02ds", minutes, (int)seconds);
		} else {
			return String.format("%.1fs", seconds);
		}
	}
	
	public static final ProgressFormatPart TOTAL_ESTIMATED_TIME_FORMAT_PART = new ProgressFormatPart() {
		@Override
		public void output(ProgressView view) throws IOException {
			double estimatedTotalTime = view.progressMeter.getEstimatedTime();
			if(estimatedTotalTime < 0) {
				view.write("?");
			} else {
				view.write(formatTime(estimatedTotalTime / 1000.0));
			}
		}
	}; 
	
	private final List<ProgressFormatPart> lineFormat;
	private final List<ProgressFormatPart> progressFormat;
	private final ProgressMonitor progressMeter;
	private final OutputStreamWriter out;
	private String line;
	private int written;
	private int lastProgress;

	private long lastProgressTime = System.currentTimeMillis();

	public ProgressView(
			ProgressMonitor progressMeter,
			OutputStreamWriter out,
			List<ProgressFormatPart> lineFormat,
			List<ProgressFormatPart> progressFormat) {
		
		this.progressMeter = progressMeter;
		this.out = out;
		this.lineFormat = new ArrayList<ProgressFormatPart>(lineFormat);
		this.progressFormat = new ArrayList<ProgressFormatPart>(progressFormat);
		
		final Thread ticker = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					handleProgressTicks();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		ticker.setDaemon(true);
		ticker.start();
	}

	protected void write(String string) throws IOException {
		out.write(string);
		written += string.length();
	}

	public void outputLine(String line) throws IOException {
		this.line = line;
		this.written = 0;
		for(ProgressFormatPart part : lineFormat) {
			part.output(this);
		}
		this.line = null;
		
		final int spaces = lastProgress - written;
		for(int i = 0; i < spaces; i++) {
			out.write(' ');
		}
		
		out.write('\n');
		lastProgress = 0;
		out.flush();
	}
	
	public void outputProgress() throws IOException {
		this.lastProgressTime = System.currentTimeMillis();
		this.written = 0;
		for(ProgressFormatPart part : progressFormat) {
			part.output(this);
		}

		final int spaces = lastProgress - written;
		for(int i = 0; i < spaces; i++) {
			out.write(' ');
		}
		
		this.lastProgress = written;
		
		out.write('\r');
		out.flush();
	}
	
	private void handleProgressTicks() throws InterruptedException, IOException {
		while(true) {
			synchronized (this) {
				final long currentTime = System.currentTimeMillis();
				final long remaining = 1000 - (currentTime - lastProgressTime);
				if(remaining > 0) {
					wait(remaining);
				} else {
					synchronized (progressMeter) {
						progressMeter.updateTotalTime();
					}
					outputProgress();
				}
			}
		}
	}

}

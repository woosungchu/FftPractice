package woosungchu;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class Lora {
	
	boolean running = false;
	boolean isMatching = false;
	public final int UPPER_LIMIT = 300;
	public final int LOWER_LIMIT = 40;
	
	double highscores[][];
	double recordPoints[][];
	long points[][];
	Map<Long, List<DataPoint>> hashMap;
	Map<Integer, Map<Integer, Integer>> matchMap; // Map<SongId, Map<Offset,Count>>
	
	long songCount = 0;
	
	public final int[] RANGE = new int[] { 40, 80, 120, 180, UPPER_LIMIT + 1 };
	
	private static final int FUZ_FACTOR = 2;
	
	public void run(){
		ByteArrayOutputStream out = null;
		try {
			out = listen();
			
			Complex[][] results = fft(out);
			analyze(results);
			songCount++;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run(File file){
		ByteArrayOutputStream out = null;
		try {
			if(file != null){
				out = getAudio(file);
			}
			
			Complex[][] results = fft(out);
			analyze(results);
			songCount++;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private AudioFormat getFormat() {
	    float sampleRate = 44100;
	    int sampleSizeInBits = 8;
	    int channels = 1; //mono
	    boolean signed = true;
	    boolean bigEndian = true;
	    return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}

	private ByteArrayOutputStream getAudio(File file) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedInputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream(file));
			
			int read;
			byte[] buff = new byte[1024];
			while ((read = in.read(buff)) > 0)
			{
			    out.write(buff, 0, read);
			}
			out.flush();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return out;
	}
	
	private ByteArrayOutputStream listen() throws LineUnavailableException{
		final AudioFormat format = getFormat(); //Fill AudioFormat with the wanted settings
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
		line.open(format);
		line.start();
		
		byte[] buffer = new byte[line.getBufferSize() / 5];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		running = true;

		try {
		    while (running) {
		        int count = line.read(buffer, 0, buffer.length);
		        if (count > 0) {
		            out.write(buffer, 0, count);
		        }
		    }
		    out.close();
		} catch (IOException e) {
		    System.err.println("I/O problems: " + e);
		    System.exit(-1);
		}
		
		return out;
	}
	
	private Complex[][] fft(ByteArrayOutputStream out) {
		byte audio[] = out.toByteArray();

		final int totalSize = audio.length;

		int amountPossible = totalSize/Harvester.CHUNK_SIZE;

		//When turning into frequency domain we'll need complex numbers:
		Complex[][] results = new Complex[amountPossible][];

		//For all the chunks:
		for(int times = 0;times < amountPossible; times++) {
		    Complex[] complex = new Complex[Harvester.CHUNK_SIZE];
		    for(int i = 0;i < Harvester.CHUNK_SIZE;i++) {
		        //Put the time domain data into a complex number with imaginary part as 0:
		        complex[i] = new Complex(audio[(times*Harvester.CHUNK_SIZE)+i], 0);
		    }
		    //Perform FFT analysis on the chunk:
		    results[times] = FFT.fft(complex);
		}
		
		System.out.println(Arrays.toString(results));
		return results;
	}
	
	// Find out in which range
	public int getIndex(int freq) {
		int i = 0;
		while (RANGE[i] < freq)
			i++;
		return i;
	}
	

	private void analyze(Complex[][] results) {
		highscores = new double[results.length][5];
		recordPoints = new double[results.length][UPPER_LIMIT];
		points = new long[results.length][5];
		
		for (int i = 0; i < results.length; i++) {
			for (int j = 0; j < 5; j++) {
				highscores[i][j] = 0;
			}
		}

		for (int i = 0; i < results.length; i++) {
			for (int j = 0; j < UPPER_LIMIT; j++) {
				recordPoints[i][j] = 0;
			}
		}

		for (int i = 0; i < results.length; i++) {
			for (int j = 0; j < 5; j++) {
				points[i][j] = 0;
			}
		}
		
		//de
		
		for (int t = 0; t < results.length; t++) {
			for (int freq = LOWER_LIMIT; freq < UPPER_LIMIT - 1; freq++) {
				// Get the magnitude:
				double mag = Math.log(results[t][freq].abs() + 1);

				// Find out which range we are in:
				int index = getIndex(freq);

				// Save the highest magnitude and corresponding frequency:
				if (mag > highscores[t][index]) {
					highscores[t][index] = mag;
					recordPoints[t][freq] = 1;
					points[t][index] = freq;
				}
			}
			
			//console
			for (int k = 0; k < 5; k++) {
				System.out.println("" + highscores[t][k] + ";" + recordPoints[t][k] + "\t");
			}
			
			//hashing
			long h = hash(points[t][0], points[t][1], points[t][2], points[t][3]);
			
			if (isMatching) {
				List<DataPoint> listPoints;

				if ((listPoints = hashMap.get(h)) != null) {
					for (DataPoint dP : listPoints) {
						int offset = Math.abs(dP.getTime() - t);
						Map<Integer, Integer> tmpMap = null;
						if ((tmpMap = this.matchMap.get(dP.getSongId())) == null) {
							tmpMap = new HashMap<Integer, Integer>();
							tmpMap.put(offset, 1);
							matchMap.put(dP.getSongId(), tmpMap);
						} else {
							Integer count = tmpMap.get(offset);
							if (count == null) {
								tmpMap.put(offset, new Integer(1));
							} else {
								tmpMap.put(offset, new Integer(count + 1));
							}
						}
					}
				}
			} else {
				List<DataPoint> listPoints = null;
				if ((listPoints = hashMap.get(h)) == null) {
					listPoints = new ArrayList<DataPoint>();
					DataPoint point = new DataPoint((int) songCount, t);
					listPoints.add(point);
					hashMap.put(h, listPoints);
				} else {
					DataPoint point = new DataPoint((int) songCount, t);
					listPoints.add(point);
				}
			}
			
		}//end for
		
	}
	
	private long hash(long p1, long p2, long p3, long p4) {
		return (p4 - (p4 % FUZ_FACTOR)) * 100000000 + (p3 - (p3 % FUZ_FACTOR))
				* 100000 + (p2 - (p2 % FUZ_FACTOR)) * 100
				+ (p1 - (p1 % FUZ_FACTOR));
	}
}

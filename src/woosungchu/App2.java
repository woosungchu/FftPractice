package woosungchu;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class App2 {
	
	private static final String URL = "JessieJBangBang.wav";
	
	public static void main(String[] args) throws LineUnavailableException {
	    System.out.println("Working Directory = " + System.getProperty("user.dir"));
		 
		final AudioFormat format = getFormat();
		
		if(format != null){
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format);
			line.start();
		}
		
	}
	
	private static AudioFormat getFormat(){
		AudioInputStream stream = null;
		
		try {
			System.out.println("Getting AudioFormat... of '"+URL+"'");
			stream = AudioSystem.getAudioInputStream(new File(URL));
		} catch (UnsupportedAudioFileException e) {
			//[WAV, AU, WAV] suppoted / [MP3] not supported
			System.out.println("This Audio file format is not supported");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stream.getFormat();
	}
	

}

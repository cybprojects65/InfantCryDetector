package it.cnr.infant.toneunits;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import it.cnr.infant.features.AudioBits;
import it.cnr.infant.features.AudioWaveGenerator;
import it.cnr.infant.toneunits.LabFile.Segment;
import it.cnr.infant.utils.SignalProcessing;

public class Lab2Audio {

	public static File [] extractTUs(File audioFile, File labFile) throws Exception {
		
		
		File tufolder = new File(audioFile.getParentFile(),audioFile.getName().replace(".wav", "_TUs"));

		System.out.println("[TU Extr] extracting to folder "+tufolder.getAbsolutePath());
		
		if (!tufolder.exists()) {
			tufolder.mkdir();
			AudioBits sb = new AudioBits(audioFile);
			AudioFormat format = sb.getAudioFormat();
			double samplingFrequency = format.getSampleRate();
			
			System.out.println("[TU Extr] reading input file at sampling freq "+(int) samplingFrequency);
			
			short signal [] = sb.getShortVectorAudio();
			
			System.out.println("[TU Extr] signal read - reading labels");
			
			LabFile lab = LabFile.parse(Files.readString(labFile.toPath()));
			List<Segment> segments = lab.getSegments();
			int tus = 0;
			for (Segment s : segments) {
				double t0 = s.start;
				double t1 = s.end;
				String label = s.label;
				if (label.trim().length()>0 && !label.equals("-")) {
					String filename = label+".wav";
					short[] portion = SignalProcessing.extractSignalPortion(signal, samplingFrequency, t0, t1);
					AudioWaveGenerator.generateWaveFromSamplesWithSameFormat(portion, new File(tufolder,filename) , format);
					tus++;
				}				
			}
			
			System.out.println("[TU Extr] extracted "+tus+" TUs");
		}
		
		return tufolder.listFiles();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

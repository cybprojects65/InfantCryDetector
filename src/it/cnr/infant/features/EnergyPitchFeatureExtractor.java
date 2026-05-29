package it.cnr.infant.features;

import java.io.File;



public class EnergyPitchFeatureExtractor {

	public double SNR;
	//public WorkflowConfiguration config;
	private double window4Analysis;
	
	public EnergyPitchFeatureExtractor() {
		this.window4Analysis = 0.1;
	}
	
	public EnergyPitchFeatureExtractor(double window4Analysis) {
		this.window4Analysis = window4Analysis;
	}
	
		
	public double getSNR() {
		return SNR;
	}

	
	public double [] getEnergyFeatures(File audioFile) {
		return new Energy().energyCurve((float)window4Analysis, audioFile, true);
	}
	
	public double [] getPitchFeatures(File audioFile) {
		PitchExtractor pitchExtr = new PitchExtractor();
		pitchExtr.setPitchWindowSec(window4Analysis);
		pitchExtr.calculatePitch(audioFile.getAbsolutePath());
		Double [] pitchCurve = pitchExtr.pitchCurve;
		double pitch [] = new double[pitchCurve.length];
		for (int i=0;i<pitchCurve.length;i++) { 
			if (pitchCurve[i] == null || Double.isNaN(pitchCurve[i]) || Double.isInfinite(pitchCurve[i]))
				pitch [i]= 0;
			else
				pitch [i]= pitchCurve[i].doubleValue();
		}
		return pitch;
	}
	
	
	
}

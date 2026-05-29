package it.cnr.infant.main;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import it.cnr.infant.asr.ASR;
import it.cnr.infant.hmm.HMMManager;
import it.cnr.infant.toneunits.Lab2Audio;
import it.cnr.infant.toneunits.LabFile;
import it.cnr.infant.toneunits.ToneUnitSegmentationManager;

import it.cnr.infant.utils.Utils;

public class Main {

	public static void main(String args []) throws Exception{
		
		File test = new File("./testFiles/Subintensive-unknown-1child-21_01.wav");
		//File test = new File("./testFiles/Subintensive-wakeup-1child-21_05.wav");
		//File test = new File("./testFiles/Subintensive-seeking_attention_pathologic-2child-23_01.wav");
		//File test = new File("./testFiles/Subintensive-pathological_brain-2children#1#2-23_05.wav");
		//File test = new File("./testFiles/Subintensive-annoyed-1child#2-23_04.wav");
		//File test = new File("./testFiles/Subintensive-pathological_brain-1child#1-23_06.wav");
		//File test = new File("./testFiles/Subintensive-annoyed-1child#2-23_03.wav");
		//File test = new File("./testFiles/Subintensive-annoyed-1child#2-23_02.wav");
		//File test = new File("./testFiles/Subintensive-hunger-1child-21_04.wav");
		//File test = new File("./testFiles/Subintensive-hunger-1child-21_02.wav");
		//File test = new File("./testFiles/Subintensive-annoyed-1child#2-23_01.wav");
		
		if (args!=null && args.length>0){
			
			test = new File(args[0]);
			
		}
		  
		System.out.println("[MAIN] Processing file "+test.getAbsolutePath());
		
		System.out.println("[MAIN] Step 1 - Tone unit segmentation");
		ToneUnitSegmentationManager tusm = new ToneUnitSegmentationManager();
		File toneUnitSegmentation = tusm.detectToneUnits(test);
		
		String inputLabText = Files.readString(toneUnitSegmentation.toPath());
		LabFile lab = LabFile.parse(inputLabText);
		int originallyFoundSegments = lab.getNumberSegments();
		
		System.out.println("[MAIN] Step 2 - Tone unit filtering");
		File toneUnitSegmentationFiltered = tusm.filterShortToneUnits(toneUnitSegmentation);
		
		System.out.println("[MAIN] Step 2 - Tone unit saving");
		File [] tus = Lab2Audio.extractTUs(test, toneUnitSegmentationFiltered);
		
		List<File> tusWaves = new ArrayList<File>();
		
		for (File af : tus) {
			if (af.getName().endsWith(".wav")) {
				tusWaves.add(af);
			}
			
		}
		
		tus = new File[tusWaves.size()];
		tus = tusWaves.toArray(tus);
		
		System.out.println("[MAIN] Step 3 - HMM classification");
		HMMManager hmm = new HMMManager();
		double scores [] = hmm.cryClassificationScore(tus);
		
		System.out.println("[MAIN] Step 4 - ASR scoring");
		ASR asr = new ASR(); 
		double [] isSpeechScores = asr.detectNonSpeechScore(tus);
		System.out.println("[MAIN] Step 5 - Decision");
		boolean isCryVector[] = new boolean[tus.length];
		
		int i = 0;
		for (File tu:tus) {
			
			double score = scores [i];
			double isSpeechScore = isSpeechScores [i];
			boolean isCry = false;
			
			if (score>1)
				isCry = false;
			
			else if (isSpeechScore==0 && score>0.60) 
				isCry = true;
			
			else if (isSpeechScore>90)
				isCry = true;
			
			else if (isSpeechScore==0) 
				isCry = false;
			
			//else if (score>0.90) 
				//isCry = true;
			
			System.out.println("[MAIN] "+tu.getName()+" hmm:"+score+" asr:"+isSpeechScore+" -> is cry "+isCry);
			isCryVector[i] = isCry;
			
			i++;
		}
		
		System.out.println("[MAIN] Step 6 - Saving TUs of infant cry");
		
		File outputFolder = new File(test.getAbsolutePath().replace(".wav","_infant_cry_isolated"));
		if (outputFolder.exists()) {
			Utils.deleteDirectory(outputFolder.toPath());
		
		}
		outputFolder.mkdir();
			i = 0;
			int k = 0;
			for (File tu:tus) {
				if (isCryVector[i]) {
					Files.copy(tu.toPath(), new File(outputFolder,tu.getName()).toPath());
					k++;
				}
				
				i++;
			
		}
			System.out.println("[MAIN] Saved "+k+" TUs over the original "+originallyFoundSegments+" TUs");
		System.out.println("[MAIN] DONE.");
	}
}

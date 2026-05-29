package it.cnr.infant.hmm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationVector;
import be.ac.ulg.montefiore.run.jahmm.OpdfMultiGaussianFactory;
import be.ac.ulg.montefiore.run.jahmm.learn.KMeansLearner;
import it.cnr.infant.features.AudioBits;
import it.cnr.infant.features.EnergyPitchFeatureExtractor;
import it.cnr.infant.features.MFCCExtractor;
import it.cnr.infant.utils.ConfigManager;

public class HMMManager {

	public int numberOfStates;
	public Hmm<ObservationVector> hmm;

	public List<ObservationVector> timeSeriesToObservations(double[][] timeSeries) {

		List<ObservationVector> sequence = new ArrayList<ObservationVector>();

		for (int i = 0; i < timeSeries.length; i++) {

			double vett[] = timeSeries[i];
			ObservationVector obs = new ObservationVector(vett);
			sequence.add(obs);
		}

		return sequence;
	}

	public List<List<ObservationVector>> timeSeriesListToObservationList(List<double[][]> timeSeriesList) {

		List<List<ObservationVector>> allObs = new ArrayList<List<ObservationVector>>();

		for (double[][] timeSeries : timeSeriesList) {
			List<ObservationVector> ts = timeSeriesToObservations(timeSeries);
			allObs.add(ts);
		}

		return allObs;

	}

	public HMMManager(int numberOfStates) {
		this.numberOfStates = numberOfStates;
	}

	public HMMManager() {

	}

	
	public double [] cryClassificationScore(File [] files) throws Exception{
		
		File hmm_pretrained_cry = new File(ConfigManager.getProperty("hmm_pretrained"));
		File hmm_pretrained_non_cry = new File(ConfigManager.getProperty("hmm_pretrained_noncry"));
		HMMManager manager_cry = new HMMManager();
		HMMManager manager_noncry = new HMMManager();
		
		double [] likes_cry = manager_cry.calcLikelyhoodMFCC(files, hmm_pretrained_cry);
		double [] likes_noncry = manager_noncry.calcLikelyhoodMFCC(files, hmm_pretrained_non_cry);
			
		double [] scores = new double[likes_cry.length];
		
		for (int i=0;i<scores.length;i++) {
			//since numbers are negative:
			//<1 : cry
			//>1 : noncry
			scores[i] = likes_cry[i]/likes_noncry[i];
			
		}
		
		return scores;
	}
	
	public Hmm<ObservationVector> trainHMM(List<double[][]> timeSeriesList) {

		List<List<ObservationVector>> allObs = timeSeriesListToObservationList(timeSeriesList);
		int numFeats = allObs.get(0).get(0).dimension();

		hmm = new Hmm<ObservationVector>(numberOfStates, new OpdfMultiGaussianFactory(numFeats));
		KMeansLearner<ObservationVector> bwl = new KMeansLearner<ObservationVector>(numberOfStates,
				new OpdfMultiGaussianFactory(numFeats), allObs);
		hmm = bwl.learn();

		return hmm;
	}

	public double calcLike(double[][] X) {
		List<ObservationVector> oseq = timeSeriesToObservations(X);
		// apply Viterbi
		double like = hmm.lnProbability(oseq);
		return like;
	}

	public void saveHMM(File outputFile) throws Exception {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFile));
		oos.writeObject(hmm);
		oos.close();
		System.out.println("HMM saved");

	}

	@SuppressWarnings("unchecked")
	public Hmm<ObservationVector> loadHMM(File outputFile) throws Exception {

		if (hmm == null) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(outputFile));
			Object hmmobs = ois.readObject();

			hmm = (Hmm<ObservationVector>) hmmobs;
			numberOfStates = hmm.nbStates();

			ois.close();
			System.out.println("HMM loaded");
		}
		return hmm;

	}

	boolean[] potentialcries;

	
	public double[] calcLikelyhoodMFCC(File audioFiles[], File hmmFile) throws Exception {

		List<File> wavFiles = new ArrayList<File>();
		
		for (File af : audioFiles) {
			if (af.getName().endsWith(".wav")) {
				wavFiles.add(af);
			}
			
		}
		
		audioFiles = new File[wavFiles.size()];
		audioFiles = wavFiles.toArray(audioFiles);
		
		double likes[] = new double[audioFiles.length];

		int i = 0;
		double cumulative = 0;
		for (File af : audioFiles) {
			if (af.getName().endsWith(".wav")) {
				likes[i] = calcLikelyhoodMFCC(af,hmmFile);
				cumulative += likes[i];

				//System.out.println("[HMM] " + af.getName() + "->" + likes[i] + " : " + potentialcries[i]);

				i++;
			}
		}

		System.out.println("[HMM] Cumulative likelihood " + cumulative);
		System.out.println("[HMM] Average likelihood " + (cumulative / (double) audioFiles.length));
		
		
		return likes;
	}
	
	
	public double calcLikelyhoodMFCC(File audioFile, File hmmfile) throws Exception {

		// getReferenceVectors(energyWindow4Analysis, pitchWindow4Analysis,
		// featurewindowsize, featurewindowshift);
		//File hmmfile = new File(ConfigManager.getProperty("hmm_pretrained"));

		loadHMM(hmmfile);
		
		AudioBits ab = new AudioBits(audioFile);
		double samplingFrequency = ab.getAudioFormat().getSampleRate();
		short[] signal = ab.getShortVectorAudio();
		
		double[][] vec = MFCCExtractor.extractMFCCWithDeltas(
                signal,
                samplingFrequency,
                13,
                26,
                25.0,
                10.0
        );
		double d = calcLike(vec);

		return d;
	}
	
	public double[][] extractEnergypitchFeatures(File audioFile) {

		float energyWindow4Analysis = Float.parseFloat(ConfigManager.getProperty("energy_analysis_window"));
		EnergyPitchFeatureExtractor fe = new EnergyPitchFeatureExtractor(energyWindow4Analysis);

		double[] energy = fe.getEnergyFeatures(audioFile);
		double[] pitch = fe.getPitchFeatures(audioFile);
		double[][] vec = new double[energy.length][2];

		for (int i = 0; i < energy.length; i++) {
			vec[i][0] = energy[i];
			if (i<pitch.length)
				vec[i][1] = pitch[i];
			else
				vec[i][1] = 0;
		}

		return vec;
	}

	public void trainHMMs(File folderWithAudioFiles) throws Exception {

		File[] files = folderWithAudioFiles.listFiles();

		String hmm_pretrained = ConfigManager.getProperty("hmm_pretrained");

		System.out.println("[HMM] Training hmm from folder " + folderWithAudioFiles.getAbsolutePath());

		List<double[][]> timeSeriesList = new ArrayList<double[][]>();

		for (File f : files) {
			if (f.getName().endsWith(".wav")) {
				double[][] vec = extractEnergypitchFeatures(f);
				timeSeriesList.add(vec);
				System.out.println("[HMM] added " + f.getName());
			}
		}

		System.out.println("[HMM] training ... ");
		Hmm<ObservationVector> hmm = trainHMM(timeSeriesList);
		this.hmm = hmm;

		System.out.println("[HMM] saving ... ");
		saveHMM(new File(hmm_pretrained));
		System.out.println("[HMM] done.");

	}

	public void trainHMMsMFCC(File folderWithAudioFiles, File hmm_pretrained) throws Exception {

		File[] files = folderWithAudioFiles.listFiles();

		//String hmm_pretrained = ConfigManager.getProperty("hmm_pretrained");

		System.out.println("[HMM] Training hmm from folder " + folderWithAudioFiles.getAbsolutePath());

		List<double[][]> timeSeriesList = new ArrayList<double[][]>();

		for (File f : files) {
			if (f.getName().endsWith(".wav")) {
				System.out.println("[HMM] extracting MFCC for " + f.getName());
				AudioBits ab = new AudioBits(f);
				double samplingFrequency = ab.getAudioFormat().getSampleRate();
				short[] signal = ab.getShortVectorAudio();
				
				double[][] vec = MFCCExtractor.extractMFCCWithDeltas(
		                signal,
		                samplingFrequency,
		                13,
		                26,
		                25.0,
		                10.0
		        );
				timeSeriesList.add(vec);
				System.out.println("[HMM] added " + f.getName());
			}
		}

		System.out.println("[HMM] training ... ");
		Hmm<ObservationVector> hmm = trainHMM(timeSeriesList);
		this.hmm = hmm;

		System.out.println("[HMM] saving ... ");
		saveHMM(hmm_pretrained);
		System.out.println("[HMM] done.");

	}
	
	public static void main(String args[]) throws Exception {

		File audiosamplefolder_cry = new File("./crycorpus");
		File audiosamplefolder_noncry = new File("./noncrycorpus");
		
		File [] listCry = audiosamplefolder_cry.listFiles();
		double totalSecCry = 0;
		for (File cryf:listCry) {
			
			AudioBits ab = new AudioBits(cryf);
			double fs = ab.getAudioFormat().getSampleRate();
			int l = ab.getShortVectorAudio().length;
			double secs = ((double)l)/fs;
			totalSecCry+=secs;
		}
		System.out.println("[HMM TRAINING START] using "+totalSecCry+"s of cry to train the HMM");	
		
		
		File [] listNonCry = audiosamplefolder_noncry.listFiles();
		double totalSecNonCry = 0;
		for (File cryf:listNonCry) {
			
			AudioBits ab = new AudioBits(cryf);
			double fs = ab.getAudioFormat().getSampleRate();
			int l = ab.getShortVectorAudio().length;
			double secs = ((double)l)/fs;
			totalSecNonCry+=secs;
		}
		System.out.println("[HMM TRAINING START] using "+totalSecNonCry+"s of NONcry to train the HMM");
		
		
		int numberOfStates = 10;
		boolean debug = false;
		File hmm_pretrained_cry = new File(ConfigManager.getProperty("hmm_pretrained"));
		File hmm_pretrained_non_cry = new File(ConfigManager.getProperty("hmm_pretrained_noncry"));
		
		HMMManager manager_cry = new HMMManager(numberOfStates);
		// File hmmfile = new File(ConfigManager.getProperty("hmm_pretrained"));
		// Hmm<ObservationVector> hmm = manager.loadHMM(hmmfile);
		// System.out.println("HMM # states: "+hmm.nbStates());
		System.out.println("[Main] TRAINING");
		HMMManager manager_noncry = new HMMManager(numberOfStates);
		
		
		if (!hmm_pretrained_cry.exists() || debug) {
			manager_cry.trainHMMsMFCC(audiosamplefolder_cry,hmm_pretrained_cry);
			manager_noncry.trainHMMsMFCC(audiosamplefolder_noncry,hmm_pretrained_non_cry);
		}
		
		System.out.println("[Main] TESTING ON CRY");
		double [] likes_cry = manager_cry.calcLikelyhoodMFCC(audiosamplefolder_cry.listFiles(), hmm_pretrained_cry);
		double [] likes_noncry = manager_noncry.calcLikelyhoodMFCC(audiosamplefolder_cry.listFiles(), hmm_pretrained_non_cry);
		
		int score = 0;
		for (int i=0;i<likes_cry.length;i++) {
			
			if (likes_cry[i]>likes_noncry[i])
				score++;
		} 
		
		double accuracyC = (double) (score)/(double)likes_cry.length;
			
		System.out.println("[Main] ACCURACY CRY "+accuracyC);
		
		System.out.println("[Main] TESTING ON NON CRY");
		likes_cry = manager_cry.calcLikelyhoodMFCC(audiosamplefolder_noncry.listFiles(), hmm_pretrained_cry);
		likes_noncry = manager_noncry.calcLikelyhoodMFCC(audiosamplefolder_noncry.listFiles(), hmm_pretrained_non_cry);

		score = 0;
		for (int i=0;i<likes_cry.length;i++) {
			
			if (likes_cry[i]<likes_noncry[i])
				score++;
		} 
		
		double accuracyNC = (double) (score)/(double)likes_cry.length;
		System.out.println("[Main] ACCURACY NON CRY "+accuracyNC);
		
	}

}

package it.cnr.infant.asr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.cnr.infant.utils.ConfigManager;
import it.cnr.infant.utils.DockerExecutor;

public class ASR {

	public static String asr_template = "docker run --rm -i=false -v \"#FOLDER#\":/home/docker srivastavam/whisper_base_ms:v1.3 \"transcribe.py #FILE#\"";
	public static List<String> queue = new ArrayList<String>();
	public String serverLocation; // = "http://asrncss.ddns.net";

	public ASR() {
		serverLocation = ConfigManager.getProperty("serverLocation");
	}

	public static ASResult parseLog(String logText, File transcriptionfile) throws Exception {

		double confidence = extractValue(logText, "'▶ Confidence'\\s*:\\s*([-0-9.]+)");

		double avgLogProb = extractValue(logText, "'avg_logprob'\\s*:\\s*([-0-9.]+)");

		double compressionRatio = extractValue(logText, "'compression_ratio'\\s*:\\s*([-0-9.]+)");

		String transcription = Files.readString(transcriptionfile.toPath()).trim();

		return new ASResult(confidence, avgLogProb, compressionRatio, transcription);
	}

	public static ASResult parseLog(String logText) throws Exception {
		String tran = "===== TRANSCRIPTION =====";
		String console = "===== CONSOLE OUTPUT =====";

		String transcription = logText.substring(logText.indexOf(tran) + tran.length(), logText.indexOf(console))
				.trim();

		String rest = logText.substring(logText.indexOf(console));
		logText = rest;

		double confidence = extractValue(logText, "'▶ Confidence'\\s*:\\s*([-0-9.]+)");

		double avgLogProb = extractValue(logText, "'avg_logprob'\\s*:\\s*([-0-9.]+)");

		double compressionRatio = extractValue(logText, "'compression_ratio'\\s*:\\s*([-0-9.]+)");

		return new ASResult(confidence, avgLogProb, compressionRatio, transcription);
	}

	private static double extractValue(String text, String regex) {

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(text);

		if (matcher.find()) {
			return Double.parseDouble(matcher.group(1));
		}

		throw new RuntimeException("Value not found for regex: " + regex);
	}

	public ASResult recognizeRemote(File audioFile) throws Exception {

		System.out.println("[ASR] Processing file  " + audioFile.getAbsolutePath());
		File transcription = new File(audioFile.getAbsolutePath().replace(".wav", ".txt"));
		String output = null;
		if (!transcription.exists()) {
			System.out.println("[ASR] Sending audio to the server..");
			output = ASRClient.sendWaveFile(audioFile, serverLocation);

			BufferedWriter bw = new BufferedWriter(new FileWriter(transcription));
			bw.write(output);
			bw.close();
		} else {
			output = new String(Files.readAllBytes(transcription.toPath()));
		}

		System.out.println("[ASR] Parsing output  " + audioFile.getAbsolutePath());
		ASResult result = parseLog(output);

		System.out.println("[ASR] RESULT:\n" + result.toString());

		return result;
	}

	public static double hRatio(String text) {

	    if (text == null || text.isEmpty()) {
	        return 0.0;
	    }

	    int hCount = 0;

	    for (int i = 0; i < text.length(); i++) {
	        char c = Character.toLowerCase(text.charAt(i));

	        if (c == 'h' || c == 'u' || c == 'm') {
	            hCount++;
	        }
	    }

	    return (double) hCount / text.length();
	}
	
	public static double calcScore(ASResult result) {
		
		String transcription = result.transcription.replaceAll("\\(.+\\)","").trim();
		
		double hrat = hRatio(transcription);
		double hratThr = Double.parseDouble(ConfigManager.getProperty("asr_cry_detection_h_ratio_thr"));
		double asr_cry_detection_maximum_compression_ratio_of_speech = Double.parseDouble(ConfigManager.getProperty("asr_cry_detection_maximum_compression_ratio_of_speech"));
		double asr_cry_detection_maximum_avg_log_of_speech = Double.parseDouble(ConfigManager.getProperty("asr_cry_detection_maximum_avg_log_of_speech"));
		String asr_cry_detection_default_nonword = ConfigManager.getProperty("asr_cry_detection_default_nonword");
		double asr_cry_detection_maximum_avg_log_of_noise = Double.parseDouble(ConfigManager.getProperty("asr_cry_detection_maximum_avg_log_of_noise"));
		
		System.out.println("[ASR] hrat: " + hrat);
		
		if (hrat>hratThr)
			return 100;
		
		else {
			if (result.compression_ratio<asr_cry_detection_maximum_compression_ratio_of_speech && result.avg_log<asr_cry_detection_maximum_avg_log_of_speech)
				return 0;
			
			if (transcription.startsWith(asr_cry_detection_default_nonword) && 
					result.compression_ratio>asr_cry_detection_maximum_compression_ratio_of_speech && 
					result.avg_log>asr_cry_detection_maximum_avg_log_of_speech && 
					result.avg_log<asr_cry_detection_maximum_avg_log_of_noise)
				return 0;
			
			else return -1;
		}
			
		
		//return hrat;
		
			
			
		
	}
	
	public double [] detectNonSpeechScore(File[] audioFiles) throws Exception {

		List<File> wavFiles = new ArrayList<File>();
		
		for (File af : audioFiles) {
			if (af.getName().endsWith(".wav")) {
				wavFiles.add(af);
			}
			
		}
		
		audioFiles = new File[wavFiles.size()];
		audioFiles = wavFiles.toArray(audioFiles);
		
		double [] isnonspeech = new double[audioFiles.length];

		int i = 0;
		for (File a : audioFiles) {
			if (a.getName().endsWith(".wav")) {
				System.out.println("\n[ASR] S/NS: " + a.getName());
				ASResult res = recognizeRemote(a);
				double score = calcScore(res);
				/*
				if (score > threshold)
					isnonspeech[i] = true;
				else
					isnonspeech[i] = false;

				System.out.println("[ASR] " + a.getName() + " : " + res.confidence + " " + res.compression_ratio + " "
						+ res.avg_log + " score: " + score + " (" + isnonspeech[i] + ")");
				 */
				isnonspeech[i]=score;
				i++;
			}
		}

		return isnonspeech;
	}

	public ASResult recognize(File audioFile) throws Exception {

		File absoluteAudioFile = audioFile.getAbsoluteFile();

		String filename = absoluteAudioFile.getName();

		String folder = audioFile.getCanonicalFile().getParentFile().getCanonicalPath();

		System.out.println("[ASR] Processing file in " + folder + " named " + filename);

		String asrcall = asr_template.replace("#FOLDER#", folder).replace("#FILE#", filename);

		System.out.println("[ASR] Process call: " + asrcall);

		String output = DockerExecutor.runDockerCommand(asrcall);

		System.out.println("[ASR] Process answer: " + output);

		File transcription = new File(audioFile.getAbsolutePath().replace(".wav", ".txt"));

		ASResult result = parseLog(output, transcription);

		System.out.println("[ASR] RESULT:\n" + result.toString());

		return result;

	}

	public static void main(String[] args) throws Exception {
		String testFilePath = "./testFiles/PS14Audio_1_vocals_large_16khz.wav";
		File testFile = new File(testFilePath);
		ASR asr = new ASR();
		ASResult res = asr.recognizeRemote(testFile);

	}

}

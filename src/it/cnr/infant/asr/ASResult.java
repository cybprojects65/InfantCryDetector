package it.cnr.infant.asr;

public class ASResult {

	public double confidence;
	public double avg_log;
	public double compression_ratio;
	public String transcription;

	public ASResult(double confidence, double avg_log, double compression_ratio, String transcription) {
		this.confidence = confidence;
		this.avg_log = avg_log;
		this.compression_ratio = compression_ratio;
		this.transcription = transcription;
	}
	
	public String toString() {
		
		return "Transcription: "+transcription+"\n"+
				"Confidence: "+confidence+"\n"+
				"Avg log likelihood: "+avg_log+"\n"+
				"Compression ratio: "+compression_ratio+"\n";
				
		
	}
	
}

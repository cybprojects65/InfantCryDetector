package it.cnr.infant.utils;

public class SignalProcessing {

	
	
	public static short[] extractSignalPortion(
	        short[] signal,
	        double samplingFrequency,
	        double t0,
	        double t1) throws Exception{

	    if (t0 < 0 || t1 < 0 || t1 <= t0) {
	        throw new IllegalArgumentException(
	                "Invalid time boundaries"
	        );
	    }

	    int startIndex = (int) Math.round(t0 * samplingFrequency);
	    int endIndex = (int) Math.round(t1 * samplingFrequency);

	    // Clamp to signal boundaries
	    startIndex = Math.max(0, startIndex);
	    endIndex = Math.min(signal.length, endIndex);

	    if (startIndex >= endIndex) {
	        return new short[0];
	    }

	    short[] portion = new short[endIndex - startIndex];

	    System.arraycopy(
	            signal,
	            startIndex,
	            portion,
	            0,
	            portion.length
	    );

	    return portion;
	}
	
	
}

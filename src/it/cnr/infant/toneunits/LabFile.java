package it.cnr.infant.toneunits;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;


public class LabFile {

	    public static class Segment {
	        public double start;
	        public double end;
	        public String label;

	        public Segment(double start, double end, String label) {
	            this.start = start;
	            this.end = end;
	            this.label = label;
	        }

	        public double getStart() {
	            return start;
	        }

	        public double getEnd() {
	            return end;
	        }

	        public String getLabel() {
	            return label;
	        }

	        public double getDuration() {
	            return end - start;
	        }

	        @Override
	        public String toString() {
	            return label + " [" + start + " - " + end + "] duration=" + getDuration();
	        }
	    }

	    private final List<Segment> segments;

	    public LabFile(List<Segment> segments) {
	        this.segments = segments;
	    }

	    public List<Segment> getSegments() {
	        return segments;
	    }

	    public int getNumberSegments() {
	        return segments.size();
	    }

	    
	    public void save(File outputFile) throws Exception{
	    	StringBuffer sb = new StringBuffer();
	    	
	    	for (Segment s:segments) {
	    		
	    		String row = s.start+" "+s.end+" "+s.label+"\n";
	    		sb.append(row);
	    		
	    	}
	    	
	    	BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
	    	
	    	bw.write(sb.toString());
	    	
	    	bw.close();
	    	
	    	
	    }
	    
	    
	    public static LabFile parse(String logText) {
	        List<Segment> segments = new ArrayList<>();

	        String[] lines = logText.split("\\R");

	        for (String line : lines) {
	            line = line.trim();

	            if (line.isEmpty()) {
	                continue;
	            }

	            String[] parts = line.split("\\s+");

	            if (parts.length != 3) {
	                throw new IllegalArgumentException("Invalid line: " + line);
	            }

	            double start = Double.parseDouble(parts[0]);
	            double end = Double.parseDouble(parts[1]);
	            String label = parts[2];

	            segments.add(new Segment(start, end, label));
	        }
	        
	        return new LabFile(segments);
	    }

	    @Override
	    public String toString() {
	        StringBuilder sb = new StringBuilder();

	        for (Segment segment : segments) {
	            sb.append(segment).append(System.lineSeparator());
	        }

	        return sb.toString();
	    }

	    public static void main(String[] args) {
	        String log =
	                "0.0 3.8000000000000003 TU0\n" +
	                "3.8000000000000003 8.6 TU1\n" +
	                "8.6 14.600000000000001 TU2";

	        LabFile parsed = LabFile.parse(log);

	        System.out.println(parsed);

	        for (Segment s : parsed.getSegments()) {
	            System.out.println(
	                    s.getLabel() + ": " +
	                    s.getStart() + " -> " +
	                    s.getEnd()
	            );
	        }
	    }
	
	
}

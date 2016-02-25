package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import com.jgaap.generics.AnalyzeException;
import com.jgaap.generics.DistanceCalculationException;
import com.jgaap.generics.Event;
import com.jgaap.generics.EventHistogram;
import com.jgaap.generics.EventSet;
import com.jgaap.generics.NeighborAnalysisDriver;
import com.jgaap.generics.Pair;

/**
 * 
 * This is a version of was had been the Frequency Centroid Driver
 * This uses the average relative frequency of events of a single author as a centroid
 * 
 * @author Michael Ryan
 * @since 5.0.2
 */
public class CentroidDriverSD extends NeighborAnalysisDriver {
	
	private static final long serialVersionUID = 1L;
	private boolean useFeatureSD;
	private boolean adjustByAuthorAvgInnerDist;
	
	/**
	 * Sets whether to normalize the features in the author centroids using the
	 * feature standard deviations (each feature with its own SD).
	 * @param useFeatureSD
	 */
	public void setUseFeatureSD(boolean useFeatureSD)
	{
		this.useFeatureSD = useFeatureSD;
	}
	
	/**
	 * Sets whether to use author's document pairwise distances to adjust the
	 * distance measured between the author and the test document.
	 * @param adjustByAuthorAvgInnerDist
	 */
	public void setAdjustByAuthorAvgInnerDist(boolean adjustByAuthorAvgInnerDist)
	{
		this.adjustByAuthorAvgInnerDist = adjustByAuthorAvgInnerDist;
	}
	
	@Override
	public String displayName() {
		return "Centroid Driver SD"+getDistanceName();
	}

	@Override
	public String tooltipText() {
		return "Computes one centroid per Author.\n" +
				"Centroids are the average relitive frequency of events over all docuents provided.\n" +
				"i=1 to n \u03A3frequencyIn_i(event)/n";
	}

	@Override
	public boolean showInGUI() {
		return true;
	}
	
	@Override
	public List<Pair<String, Double>> analyze(EventSet unknown, List<EventSet> knowns) throws AnalyzeException {
		Map<String, List<EventHistogram>>knownHistograms=new HashMap<String, List<EventHistogram>>();
		Set<Event> events = new HashSet<Event>();
		for(EventSet known : knowns){
			EventHistogram histogram = new EventHistogram();
			for(Event event : known){
				events.add(event);
				histogram.add(event);
			}
			List<EventHistogram> histograms = knownHistograms.get(known.getAuthor());
			if(histograms != null){
				histograms.add(histogram);
			} else {
				histograms = new ArrayList<EventHistogram>();
				histograms.add(histogram);
				knownHistograms.put(known.getAuthor(), histograms);
			}
		}
		EventHistogram unknownHistogram = new EventHistogram();
		for(Event event : unknown){
			events.add(event);
			unknownHistogram.add(event);
		}
		Vector<Double> unknownVector = new Vector<Double>(events.size());
		for(Event event : events){
			unknownVector.add(unknownHistogram.getRelativeFrequency(event));
		}
		List<Pair<String, Double>> result = new ArrayList<Pair<String,Double>>(knownHistograms.size());
		
		for(Entry<String, List<EventHistogram>> knownEntry : knownHistograms.entrySet()){
			Vector<Double> knownVector = new Vector<Double>(events.size());
			List<EventHistogram> currentKnownHistogram = knownEntry.getValue();
			for(Event event : events){
				double frequency = 0.0;
				double size = currentKnownHistogram.size();
				for(EventHistogram known : currentKnownHistogram){
					frequency += known.getRelativeFrequency(event)/size;
				}
				knownVector.add(frequency);
			}
			
			// compute author's documents pairwise distance average
			List<Vector<Double>> knownVectors =
					new ArrayList<Vector<Double>>();
			double authorPairwiseAvgDist = 0;
			if (adjustByAuthorAvgInnerDist)
			{
				// fill in known vectors
				Vector<Double> kv;
				for(EventHistogram known : currentKnownHistogram){
					kv = new Vector<Double>(events.size());
					for(Event event : events){
						kv.add(known.getRelativeFrequency(event));
					}
					knownVectors.add(kv);
				}
				// calculate pairwise average distance
				int numKnownVectors = knownVectors.size();
				double numPairs = numKnownVectors * (numKnownVectors - 1) / 2.0;
				try {
				for (int i = 0; i < numKnownVectors; i++)
					for (int j = 0; j < numKnownVectors; j++)
					{
						authorPairwiseAvgDist +=
								distance.distance(knownVectors.get(i), knownVectors.get(j)) / numPairs;
					}
				} catch (DistanceCalculationException e) {
					System.err.println("Distance "+distance.displayName()+" failed: " + e.toString());
					throw new AnalyzeException("Distance "+distance.displayName()+" failed");
				}
			}
			
			// compute per feature SD
			// knownVector is actually the mean vector
			Vector<Double> knownSD = new Vector<Double>(events.size());
			Vector<Double> knownVectorCopy = new Vector<Double>(knownVector);
			Vector<Double> unknownVectorCopy = new Vector<Double>(unknownVector);
			if (useFeatureSD)
			{
				// compute SD vector
				int index = 0;
				double relFreq;
				double mean;
				for(Event event : events){
					double sd = 0.0;
					for(EventHistogram known : currentKnownHistogram){
						relFreq = known.getRelativeFrequency(event);
						mean = knownVector.get(index);
						sd += (mean - relFreq) * (mean - relFreq);
					}
					sd = Math.sqrt(sd);
					knownSD.add(sd == 0 ? 1 : sd);
					index++;
				}
				
				// adjust knownVector and unknownVector accordingly (divide their values by the SDs)
				int size = knownVectorCopy.size();
				for (int i = 0; i < size; i++)
				{
					knownVectorCopy.set(i, knownVector.get(i) / knownSD.get(i));
					unknownVectorCopy.set(i, unknownVector.get(i) / knownSD.get(i));
				}
			}
			
			
			try {
				result.add(new Pair<String, Double>(knownEntry.getKey(),
						distance.distance(unknownVectorCopy, knownVectorCopy) - authorPairwiseAvgDist, 2));
			} catch (DistanceCalculationException e) {
				System.err.println("Distance "+distance.displayName()+" failed: " + e.toString());
				throw new AnalyzeException("Distance "+distance.displayName()+" failed");
			}			
		}
		Collections.sort(result);
		return result;
	}


}

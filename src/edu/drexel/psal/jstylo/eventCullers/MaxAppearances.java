package edu.drexel.psal.jstylo.eventCullers;

import java.util.*;

import com.jgaap.canonicizers.UnifyCase;
import com.jgaap.eventDrivers.NaiveWordEventDriver;
import com.jgaap.generics.*;

/**
 * Removes all events with number of appearances across all documents more than the configured threshold.
 * 
 * @author Ariel Stolerman
 */
public class MaxAppearances extends FrequencyEventsExtended {
	private static final long serialVersionUID = 1L;
	@Override
	public List<EventSet> cull(List<EventSet> eventSets) {
		
		// get minimum number of appearances to consider
		if(!getParameter("N").equals("")) {
			N = Integer.parseInt(getParameter("N"));
		}
		
		// calculate frequency of events across all documents
		map = getFrequency(eventSets);

		// remove irrelevant events
		Event e;
		for (EventSet es: eventSets) {
			for (int i=es.size()-1; i >= 0; i--) {
				e = es.eventAt(i); 
				if (map.get(e.toString()) > N)
					es.removeEvent(e);
			}
		}

		return eventSets;
	}

	@Override
	public String displayName() {
		return "Events with frequency at most N";
	}

	@Override
	public String tooltipText() {
		return displayName();
	}

	@Override
	public boolean showInGUI() {
		return false;
	}
	
}

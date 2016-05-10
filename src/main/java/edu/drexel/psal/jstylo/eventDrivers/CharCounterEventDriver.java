package edu.drexel.psal.jstylo.eventDrivers;

import com.jgaap.generics.*;

public class CharCounterEventDriver extends SingleNumericEventDriver {
	private static final long serialVersionUID = 1L;
	
	/* ==================
	 * overriding methods
	 * ==================
	 */
	
	public String displayName() {
		return "Character count";
	}

	public String tooltipText() {
		return "The total number of characters.";
	}

	public boolean showInGUI() {
		return false;
	}

	public double getValue(Document doc) {
		return doc.stringify().length();
	}
}

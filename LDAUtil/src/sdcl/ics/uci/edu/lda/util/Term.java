package sdcl.ics.uci.edu.lda.util;

public class Term implements Comparable<Term> {
	private final String term;

	private int frequency = 0;
	private double weight = 0;

	public Term(String term) {
		this.term = term;
	}

	public String getTerm() {
		return term;
	}

	public int getFrequency() {
		return frequency;
	}

	public void increaseFrecuency() {
		frequency++;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	@Override
	public int compareTo(Term o) {
		return (int) (weight * 1000 - o.weight * 1000);
	}

}

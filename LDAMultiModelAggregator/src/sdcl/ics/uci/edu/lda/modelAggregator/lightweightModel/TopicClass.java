package sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel;

public class TopicClass implements Comparable<TopicClass> {

	private String className;
	private double probability;
	private String classText;

	public TopicClass(String className, double probability) {
		this.className = className;
		this.probability = probability;

	}

	public String getClassName() {
		return className;
	}

	public double getProbability() {
		return probability;
	}

	public String getClassText() {
		return classText;
	}

	public void setClassText(String classText) {
		this.classText = classText;
	}

	@Override
	public int compareTo(TopicClass another) {
		return (int) ((-1) * (this.probability * 10000 - another.probability * 10000));
	}
}
